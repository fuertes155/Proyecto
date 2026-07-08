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
  'saldo': 'Tu saldo está en el ahorro principal de Inicio. Para ahorros o créditos, revisa cada sección desde el menú.',

  'creditos': 'Ve a Créditos en el menú para ver saldo pendiente, cuotas y fechas de pago. Para un crédito nuevo, visita una oficina o llama al 01 8000 XXX XXX.',

  'transferencias': 'Ve a Inicio → Transferir, ingresa cuenta destino y monto, confirma con tu PIN. Máximo según tus límites de operación.',

  'contacto': '📞 01 8000 XXX XXX (lun–sáb, 7am–7pm)\n💬 WhatsApp: +57 300 XXX XXXX\n✉️ atencion@met.coop',

  'faq': '🔐 Olvidé PIN → Ajustes → Cambiar PIN\n💳 Cuenta bloqueada → Llama al 01 8000 XXX XXX\n📄 Extracto → Movimientos → Descargar\n📱 Actualizar datos → Perfil → Mis Datos',
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
            'Soy el asistente virtual de MET.',
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
    return 'Para más ayuda llama al 01 8000 XXX XXX (lun–sáb 7am–7pm) o escríbenos al WhatsApp. 😊';
  }
}

final chatProvider = StateNotifierProvider<ChatNotifier, ChatState>((ref) {
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
  return ChatNotifier(dio);
});
