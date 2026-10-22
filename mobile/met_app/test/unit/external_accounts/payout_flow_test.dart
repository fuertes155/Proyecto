import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/features/external_accounts/data/models/external_bank_account_model.dart';
import 'package:met/features/external_accounts/data/models/register_external_bank_account_request.dart';
import 'package:met/features/external_accounts/presentation/providers/external_accounts_provider.dart';
import 'package:met/features/external_accounts/presentation/providers/payout_ui_provider.dart';

import '../../helpers/fake_dio.dart';
import '../../helpers/test_rsa_keys.dart';

const _account = {
  'id': 'ext-1',
  'bankCode': '1007',
  'bankName': 'Bancolombia',
  'accountType': 'SAVINGS',
  'maskedAccountNumber': '****6789',
  'verificationStatus': 'PENDING',
  'verificationPending': true,
  'createdAt': '2026-10-20T10:00:00Z',
};

/// Cuentas bancarias externas y retiros (payouts) de punta a punta contra un servidor falso.
void main() {
  late FakeDioAdapter server;
  late ProviderContainer container;

  setUp(() {
    server = FakeDioAdapter();
    server.on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}));
    server.on('GET', '/v1/accounts/external-bank-accounts', const FakeResponse([_account]));
    container = ProviderContainer(overrides: [apiClientProvider.overrideWithValue(fakeDio(server))]);
    container.listen(payoutNotifierProvider, (_, __) {});
  });

  tearDown(() => container.dispose());

  group('Cuentas externas', () {
    test('lista las cuentas con etiquetas legibles', () async {
      final notifier = container.read(externalBankAccountsListProvider.notifier);

      await notifier.load();

      final account = container.read(externalBankAccountsListProvider).value!.single;
      expect(account.accountTypeLabel, 'Ahorros');
      expect(account.statusLabel, 'En revisión');
      expect(account.verificationPending, isTrue);
      expect(account.isUsable, isTrue);
    });

    test('registra, reenvía y confirma la verificación recargando la lista cada vez', () async {
      server
        ..on('POST', '/v1/accounts/external-bank-accounts', const FakeResponse({}))
        ..on('POST', '/v1/accounts/external-bank-accounts/ext-1/verify/resend', const FakeResponse({}))
        ..on('POST', '/v1/accounts/external-bank-accounts/ext-1/verify', const FakeResponse({}));
      final notifier = container.read(externalBankAccountsListProvider.notifier);

      await notifier.register(RegisterExternalBankAccountRequest(bankCode: '1007', accountType: 'SAVINGS', accountNumber: '00123456789'));
      expect(server.requests[0].json, {'bankCode': '1007', 'accountType': 'SAVINGS', 'accountNumber': '00123456789'});

      await notifier.resendVerification('ext-1');
      await notifier.confirmVerification('ext-1', 2731);

      expect(server.last.path, '/v1/accounts/external-bank-accounts');
      expect(server.requests.firstWhere((r) => r.path.endsWith('/verify')).json, {'amount': 2731});
      expect(server.requests.where((r) => r.method == 'GET').length, 3, reason: 'recarga tras cada acción');
    });

    test('expone el error cuando no puede cargar la lista', () async {
      server.on('GET', '/v1/accounts/external-bank-accounts', const FakeResponse({}, status: 500));
      final notifier = container.read(externalBankAccountsListProvider.notifier);

      await notifier.load();

      expect(container.read(externalBankAccountsListProvider).hasError, isTrue);
    });

    test('trae el catálogo de bancos del tipo pedido', () async {
      server.on('GET', '/v1/banks', const FakeResponse([
        {'code': '1007', 'name': 'Bancolombia', 'supportsPayout': true},
        {'code': '1051', 'name': 'Davivienda'},
      ]));

      final banks = await container.read(banksProvider('PAYOUT').future);

      expect(server.last.queryParameters, {'type': 'PAYOUT'});
      expect(banks.map((b) => b.name), ['Bancolombia', 'Davivienda']);
      expect(banks[1].supportsPayout, isFalse, reason: 'un campo ausente se toma como no soportado');
    });

    test('marca como no usable una cuenta rechazada', () {
      final rejected = ExternalBankAccountModel.fromJson({..._account, 'verificationStatus': 'REJECTED', 'accountType': 'CHECKING'});

      expect(rejected.isUsable, isFalse);
      expect(rejected.statusLabel, 'Rechazada');
      expect(rejected.accountTypeLabel, 'Corriente');
    });
  });

  group('Retiro a cuenta externa', () {
    PayoutNotifier payout() => container.read(payoutNotifierProvider.notifier);
    PayoutState payoutState() => container.read(payoutNotifierProvider);

    ExternalBankAccountModel account() => ExternalBankAccountModel.fromJson(_account);

    test('envía el retiro con el PIN cifrado y la llave de idempotencia', () async {
      server.on('POST', '/v1/accounts/transactions/payout', const FakeResponse({'message': 'Retiro iniciado con éxito'}));
      payout()
        ..selectAccount(account())
        ..updateAmount(150000)
        ..updateConcept('Ganancias')
        ..updateOtp('482913');

      expect(await payout().executePayout('4821'), isTrue);

      final sent = server.last.json;
      expect(sent['externalBankAccountId'], 'ext-1');
      expect(sent['idempotencyKey'], payoutState().idempotencyKey);
      expect(sent['otp'], '482913');
      expect(decryptWithTestPrivateKey(sent['pin'] as String), '4821');
      expect(payoutState().step, 2);
    });

    test('muestra el motivo cuando el retiro es rechazado y no se queda cargando con errores raros', () async {
      payout()
        ..selectAccount(account())
        ..updateAmount(150000);

      server.on('POST', '/v1/accounts/transactions/payout',
          const FakeResponse({'code': 'INSUFFICIENT_EARNINGS', 'message': 'El monto supera tus ganancias'}, status: 422));
      expect(await payout().executePayout('4821'), isFalse);
      expect(payoutState().error, 'El monto supera tus ganancias');

      server.on('POST', '/v1/accounts/transactions/payout', const FakeResponse('<html>502</html>', status: 502));
      expect(await payout().executePayout('4821'), isFalse);
      expect(payoutState().error, 'Ocurrió un error inesperado');
      expect(payoutState().isLoading, isFalse);
    });

    test('no envía nada sin cuenta, sin monto o sin PIN', () async {
      expect(await payout().executePayout('4821'), isFalse);
      payout().selectAccount(account());
      expect(await payout().executePayout('4821'), isFalse);
      payout().updateAmount(1000);
      expect(await payout().executePayout(''), isFalse);

      expect(server.requests.where((r) => r.method == 'POST'), isEmpty);
    });

    test('pide el OTP y vuelve atrás entre pasos', () async {
      server.on('POST', '/v1/accounts/transactions/transfer/otp/request', const FakeResponse({}, status: 500));
      await payout().requestOtp();
      expect(payoutState().error, 'Error solicitando código OTP');

      server.on('POST', '/v1/accounts/transactions/transfer/otp/request', const FakeResponse({}));
      await payout().requestOtp();
      expect(payoutState().error, isNull);

      payout()
        ..selectAccount(account())
        ..previousStep()
        ..previousStep();
      expect(payoutState().step, 0);
    });
  });
}
