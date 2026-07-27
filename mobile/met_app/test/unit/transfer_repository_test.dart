import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:met/features/transfers/domain/repositories/transfers_repository.dart';
import 'package:met/features/transfers/data/models/verify_recipient_model.dart';
import 'package:met/features/transfers/data/models/transfer_request_model.dart';
import 'package:met/features/transfers/data/models/core_account_model.dart';

class MockTransfersRepository extends Mock implements TransfersRepository {}

class FakeTransferRequestModel extends Fake implements TransferRequestModel {}

void main() {
  late MockTransfersRepository repository;

  setUpAll(() {
    registerFallbackValue(FakeTransferRequestModel());
  });

  setUp(() {
    repository = MockTransfersRepository();
  });

  group('TransfersRepository', () {
    test('verifyRecipient returns recipient info', () async {
      final mockRecipient = VerifyRecipientModel(
        accountId: 'acc-123',
        ownerName: 'Juan Perez',
      );

      when(() => repository.verifyRecipient(any()))
          .thenAnswer((_) async => mockRecipient);

      final result = await repository.verifyRecipient('100020003000');

      expect(result.ownerName, 'Juan Perez');
      expect(result.accountId, 'acc-123');
    });

    test('executeTransfer completes successfully', () async {
      when(() => repository.executeTransfer(any()))
          .thenAnswer((_) async => {});

      final req = TransferRequestModel(
        destinationAccountId: 'acc-123',
        amount: 50000,
        concept: 'Pago prestamo',
        pin: '1234',
        otp: '123456',
        idempotencyKey: 'idem-123'
      );

      await repository.executeTransfer(req);
      
      verify(() => repository.executeTransfer(any())).called(1);
    });
    
    test('executeTransfer throws when funds are insufficient', () async {
      when(() => repository.executeTransfer(any()))
          .thenThrow(Exception('Insufficient funds'));

      final req = TransferRequestModel(
        destinationAccountId: 'acc-123',
        amount: 9999999,
        concept: 'Pago gigante',
        pin: '1234',
        otp: '123456',
        idempotencyKey: 'idem-124'
      );

      expect(
        () => repository.executeTransfer(req),
        throwsA(isA<Exception>()),
      );
    });
  });
}
