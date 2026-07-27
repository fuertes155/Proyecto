import 'dart:convert';
import 'dart:io';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:http/http.dart' as http;

class NoInternetException implements Exception {
  final String message;
  NoInternetException([this.message = 'No hay conexión a internet.']);

  @override
  String toString() => message;
}

class ApiClient {
  final String baseUrl;

  const ApiClient({
    required this.baseUrl,
  });

  Future<void> _checkConnectivity() async {
    final connectivityResult = await Connectivity().checkConnectivity();
    if (connectivityResult.contains(ConnectivityResult.none)) {
      throw NoInternetException();
    }
  }

  Future<http.Response> postJson(
    String path, {
    required Map<String, dynamic> body,
    Map<String, String>? headers,
  }) async {
    await _checkConnectivity();
    final uri = Uri.parse('$baseUrl$path');
    try {
      return await http.post(
        uri,
        headers: {
          'Content-Type': 'application/json',
          ...?headers,
        },
        body: jsonEncode(body),
      );
    } on SocketException {
      throw NoInternetException();
    }
  }

  Future<http.Response> get(
    String path, {
    Map<String, String>? headers,
  }) async {
    await _checkConnectivity();
    final uri = Uri.parse('$baseUrl$path');
    try {
      return await http.get(
        uri,
        headers: headers,
      );
    } on SocketException {
      throw NoInternetException();
    }
  }
}

