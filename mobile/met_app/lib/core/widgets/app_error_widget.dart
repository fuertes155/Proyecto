import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

/// Reemplazo del "pantallazo rojo" por defecto de Flutter cuando el build de
/// una pantalla lanza una excepción no capturada (p. ej. un DioException que
/// se escapó de un try/catch). Se instala en main() con:
///
///   ErrorWidget.builder = (details) => AppErrorView(details: details);
///
/// En vez de volcar el stacktrace en rojo a pantalla completa, muestra un
/// mensaje amable. En debug deja ver el detalle técnico plegado, para no
/// perder información al desarrollar.
class AppErrorView extends StatelessWidget {
  const AppErrorView({super.key, required this.details});

  final FlutterErrorDetails details;

  @override
  Widget build(BuildContext context) {
    // Puede renderizarse fuera de un Scaffold/Directionality, así que se
    // envuelve en lo mínimo para que no falle a su vez.
    return Directionality(
      textDirection: TextDirection.ltr,
      child: Material(
        color: const Color(0xFFF5F5F5),
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.sentiment_dissatisfied,
                    size: 56, color: Color(0xFF9E9E9E)),
                const SizedBox(height: 16),
                const Text(
                  'Algo salió mal en esta pantalla',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Vuelve atrás e inténtalo de nuevo. Si el problema sigue, '
                  'cierra y abre la aplicación.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Color(0xFF616161)),
                ),
                if (kDebugMode) ...[
                  const SizedBox(height: 20),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFFECECEC),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      details.exceptionAsString(),
                      style: const TextStyle(
                          fontSize: 11, fontFamily: 'monospace', color: Color(0xFF424242)),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
