import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

// Definición básica de los modelos para el Panel Administrativo
class UserModel {
  final String id;
  final String firstName;
  final String lastName;
  final String email;
  final String kycStatus;
  
  UserModel({required this.id, required this.firstName, required this.lastName, required this.email, required this.kycStatus});
  
  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] ?? '',
      firstName: json['firstName'] ?? 'Sin nombre',
      lastName: json['lastName'] ?? '',
      email: json['email'] ?? 'Sin correo',
      kycStatus: json['kycStatus'] ?? 'PENDING',
    );
  }
}

class AccountModel {
  final String id;
  final String accountNumber;
  final String userId;
  final String status;
  
  AccountModel({required this.id, required this.accountNumber, required this.userId, required this.status});
  
  factory AccountModel.fromJson(Map<String, dynamic> json) {
    return AccountModel(
      id: json['id'] ?? '',
      accountNumber: json['accountNumber'] ?? '',
      userId: json['userId'] ?? '',
      status: json['status'] ?? 'ACTIVE',
    );
  }
}

class LoanModel {
  final String id;
  final String userId;
  final String status;
  
  LoanModel({required this.id, required this.userId, required this.status});
  
  factory LoanModel.fromJson(Map<String, dynamic> json) {
    return LoanModel(
      id: json['id'] ?? '',
      userId: json['userId'] ?? '',
      status: json['status'] ?? 'PENDING',
    );
  }
}

// Cliente de API configurado para el Admin
final adminApiClientProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: 'http://localhost:8080/api/v1/admin',
    connectTimeout: const Duration(seconds: 5),
  ));
  
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      const storage = FlutterSecureStorage();
      final token = await storage.read(key: 'admin_access_token');
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      return handler.next(options);
    }
  ));
  
  return dio;
});

// Providers de datos
final usersProvider = FutureProvider<List<UserModel>>((ref) async {
  try {
    final dio = ref.read(adminApiClientProvider);
    final response = await dio.get('/users');
    final List data = response.data;
    return data.map((json) => UserModel.fromJson(json)).toList();
  } catch (e) {
    print('Error fetching users: $e');
    return [];
  }
});

final accountsProvider = FutureProvider<List<AccountModel>>((ref) async {
  try {
    final dio = ref.read(adminApiClientProvider);
    final response = await dio.get('/accounts');
    final List data = response.data;
    return data.map((json) => AccountModel.fromJson(json)).toList();
  } catch (e) {
    print('Error fetching accounts: $e');
    return [];
  }
});

final loansProvider = FutureProvider<List<LoanModel>>((ref) async {
  try {
    final dio = ref.read(adminApiClientProvider);
    final response = await dio.get('/loans');
    final List data = response.data;
    return data.map((json) => LoanModel.fromJson(json)).toList();
  } catch (e) {
    print('Error fetching loans: $e');
    return [];
  }
});
