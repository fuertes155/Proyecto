import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';

class ChatMessage {
  final String text;
  final bool isUser;
  ChatMessage(this.text, this.isUser);
}

class ChatState {
  final List<ChatMessage> messages;
  final bool isLoading;
  ChatState(this.messages, this.isLoading);
}

class ChatNotifier extends StateNotifier<ChatState> {
  final Dio dio;

  ChatNotifier(this.dio)
      : super(ChatState([
          ChatMessage('¡Hola! Soy tu asistente 24/7 con IA. ¿En qué te puedo ayudar hoy?', false)
        ], false));

  Future<void> sendMessage(String text) async {
    if (text.trim().isEmpty) return;
    
    // Add user message
    final newMessages = [...state.messages, ChatMessage(text, true)];
    state = ChatState(newMessages, true);

    try {
      final response = await dio.post(
        '/api/support/chat', // Requires the base URL to be configured in dio
        data: {'message': text},
      );
      
      final replyText = response.data['reply'] ?? 'Sin respuesta';
      state = ChatState([...newMessages, ChatMessage(replyText, false)], false);
    } catch (e) {
      state = ChatState([
        ...newMessages,
        ChatMessage('Error al conectar con la IA. Intenta de nuevo más tarde.', false)
      ], false);
    }
  }
}

final chatProvider = StateNotifierProvider<ChatNotifier, ChatState>((ref) {
  // Configured dio should come from a core provider, here we assume it points to localhost:8080 or backend
  // For simplicity, we just create one pointing to localhost for now, in a real app use ref.read(dioProvider)
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
  return ChatNotifier(dio);
});
