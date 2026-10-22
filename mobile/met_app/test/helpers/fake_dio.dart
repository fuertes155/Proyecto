import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

/// Respuesta enlatada para una ruta: código HTTP y cuerpo (Map/List se envían como JSON, String tal cual).
class FakeResponse {
  const FakeResponse(this.body, {this.status = 200});

  final Object? body;
  final int status;
}

/// Petición que llegó al "servidor" falso, para revisar ruta, query, cabeceras y cuerpo enviados.
class RecordedRequest {
  RecordedRequest(this.method, this.path, this.queryParameters, this.body, {this.headers = const {}});

  final String method;
  final String path;
  final Map<String, dynamic> queryParameters;
  final Map<String, dynamic> headers;
  final Object? body;

  Map<String, dynamic> get json => body as Map<String, dynamic>;
}

/// Adaptador HTTP de Dio que responde desde memoria: prueba las fuentes remotas reales
/// (rutas, parámetros, parseo de respuestas y errores) sin red y sin dependencias extra.
class FakeDioAdapter implements HttpClientAdapter {
  final Map<String, FakeResponse> _routes = {};
  final List<RecordedRequest> requests = [];

  /// Registra la respuesta para "GET /v1/ruta" (la query no forma parte de la clave).
  void on(String method, String path, FakeResponse response) => _routes['$method $path'] = response;

  RecordedRequest get last => requests.last;

  /// Respuesta según la petición (p. ej. según el token enviado); si devuelve null se usa la ruta.
  FakeResponse? Function(RecordedRequest request)? onRequest;

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    Object? sent = options.data;
    if (sent is String && sent.isNotEmpty) {
      try {
        sent = jsonDecode(sent);
      } catch (_) {}
    }
    final request = RecordedRequest(options.method, options.path, options.queryParameters, sent,
        headers: Map.of(options.headers));
    requests.add(request);

    final response = onRequest?.call(request) ?? _routes['${options.method} ${options.path}'];
    if (response == null) {
      return ResponseBody.fromString('{"message":"Ruta no simulada: ${options.method} ${options.path}"}', 404,
          headers: _jsonHeaders);
    }
    final body = response.body;
    if (body is String) {
      return ResponseBody.fromString(body, response.status, headers: {
        Headers.contentTypeHeader: ['text/plain; charset=utf-8'],
      });
    }
    return ResponseBody.fromString(jsonEncode(body), response.status, headers: _jsonHeaders);
  }

  static final _jsonHeaders = {
    Headers.contentTypeHeader: ['application/json; charset=utf-8'],
  };

  @override
  void close({bool force = false}) {}
}

/// Dio apuntando a la API de pruebas con el adaptador falso ya conectado.
Dio fakeDio(FakeDioAdapter adapter, {String baseUrl = 'http://api.test/api'}) {
  return Dio(BaseOptions(baseUrl: baseUrl))..httpClientAdapter = adapter;
}
