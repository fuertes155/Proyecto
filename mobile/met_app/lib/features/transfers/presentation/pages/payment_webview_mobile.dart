import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:webview_flutter/webview_flutter.dart';

/// Implementación nativa del WebView para Android e iOS.
/// Este archivo solo se importa en plataformas móviles.
class MobileWebViewPage extends StatefulWidget {
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
  State<MobileWebViewPage> createState() => _MobileWebViewPageState();
}

class _MobileWebViewPageState extends State<MobileWebViewPage> {
  late final WebViewController _controller;
  bool _isLoading = true;
  String? _errorMessage;

  static const _successIndicators = ['metapp://deposit/success', 'deposit/success'];
  static const _failureIndicators = ['metapp://deposit/failure', 'deposit/failure', 'deposit/cancel'];

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.white)
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageStarted: (url) {
            setState(() => _isLoading = true);
            _checkCompletion(url);
          },
          onPageFinished: (url) {
            setState(() => _isLoading = false);
            _checkCompletion(url);
          },
          onNavigationRequest: (request) {
            if (request.url.toLowerCase().startsWith('metapp://')) {
              _handleDeepLink(request.url);
              return NavigationDecision.prevent;
            }
            return NavigationDecision.navigate;
          },
          onWebResourceError: (error) {
            if (error.errorCode == -6 || error.errorCode == -1) return;
            setState(() {
              _isLoading = false;
              _errorMessage = 'Error cargando la pasarela (${error.errorCode})';
            });
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.paymentUrl));
  }

  void _checkCompletion(String url) {
    final lower = url.toLowerCase();
    if (_successIndicators.any((i) => lower.contains(i))) {
      _onSuccess();
    } else if (_failureIndicators.any((i) => lower.contains(i))) {
      _onFailure();
    }
  }

  void _handleDeepLink(String url) {
    if (_successIndicators.any((i) => url.toLowerCase().contains(i))) {
      _onSuccess();
    } else {
      _onFailure();
    }
  }

  void _onSuccess() {
    if (!mounted) return;
    context.pushReplacement('/deposit/waiting', extra: {
      'method': widget.method,
      'amount': widget.amount,
    });
  }

  void _onFailure() {
    if (!mounted) return;
    context.pop();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Text('El pago fue cancelado o rechazado.'),
        backgroundColor: Colors.red.shade700,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close, color: Colors.black87),
          onPressed: _showCancelDialog,
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(widget.method,
                style: const TextStyle(color: Colors.black87, fontWeight: FontWeight.bold, fontSize: 16)),
            const Text('Pago seguro', style: TextStyle(color: Colors.black38, fontSize: 11)),
          ],
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Row(children: [
              Icon(Icons.lock_outline, size: 14, color: Colors.green.shade600),
              const SizedBox(width: 4),
              Text('SSL',
                  style: TextStyle(
                      color: Colors.green.shade600, fontSize: 11, fontWeight: FontWeight.bold)),
            ]),
          ),
        ],
      ),
      body: Stack(
        children: [
          if (_errorMessage == null) WebViewWidget(controller: _controller) else _buildError(),
          if (_isLoading && _errorMessage == null)
            Container(
              color: Colors.white,
              child: const Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    CircularProgressIndicator(color: Color(0xFF0044ff), strokeWidth: 2),
                    SizedBox(height: 16),
                    Text('Cargando pasarela...', style: TextStyle(color: Colors.black54, fontSize: 14)),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.wifi_off_rounded, size: 64, color: Colors.grey.shade400),
            const SizedBox(height: 16),
            Text(_errorMessage ?? 'Error de conexión',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey.shade600, fontSize: 14)),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () {
                setState(() { _isLoading = true; _errorMessage = null; });
                _controller.loadRequest(Uri.parse(widget.paymentUrl));
              },
              icon: const Icon(Icons.refresh),
              label: const Text('Reintentar'),
              style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF0044ff), foregroundColor: Colors.white),
            ),
          ],
        ),
      ),
    );
  }

  void _showCancelDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('¿Cancelar pago?'),
        content: const Text('Si sales ahora, tendrás que iniciar el proceso de nuevo.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Continuar pago')),
          ElevatedButton(
            onPressed: () { Navigator.pop(ctx); context.pop(); },
            style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red.shade600, foregroundColor: Colors.white),
            child: const Text('Cancelar'),
          ),
        ],
      ),
    );
  }
}
