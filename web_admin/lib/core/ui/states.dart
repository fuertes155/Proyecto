import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../theme/app_theme.dart';

/// Convierte cualquier error en un mensaje que un administrador pueda entender.
String friendlyError(Object? error) {
  if (error is DioException) {
    final status = error.response?.statusCode;
    final data = error.response?.data;
    if (status == 401) return 'Tu sesión expiró. Vuelve a iniciar sesión para continuar.';
    if (status == 403) return 'Tu usuario no tiene permisos para esta acción.';
    if (data is Map && data['message'] is String && (data['message'] as String).isNotEmpty) {
      return data['message'] as String;
    }
    switch (error.type) {
      case DioExceptionType.connectionError:
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.receiveTimeout:
      case DioExceptionType.sendTimeout:
        return 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.';
      default:
        break;
    }
    if (status == 404) return 'No encontramos lo que buscabas. Es posible que ya no exista.';
    if (status != null && status >= 500) return 'El servidor tuvo un problema. Inténtalo de nuevo en unos minutos.';
    return 'No se pudo completar la acción${status != null ? ' (código $status)' : ''}.';
  }
  if (error is String && error.isNotEmpty) return error;
  if (error is FormatException) return 'Los datos recibidos no tienen el formato esperado.';
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
}

bool isSessionExpired(Object? error) => error is DioException && error.response?.statusCode == 401;

class LoadingState extends StatelessWidget {
  const LoadingState({super.key, this.label = 'Cargando…', this.compact = false});

  final String label;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.symmetric(vertical: compact ? 24 : 64),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(width: 28, height: 28, child: CircularProgressIndicator(strokeWidth: 2.6)),
          const SizedBox(height: 14),
          Text(label, style: const TextStyle(color: MetColors.textMuted, fontSize: 14)),
        ],
      ),
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.icon,
    required this.title,
    this.message,
    this.action,
  });

  final IconData icon;
  final String title;
  final String? message;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48, horizontal: 24),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 420),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(color: MetColors.successBg, borderRadius: BorderRadius.circular(18)),
                child: Icon(icon, color: MetColors.primary, size: 30),
              ),
              const SizedBox(height: 16),
              Text(title,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700, color: MetColors.text)),
              if (message != null) ...[
                const SizedBox(height: 6),
                Text(message!,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 14, height: 1.5, color: MetColors.textMuted)),
              ],
              if (action != null) ...[const SizedBox(height: 18), action!],
            ],
          ),
        ),
      ),
    );
  }
}

class ErrorState extends StatelessWidget {
  const ErrorState({super.key, required this.error, this.onRetry, this.title = 'No pudimos cargar esta información'});

  final Object? error;
  final VoidCallback? onRetry;
  final String title;

  @override
  Widget build(BuildContext context) {
    final expired = isSessionExpired(error);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48, horizontal: 24),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 440),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(color: MetColors.dangerBg, borderRadius: BorderRadius.circular(18)),
                child: Icon(expired ? Icons.lock_clock_rounded : Icons.cloud_off_rounded,
                    color: MetColors.danger, size: 30),
              ),
              const SizedBox(height: 16),
              Text(expired ? 'Tu sesión expiró' : title,
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700, color: MetColors.text)),
              const SizedBox(height: 6),
              Text(friendlyError(error),
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 14, height: 1.5, color: MetColors.textMuted)),
              const SizedBox(height: 18),
              Wrap(
                spacing: 10,
                runSpacing: 10,
                alignment: WrapAlignment.center,
                children: [
                  if (expired)
                    FilledButton.icon(
                      onPressed: () => context.go('/admin/login'),
                      icon: const Icon(Icons.login_rounded, size: 18),
                      label: const Text('Iniciar sesión'),
                    ),
                  if (onRetry != null)
                    OutlinedButton.icon(
                      onPressed: onRetry,
                      icon: const Icon(Icons.refresh_rounded, size: 18),
                      label: const Text('Reintentar'),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
