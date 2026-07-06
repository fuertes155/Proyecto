import '../../domain/repositories/notification_repository.dart';
import '../datasources/notification_remote_datasource.dart';
import '../models/notification_model.dart';

class NotificationRepositoryImpl implements NotificationRepository {
  NotificationRepositoryImpl(this._remote);

  final NotificationRemoteDataSource _remote;

  @override
  Future<List<NotificationModel>> getNotifications() {
    return _remote.getNotifications();
  }

  @override
  Future<int> getUnreadCount() {
    return _remote.getUnreadCount();
  }

  @override
  Future<void> markAsRead(String id) {
    return _remote.markAsRead(id);
  }
}
