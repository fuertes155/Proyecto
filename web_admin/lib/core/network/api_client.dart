import 'dart:convert';

import 'package:http/http.dart' as http;

class ApiClient {
  final String baseUrl;

  const ApiClient({
    required this.baseUrl,
  });

  Future<http.Response> postJson(
    String path, {
    required Map<String, dynamic> body,
    Map<String, String>? headers,
  }) {
    final uri = Uri.parse('$baseUrl$path');
    return http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        ...?headers,
      },
      body: jsonEncode(body),
    );
  }
}
