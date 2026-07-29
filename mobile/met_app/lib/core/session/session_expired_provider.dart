import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Se pone en true cuando el interceptor de red detecta un 401 (sesión
/// inválida/expirada) y fuerza el regreso a /login. La pantalla de login lo
/// lee una vez para mostrar un mensaje explicativo y lo vuelve a poner en false.
final sessionExpiredProvider = StateProvider<bool>((ref) => false);
