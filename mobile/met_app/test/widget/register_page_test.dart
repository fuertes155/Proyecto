import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:met/features/auth/presentation/pages/register_page.dart';

void main() {
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

  testWidgets('RegisterPage lets the user switch to cédula de extranjería', (WidgetTester tester) async {
    await pumpRegisterPage(tester);

    await tester.tap(find.text('¿Tu cédula es de extranjería?'));
    await tester.pump();

    expect(find.widgetWithText(TextFormField, 'Número de cédula de extranjería'), findsOneWidget);
    expect(find.text('Usar cédula de ciudadanía'), findsOneWidget);
  });
}
