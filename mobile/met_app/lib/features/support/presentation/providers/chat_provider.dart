import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';

class ChatMessage {
  final String text;
  final bool isUser;
  final String? category; // categoría del chip que lo originó
  ChatMessage(this.text, this.isUser, {this.category});
}

class ChatState {
  final List<ChatMessage> messages;
  final bool isLoading;
  final bool showQuickOptions; // mostrar chips de opciones rápidas
  ChatState(this.messages, this.isLoading, {this.showQuickOptions = true});
}

// ── Respuestas predefinidas por opción rápida ─────────────────────────────────
const _quickReplies = {
  'saldo': '''💰 **Consulta de Saldo**

Para consultar tu saldo:
• Ve a la sección **Inicio** de la app
• Tu saldo disponible aparece en la tarjeta virtual principal

Para el saldo de ahorros o créditos, dirígete a las secciones **Ahorros** o **Créditos** respectivamente.

¿Necesitas ayuda con algo más?''',

  'creditos': '''📋 **Mis Créditos**

Para revisar tus créditos activos:
• Dirígete a la sección **Créditos** en el menú principal
• Allí verás el saldo pendiente, cuotas y próximas fechas de pago

Para solicitar un crédito nuevo, contacta a un asesor de la cooperativa o visita la oficina más cercana.

¿Tienes alguna otra pregunta?''',

  'transferencias': '''🔄 **Transferencias**

Para realizar una transferencia:
1. Ve a **Inicio** → botón de transferencia
2. Ingresa el número de cuenta destino
3. Digita el monto (máximo según tus límites de operación)
4. Confirma con tu PIN

**Límites actuales:** consulta la sección de información o llama a la línea de atención.

¿Hay algo más en lo que pueda ayudarte?''',

  'contacto': '''📞 **Contáctanos**

Estamos aquí para ayudarte:

• **Línea de atención:** 01 8000 XXX XXX (lunes a sábado 7am – 7pm)
• **WhatsApp:** +57 300 XXX XXXX
• **Correo:** atencion@met.coop
• **Oficinas:** [Ver sucursales]

Para emergencias fuera de horario, usa la opción de **Bloqueo de cuenta** en la app.''',

  'faq': '''❓ **Preguntas Frecuentes**

Selecciona el tema que necesitas:

🔐 **Olvidé mi PIN** → Ve a Ajustes → Cambiar PIN
💳 **Bloqueé mi cuenta** → Llama al 01 8000 XXX XXX
📄 **Extracto de cuenta** → Sección Movimientos → Descargar
🏦 **Abrir otro producto** → Visita una de nuestras oficinas
📱 **Actualizar datos** → Perfil → Mis Datos

¿Necesitas más información sobre algún tema?''',
};

// ── Opciones rápidas disponibles ─────────────────────────────────────────────
const quickOptions = [
  ('💰 Saldo', 'saldo'),
  ('📋 Créditos', 'creditos'),
  ('🔄 Transferencias', 'transferencias'),
  ('📞 Contacto', 'contacto'),
  ('❓ Preguntas frecuentes', 'faq'),
];

class ChatNotifier extends StateNotifier<ChatState> {
  final Dio dio;

  ChatNotifier(this.dio)
      : super(ChatState([
          ChatMessage(
            '¡Hola! Soy tu asistente 24/7 de la Cooperativa MET 👋\n\n'
            'Puedo ayudarte con información sobre tus productos, transferencias, créditos y más.\n\n'
            'Selecciona una opción o escríbeme directamente:',
            false,
          )
        ], false, showQuickOptions: true));

  Future<void> sendMessage(String text) async {
    if (text.trim().isEmpty) return;

    final newMessages = [
      ...state.messages,
      ChatMessage(text, true),
    ];
    // Ocultar chips tras primer mensaje del usuario
    state = ChatState(newMessages, true, showQuickOptions: false);

    try {
      final response = await dio.post(
        '/api/support/chat',
        data: {'message': text},
      );

      final replyText = response.data['reply'] as String? ?? 'Sin respuesta';
      state = ChatState([...newMessages, ChatMessage(replyText, false)], false,
          showQuickOptions: false);
    } catch (_) {
      // Si el backend falla, buscar respuesta predefinida por palabra clave
      final fallback = _getFallbackReply(text);
      state = ChatState([...newMessages, ChatMessage(fallback, false)], false,
          showQuickOptions: false);
    }
  }

  /// Enviar un mensaje de opción rápida (chip)
  Future<void> sendQuickOption(String label, String category) async {
    final newMessages = [
      ...state.messages,
      ChatMessage(label, true, category: category),
    ];
    state = ChatState(newMessages, false, showQuickOptions: false);

    // Respuesta predefinida inmediata
    final reply = _quickReplies[category] ??
        'Lo siento, no tengo información sobre esa opción en este momento. Por favor llama al 01 8000 XXX XXX.';

    await Future.delayed(const Duration(milliseconds: 400));
    state = ChatState(
      [...newMessages, ChatMessage(reply, false)],
      false,
      showQuickOptions: false,
    );
  }

  /// Respuesta fallback por palabras clave cuando el backend no responde
  String _getFallbackReply(String text) {
    final lower = text.toLowerCase();
    if (lower.contains('saldo') || lower.contains('balance')) {
      return _quickReplies['saldo']!;
    }
    if (lower.contains('crédit') || lower.contains('credit') || lower.contains('préstamo')) {
      return _quickReplies['creditos']!;
    }
    if (lower.contains('transfer') || lower.contains('enviar') || lower.contains('pagar')) {
      return _quickReplies['transferencias']!;
    }
    if (lower.contains('contacto') || lower.contains('teléfono') || lower.contains('llamar')) {
      return _quickReplies['contacto']!;
    }
    return 'Entendí tu mensaje. Para darte la mejor atención, por favor llama a nuestra línea **01 8000 XXX XXX** (lunes a sábado, 7am–7pm) o escríbenos al WhatsApp. Un asesor estará contigo en minutos. 😊';
  }
}

final chatProvider = StateNotifierProvider<ChatNotifier, ChatState>((ref) {
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
  return ChatNotifier(dio);
});
