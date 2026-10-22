import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/features/transfers/data/models/deposit_request_model.dart';
import 'package:met/features/transfers/presentation/providers/deposit_provider.dart';
import 'package:met/features/transfers/presentation/providers/transfer_ui_provider.dart';
import 'package:met/features/transfers/presentation/providers/transfers_provider.dart';

import '../../helpers/fake_dio.dart';
import '../../helpers/test_rsa_keys.dart';

/// Flujo de transferencias y depósitos de punta a punta (provider → repositorio → fuente remota →
/// HTTP) contra un servidor falso: lo único simulado es la red.
void main() {
  late FakeDioAdapter server;
  late ProviderContainer container;

  setUp(() {
    server = FakeDioAdapter();
    server.on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}));
    container = ProviderContainer(overrides: [apiClientProvider.overrideWithValue(fakeDio(server))]);
    container.listen(transferNotifierProvider, (_, __) {});
  });

  tearDown(() => container.dispose());

  TransferNotifier transfer() => container.read(transferNotifierProvider.notifier);
  TransferState transferState() => container.read(transferNotifierProvider);

  group('Transferencia', () {
    test('verifica el destinatario y pasa al paso del monto', () async {
      server.on('GET', '/v1/accounts/verify', const FakeResponse({'accountId': 'acc-9', 'ownerName': 'Luis R***'}));
      transfer().updateIdentifier('3001234567');

      expect(await transfer().verifyRecipient(), isTrue);

      expect(server.last.queryParameters['identifier'], '3001234567');
      expect(transferState().recipient!.ownerName, 'Luis R***');
      expect(transferState().step, 1);
      expect(transferState().isLoading, isFalse);
    });

    test('no consulta al servidor sin identificador', () async {
      expect(await transfer().verifyRecipient(), isFalse);

      expect(transferState().error, 'Ingresa un número válido');
      expect(server.requests, isEmpty);
    });

    test('muestra el mensaje del backend cuando el destinatario no existe', () async {
      server.on('GET', '/v1/accounts/verify',
          const FakeResponse({'code': 'NOT_FOUND', 'message': 'No existe una cuenta con ese número'}, status: 404));
      transfer().updateIdentifier('999');

      expect(await transfer().verifyRecipient(), isFalse);

      expect(transferState().error, 'No existe una cuenta con ese número');
      expect(transferState().isLoading, isFalse);
    });

    test('no se queda cargando si el servidor responde algo que no es JSON (p. ej. un 502 en HTML)', () async {
      server.on('GET', '/v1/accounts/verify', const FakeResponse('<html>502 Bad Gateway</html>', status: 502));
      transfer().updateIdentifier('3001234567');

      expect(await transfer().verifyRecipient(), isFalse);

      expect(transferState().isLoading, isFalse);
      expect(transferState().error, isNotNull);
    });

    test('envía la transferencia con el PIN cifrado con la llave pública del servidor', () async {
      await givenVerifiedRecipient(server, transfer);
      server.on('POST', '/v1/accounts/transactions/transfer', const FakeResponse({}));
      transfer()
        ..updateAmount(150000)
        ..updateConcept('Arriendo')
        ..updateOtp('482913');

      expect(await transfer().executeTransfer('4821'), isTrue);

      final sent = server.last.json;
      expect(sent['destinationAccountId'], 'acc-9');
      expect(sent['amount'], 150000);
      expect(sent['concept'], 'Arriendo');
      expect(sent['otp'], '482913');
      expect(sent['idempotencyKey'], transferState().idempotencyKey);
      expect(sent['pin'], isNot('4821'), reason: 'el PIN nunca viaja en claro');
      expect(decryptWithTestPrivateKey(sent['pin'] as String), '4821');
      expect(transferState().step, 2);
    });

    test('muestra el motivo del rechazo que da el backend', () async {
      await givenVerifiedRecipient(server, transfer);
      server.on('POST', '/v1/accounts/transactions/transfer',
          const FakeResponse({'code': 'DAILY_LIMIT_EXCEEDED', 'message': 'Superas tu límite diario'}, status: 422));
      transfer().updateAmount(150000);

      expect(await transfer().executeTransfer('4821'), isFalse);

      expect(transferState().error, 'Superas tu límite diario');
      expect(transferState().step, 1);
    });

    test('no se queda cargando si la transferencia falla con una respuesta que no es JSON', () async {
      await givenVerifiedRecipient(server, transfer);
      server.on('POST', '/v1/accounts/transactions/transfer', const FakeResponse('Service Unavailable', status: 503));
      transfer().updateAmount(150000);

      expect(await transfer().executeTransfer('4821'), isFalse);

      expect(transferState().isLoading, isFalse);
      expect(transferState().error, isNotNull);
    });

    test('no envía nada sin destinatario, sin monto o sin PIN', () async {
      expect(await transfer().executeTransfer('4821'), isFalse);
      await givenVerifiedRecipient(server, transfer);
      expect(await transfer().executeTransfer('4821'), isFalse, reason: 'monto 0');
      transfer().updateAmount(1000);
      expect(await transfer().executeTransfer(''), isFalse, reason: 'sin PIN');

      expect(server.requests.where((r) => r.method == 'POST'), isEmpty);
    });

    test('solicita el código OTP y reporta si falla', () async {
      server.on('POST', '/v1/accounts/transactions/transfer/otp/request', const FakeResponse({}));
      await transfer().requestOtp();
      expect(transferState().error, isNull);

      server.on('POST', '/v1/accounts/transactions/transfer/otp/request', const FakeResponse({}, status: 500));
      await transfer().requestOtp();
      expect(transferState().error, 'Error solicitando código OTP');
    });

    test('permite volver atrás sin bajar del primer paso', () {
      transfer()
        ..nextStep()
        ..previousStep()
        ..previousStep();

      expect(transferState().step, 0);
    });
  });

  group('Depósito', () {
    DepositNotifier deposit() => container.read(depositProvider.notifier);
    DepositState depositState() => container.read(depositProvider);

    setUp(() {
      server.on('GET', '/v1/accounts/me', const FakeResponse({
        'id': 'acc-1', 'accountNumber': '1000012345', 'principalBalance': 250000, 'interestBalance': 1200.5, 'status': 'ACTIVE'
      }));
    });

    test('abre el checkout de Wompi recordando el saldo previo para detectar la acreditación', () async {
      server.on('POST', '/v1/accounts/deposit-pse', const FakeResponse({'paymentUrl': '/v1/payments/checkout/abc'}));
      deposit().setAmount(50000);

      await deposit().submitDeposit('NEQUI');

      expect(depositState().paymentUrl, 'http://api.test/api/v1/payments/checkout/abc');
      expect(depositState().baselineBalance, 250000);
      expect(server.last.json['returnUrl'], 'metapp://deposit/success');
    });

    test('usa tal cual una URL de pago absoluta', () async {
      server.on('POST', '/v1/accounts/deposit-pse', const FakeResponse({'paymentUrl': 'https://checkout.wompi.co/l/xyz'}));
      deposit().setAmount(50000);

      await deposit().submitDeposit('PSE');

      expect(depositState().paymentUrl, 'https://checkout.wompi.co/l/xyz');
    });

    test('exige monto y banco antes de crear el PSE nativo', () async {
      await deposit().submitNativePseDeposit();
      expect(depositState().error, 'El monto debe ser mayor a 0');

      deposit().setAmount(50000);
      await deposit().submitNativePseDeposit();
      expect(depositState().error, 'Selecciona tu banco para continuar');

      await deposit().submitDeposit('PSE');
      expect(server.requests.where((r) => r.path == '/v1/accounts/deposit-pse/native'), isEmpty);
    });

    test('crea el PSE nativo con el banco elegido en la app', () async {
      server.on('POST', '/v1/accounts/deposit-pse/native', const FakeResponse({'asyncPaymentUrl': 'https://banco.test/pse/1'}));
      deposit()
        ..setAmount(80000)
        ..setBankCode('1007');

      await deposit().submitNativePseDeposit();

      expect(server.last.json['bankCode'], '1007');
      expect(depositState().paymentUrl, 'https://banco.test/pse/1');
      expect(depositState().isLoading, isFalse);
    });

    test('explica cuando el banco no entregó el enlace o el backend rechazó', () async {
      server.on('POST', '/v1/accounts/deposit-pse/native', const FakeResponse({'asyncPaymentUrl': ''}));
      deposit()
        ..setAmount(80000)
        ..setBankCode('1007');
      await deposit().submitNativePseDeposit();
      expect(depositState().error, contains('no entregó el enlace'));

      server.on('POST', '/v1/accounts/deposit-pse/native',
          const FakeResponse({'code': 'BANK_NOT_SYNCED', 'message': 'Este banco aún no está disponible'}, status: 422));
      await deposit().submitNativePseDeposit();
      expect(depositState().error, 'Este banco aún no está disponible');
    });

    test('marca el éxito y luego reinicia el flujo', () async {
      deposit().setAmount(50000);
      deposit().markSuccess();
      expect(depositState().isSuccess, isTrue);

      deposit().reset();
      expect(depositState().amount, 0);
      expect(depositState().isSuccess, isFalse);
    });
  });

  group('Fuente remota de cuentas', () {
    test('lee movimientos, destinatarios recientes y el extracto en CSV', () async {
      server
        ..on('GET', '/v1/accounts/movements', const FakeResponse([
          {'id': 'm1', 'type': 'DEPOSIT', 'typeLabel': 'Recarga', 'concept': null, 'amount': 50000, 'isCredit': true,
            'status': 'COMPLETED', 'createdAt': '2026-10-01T15:00:00Z'}
        ]))
        ..on('GET', '/v1/accounts/recent-recipients',
            const FakeResponse([{'accountId': 'a1', 'accountNumber': '100', 'ownerName': 'Socio Uno'}]))
        ..on('GET', '/v1/accounts/statement', const FakeResponse('Fecha,Tipo\n2026-10-01,DEPOSIT\n'))
        ..on('POST', '/v1/accounts/deposit', const FakeResponse({}));
      final repository = container.read(transfersRepositoryProvider);

      final movements = await repository.getMovements();
      final recipients = await repository.getRecentRecipients();
      final csv = await repository.getStatementCsv(year: 2026, month: 10);
      await repository.deposit(DepositRequestModel(amount: 1000, method: 'CASH'));

      expect(movements.single.concept, '', reason: 'un concepto nulo se muestra vacío');
      expect(movements.single.createdAt, DateTime.utc(2026, 10, 1, 15));
      expect(recipients.single.ownerName, 'Socio Uno');
      expect(csv, startsWith('Fecha,Tipo'));
      expect(server.requests.firstWhere((r) => r.path == '/v1/accounts/statement').queryParameters, {'year': 2026, 'month': 10});
      expect(server.last.json, {'amount': 1000.0, 'method': 'CASH'});
    });
  });
}

Future<void> givenVerifiedRecipient(FakeDioAdapter server, TransferNotifier Function() transfer) async {
  server.on('GET', '/v1/accounts/verify', const FakeResponse({'accountId': 'acc-9', 'ownerName': 'Luis R***'}));
  transfer().updateIdentifier('3001234567');
  await transfer().verifyRecipient();
}
