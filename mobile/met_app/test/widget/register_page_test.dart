import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:met/features/auth/data/models/auth_models.dart';
import 'package:met/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:met/features/auth/domain/repositories/auth_repository.dart';
import 'package:met/features/auth/presentation/pages/register_page.dart';
import 'package:mocktail/mocktail.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

class FakeRegisterRequest extends Fake implements RegisterRequest {}

void main() {
  setUpAll(() {
    registerFallbackValue(FakeRegisterRequest());
  });

  Future<void> pumpRegisterPage(WidgetTester tester) {
    return tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(
          home: RegisterPage(),
        ),
      ),
    );
  }

  testWidgets('RegisterPage only asks for cédula, full name and PIN', (WidgetTester tester) async {
    await pumpRegisterPage(tester);

    expect(find.byType(TextFormField), findsNWidgets(3));
    expect(find.widgetWithText(TextFormField, 'Número de cédula'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Nombre completo'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'PIN de 4 dígitos'), findsOneWidget);
    expect(find.text('Correo electrónico'), findsNothing);
    expect(find.text('Teléfono (10 dígitos)'), findsNothing);
  });

  testWidgets('RegisterPage lets the user choose the type of cédula', (WidgetTester tester) async {
    await pumpRegisterPage(tester);

    // Las dos opciones están a la vista; ciudadanía viene elegida por defecto.
    expect(find.text('Ciudadanía'), findsOneWidget);
    expect(find.text('Extranjería'), findsOneWidget);
    expect(tester.widget<SegmentedButton<String>>(find.byType(SegmentedButton<String>)).selected, {'CC'});

    await tester.tap(find.text('Extranjería'));
    await tester.pump();

    expect(tester.widget<SegmentedButton<String>>(find.byType(SegmentedButton<String>)).selected, {'CE'});
    expect(find.widgetWithText(TextFormField, 'Número de cédula de extranjería'), findsOneWidget);
  });

  testWidgets('RegisterPage rejects a name without surname', (WidgetTester tester) async {
    await pumpRegisterPage(tester);

    await tester.enterText(find.widgetWithText(TextFormField, 'Número de cédula'), '1086578123');
    await tester.enterText(find.widgetWithText(TextFormField, 'Nombre completo'), 'Juan');
    await tester.enterText(find.widgetWithText(TextFormField, 'PIN de 4 dígitos'), '1234');
    await tester.ensureVisible(find.text('Registrarme'));
    await tester.tap(find.text('Registrarme'));
    await tester.pump();

    expect(find.text('Escribe tu nombre y apellido'), findsOneWidget);
  });

  testWidgets('RegisterPage registers a cédula de extranjería as CE', (WidgetTester tester) async {
    final repository = MockAuthRepository();
    when(() => repository.register(any())).thenAnswer(
      (_) async => UserResponse(
        id: 'user-1',
        firstName: 'Ana',
        lastName: 'Pérez',
        email: '',
        phone: '',
        status: 'ACTIVE',
        kycStatus: 'PENDING',
        emailNotificationsEnabled: true,
        pushNotificationsEnabled: true,
      ),
    );
    final router = GoRouter(
      routes: [
        GoRoute(path: '/', builder: (context, state) => const RegisterPage()),
        GoRoute(path: '/login', builder: (context, state) => const Scaffold(body: Text('pantalla de login'))),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [authRepositoryProvider.overrideWithValue(repository)],
        child: MaterialApp.router(routerConfig: router),
      ),
    );

    await tester.tap(find.text('Extranjería'));
    await tester.pump();
    await tester.enterText(find.widgetWithText(TextFormField, 'Número de cédula de extranjería'), '654321');
    await tester.enterText(find.widgetWithText(TextFormField, 'Nombre completo'), 'Ana Pérez');
    await tester.enterText(find.widgetWithText(TextFormField, 'PIN de 4 dígitos'), '1234');
    await tester.ensureVisible(find.byType(Checkbox));
    await tester.tap(find.byType(Checkbox));
    await tester.pump();
    await tester.ensureVisible(find.text('Registrarme'));
    await tester.tap(find.text('Registrarme'));
    await tester.pumpAndSettle();

    final request = verify(() => repository.register(captureAny())).captured.single as RegisterRequest;
    expect(request.documentType, 'CE');
    expect(request.documentNumber, '654321');
    expect(request.fullName, 'Ana Pérez');
    // Con la cuenta creada la app lleva al login.
    expect(find.text('pantalla de login'), findsOneWidget);
  });
}
