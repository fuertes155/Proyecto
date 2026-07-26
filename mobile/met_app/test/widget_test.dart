import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:met/main.dart';
import 'package:met/features/auth/presentation/pages/splash_page.dart';

// Definir los mocks de method channels antes del main
typedef Callback = void Function(MethodCall call);

void setupFirebaseAuthMocks([Callback? customHandlers]) {
  TestWidgetsFlutterBinding.ensureInitialized();
  
  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(
        const MethodChannel('plugins.flutter.io/firebase_core'),
        (MethodCall methodCall) async {
          if (methodCall.method == 'Firebase#initializeCore') {
            return [
              {
                'name': '[DEFAULT]',
                'options': {
                  'apiKey': '123',
                  'appId': '123',
                  'messagingSenderId': '123',
                  'projectId': '123',
                },
                'pluginConstants': {},
              }
            ];
          }
          if (methodCall.method == 'Firebase#initializeApp') {
            return {
              'name': methodCall.arguments['appName'],
              'options': methodCall.arguments['options'],
              'pluginConstants': {},
            };
          }
          if (customHandlers != null) {
            customHandlers(methodCall);
          }
          return null;
        },
      );
}

void main() {
  setupFirebaseAuthMocks();
  
  testWidgets('App starts and shows SplashPage', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    // Envuelto en ProviderScope porque usamos Riverpod en toda la app.
    await tester.pumpWidget(const ProviderScope(child: MetApp()));

    // Verify that the initial route resolves to SplashPage
    expect(find.byType(SplashPage), findsOneWidget);
  });
}
