import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/core/network/api_client_provider.dart';
import 'package:met/features/auth/data/models/auth_models.dart';
import 'package:met/features/auth/presentation/providers/auth_provider.dart';
import 'package:met/features/external_accounts/presentation/providers/external_accounts_provider.dart';
import 'package:met/features/home/presentation/providers/home_provider.dart';
import 'package:met/features/investment/presentation/providers/investment_providers.dart';
import 'package:met/features/loans/presentation/providers/loan_provider.dart';
import 'package:met/features/transfers/presentation/providers/transfers_provider.dart';

import '../../helpers/fake_dio.dart';
import '../../helpers/test_rsa_keys.dart';

/// Un mismo celular, dos socios: lo que cargó el primero no debe verlo el segundo.
///
/// La app no se reinicia entre un cierre de sesión y el siguiente ingreso, así que los datos de la
/// cuenta que quedaron en memoria deben descartarse al cambiar de socio.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeDioAdapter server;
  late ProviderContainer container;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    server = FakeDioAdapter()
      ..on('GET', '/v1/auth/public-key', const FakeResponse({'publicKey': testRsaPublicKeyBase64}))
      ..on('POST', '/v1/auth/logout', const FakeResponse({}));
    container = ProviderContainer(overrides: [apiClientProvider.overrideWithValue(fakeDio(server))]);
  });

  tearDown(() => container.dispose());

  /// El servidor responde como la cuenta de [name] (cuenta terminada en [account]).
  void serveMember(String id, String name, String account) {
    server
      ..on('POST', '/v1/auth/login', FakeResponse({'userId': id, 'accessToken': 'acc-$id', 'refreshToken': 'ref-$id', 'expiresInMs': 1800000}))
      ..on('GET', '/v1/auth/me', FakeResponse({'id': id, 'firstName': name, 'lastName': 'X', 'status': 'ACTIVE'}))
      ..on('GET', '/v1/account/summary', FakeResponse({'data': {'userId': id, 'accountNumber': account, 'principalBalance': 1}}))
      ..on('GET', '/v1/accounts/me', FakeResponse({
        'id': 'acc-$id', 'accountNumber': account, 'principalBalance': 1, 'interestBalance': 1, 'status': 'ACTIVE',
      }))
      ..on('GET', '/v1/investments/my-summary', FakeResponse({
        'totalInvested': account.length * 1000, 'activeAmount': 0, 'availableAmount': 0, 'paidOffAmount': 0,
        'returnedAmount': 0, 'loansFundedCount': 0,
      }))
      ..on('GET', '/v1/loans/applications', FakeResponse([
        {'id': 'loan-$id', 'amount': 1, 'termMonths': 1, 'monthlyPayment': 1, 'totalPayment': 1, 'purpose': name, 'status': 'IN_REVIEW',
          'submittedAt': '2026-10-20T10:00:00Z'},
      ]))
      ..on('GET', '/v1/accounts/external-bank-accounts', FakeResponse([
        {'id': 'ext-$id', 'bankCode': '1', 'bankName': name, 'accountType': 'SAVINGS', 'maskedAccountNumber': account,
          'verificationStatus': 'VERIFIED', 'createdAt': '2026-10-20T10:00:00Z'},
      ]));
  }

  Future<void> logIn(String document) async {
    await container
        .read(authStateProvider.notifier)
        .loginWithPin(LoginRequest(documentType: 'CC', documentNumber: document, pin: '4821'));
    expect(container.read(authStateProvider).hasError, isFalse, reason: '${container.read(authStateProvider)}');
  }

  Future<void> loadEverything() async {
    container.listen(externalBankAccountsListProvider, (_, __) {});
    await container.read(homeProvider.future);
    await container.read(myAccountProvider.future);
    await container.read(investmentSummaryProvider.future);
    await container.read(loanApplicationsProvider.future);
    await container.read(externalBankAccountsListProvider.notifier).load();
  }

  test('al entrar otro socio, la app consulta sus datos en vez de mostrar los del anterior', () async {
    serveMember('a', 'Ana', '4510001111');
    await logIn('111');
    await loadEverything();
    await container.read(authStateProvider.notifier).logout();

    serveMember('b', 'Beto', '4510002222333');
    await logIn('222');

    expect((await container.read(homeProvider.future)).accountNumber, '4510002222333');
    expect((await container.read(myAccountProvider.future)).accountNumber, '4510002222333');
    expect((await container.read(investmentSummaryProvider.future)).totalInvested, 13000);
    expect((await container.read(loanApplicationsProvider.future)).single.id, 'loan-b');
    expect(container.read(externalBankAccountsListProvider).valueOrNull?.single.bankName, isNot('Ana'),
        reason: 'las cuentas bancarias del socio anterior no deben quedar en pantalla');
  });
}
