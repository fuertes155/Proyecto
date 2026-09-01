// Fachada de utilidades que dependen de la plataforma.
//
// El navegador no tiene `dart:io` (ni `File`, `HttpClient`, `Platform`,
// `path_provider`), así que cualquier código que lo use debe vivir detrás de
// este import condicional: en web se resuelve a `platform_web.dart` (stubs) y
// en Android/iOS a `platform_native.dart` (implementación real).
export 'platform_web.dart' if (dart.library.io) 'platform_native.dart';
