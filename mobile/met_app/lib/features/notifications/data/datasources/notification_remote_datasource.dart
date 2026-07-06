import 'package:dio/dio.dart';
import '../models/notification_model.dart';

class NotificationRemoteDataSource {
  NotificationRemoteDataSource(this._dio);

  final Dio _dio;

  Future<List<NotificationModel>> getNotifications() async {
    final response = await _dio.get('/v1/notifications');
    final List<dynamic> data = response.data;
    return data.map((json) => NotificationModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<int> getUnreadCount() async {
    final response = await _dio.get('/v1/notifications/unread-count');
    return response.data['count'] as int;
  }

  Future<void> markAsRead(String id) async {
    await _dio.put('/v1/notifications/$id/read');
  }
}
