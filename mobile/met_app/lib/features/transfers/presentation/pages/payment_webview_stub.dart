import 'package:flutter/material.dart';

/// Stub de MobileWebViewPage para la plataforma web.
/// En web, PaymentWebViewPage usa url_launcher directamente,
/// por lo que este widget nunca se instancia.
class MobileWebViewPage extends StatelessWidget {
  final String paymentUrl;
  final String method;
  final double amount;

  const MobileWebViewPage({
    super.key,
    required this.paymentUrl,
    required this.method,
    required this.amount,
  });

  @override
  Widget build(BuildContext context) {
    // Este widget no debe renderizarse en web — PaymentWebViewPage lo evita
    // verificando kIsWeb antes de delegarlo aquí.
    return const SizedBox.shrink();
  }
}
