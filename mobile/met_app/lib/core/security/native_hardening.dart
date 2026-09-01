// Endurecimiento de seguridad que solo aplica en Android/iOS.
//
// `freeRASP` (Talsec) importa `dart:io` en su código Dart, así que no puede
// entrar en el grafo de compilación de web. Este import condicional deja el
// código real en `native_hardening_native.dart` y un no-op en
// `native_hardening_web.dart`.
export 'native_hardening_web.dart'
    if (dart.library.io) 'native_hardening_native.dart';
