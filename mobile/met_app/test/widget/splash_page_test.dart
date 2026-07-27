import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:met/features/auth/presentation/pages/splash_page.dart';

void main() {
  testWidgets('SplashPage renders correctly', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(
          home: SplashPage(),
        ),
      ),
    );

    // Verify loading indicator is present
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
