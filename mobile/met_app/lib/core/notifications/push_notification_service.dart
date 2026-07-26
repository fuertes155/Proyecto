import 'dart:developer';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../storage/secure_storage_service.dart';

/// Handler de notificaciones en background (debe ser top-level)
@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  log('Manejo de mensaje en background: ${message.messageId}');
}

final pushNotificationServiceProvider = Provider<PushNotificationService>((ref) {
  return PushNotificationService(ref);
});

class PushNotificationService {
  final Ref _ref;
  final FirebaseMessaging _messaging = FirebaseMessaging.instance;

  PushNotificationService(this._ref);

  Future<void> initialize() async {
    // 1. Solicitar permisos (iOS y Android 13+)
    NotificationSettings settings = await _messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      provisional: false,
    );

    if (settings.authorizationStatus == AuthorizationStatus.authorized) {
      log('User granted permission');
    } else {
      log('User declined or has not accepted permission');
      return; // Si no aceptan, no seguimos.
    }

    // 2. Registrar handler para background
    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

    // 3. Manejo en foreground
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      log('Mensaje recibido en foreground: ${message.notification?.title}');
      // Aquí se podría usar un local_notifications plugin o Riverpod para
      // actualizar un contador de "notificaciones sin leer"
    });

    // 4. Manejo cuando se abre la app desde una notificación (background to foreground)
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      log('App abierta desde notificación: ${message.data}');
      // Aquí manejaríamos el deeplinking dependiendo del payload en message.data
    });

    // 5. Obtener token de FCM
    await updateFcmToken();

    // 6. Escuchar cuando se refresca el token
    _messaging.onTokenRefresh.listen((newToken) {
      _saveTokenToBackend(newToken);
    });
  }

  Future<void> updateFcmToken() async {
    try {
      String? token = await _messaging.getToken();
      if (token != null) {
        log('FCM Token: $token');
        await _saveTokenToBackend(token);
      }
    } catch (e) {
      log('Error obteniendo FCM token: $e');
    }
  }

  Future<void> _saveTokenToBackend(String token) async {
    // Solo guardamos si hay un usuario autenticado
    final storage = _ref.read(secureStorageProvider);
    final accessToken = await storage.readAccessToken();
    
    if (accessToken != null && accessToken.isNotEmpty) {
      // Guardar localmente
      await storage.write(key: 'fcm_token', value: token);
      
      // Enviar al backend (TODO: Implementar la llamada en el ApiClient real)
      // _ref.read(apiClientProvider).post('/v1/users/fcm-token', data: {'token': token});
    }
  }
}
