import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:dio/dio.dart';

import '../../../../core/network/api_client_provider.dart';
import '../../../../core/widgets/accessible_button.dart';
import '../providers/auth_provider.dart';

class BiometricRegistrationPage extends ConsumerStatefulWidget {
  const BiometricRegistrationPage({super.key});

  @override
  ConsumerState<BiometricRegistrationPage> createState() => _BiometricRegistrationPageState();
}

class _BiometricRegistrationPageState extends ConsumerState<BiometricRegistrationPage> {
  final ImagePicker _picker = ImagePicker();
  
  File? _idImage;
  File? _selfieImage;
  bool _isLoading = false;

  Future<void> _takeIdPhoto() async {
    final XFile? image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 50);
    if (image != null) {
      setState(() => _idImage = File(image.path));
    }
  }

  Future<void> _takeSelfie() async {
    final XFile? image = await _picker.pickImage(source: ImageSource.camera, preferredCameraDevice: CameraDevice.front, imageQuality: 50);
    if (image != null) {
      setState(() => _selfieImage = File(image.path));
    }
  }

  Future<void> _submitBiometrics() async {
    if (_idImage == null || _selfieImage == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Por favor, toma ambas fotos requeridas.')),
      );
      return;
    }

    setState(() => _isLoading = true);
    
    try {
      final dio = ref.read(apiClientProvider);
      final authState = ref.read(authStateProvider);
      final userId = authState.value?.id;
      
      if (userId == null) {
        throw Exception('Usuario no autenticado.');
      }

      final idBytes = await _idImage!.readAsBytes();
      final selfieBytes = await _selfieImage!.readAsBytes();
      
      final idBase64 = base64Encode(idBytes);
      final selfieBase64 = base64Encode(selfieBytes);

      await dio.post('/v1/auth/biometric', data: {
        'userId': userId,
        'documentImageBase64': idBase64,
        'selfieImageBase64': selfieBase64,
      });

      // After successful upload, refresh the profile to get updated KYC status
      await ref.read(authStateProvider.notifier).checkSession();
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Identidad verificada exitosamente. ¡Bienvenido a Met!')),
        );
        context.go('/home');
      }
    } on DioException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: ${e.response?.data?['message'] ?? e.message}')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error inesperado: $e')),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Widget _buildPhotoSlot(String title, String subtitle, IconData icon, File? imageFile, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        height: 180,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest.withOpacity(0.3),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(
            color: imageFile != null 
              ? Theme.of(context).colorScheme.primary 
              : Theme.of(context).colorScheme.outline.withOpacity(0.5),
            width: 2,
          ),
          image: imageFile != null
              ? DecorationImage(
                  image: FileImage(imageFile),
                  fit: BoxFit.cover,
                  colorFilter: ColorFilter.mode(Colors.black.withOpacity(0.3), BlendMode.darken),
                )
              : null,
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              imageFile != null ? Icons.check_circle : icon,
              size: 48,
              color: imageFile != null 
                ? Theme.of(context).colorScheme.primary 
                : Theme.of(context).colorScheme.onSurfaceVariant,
            ),
            const SizedBox(height: 12),
            Text(
              imageFile != null ? 'Completado' : title,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
                color: imageFile != null ? Colors.white : null,
              ),
            ),
            if (imageFile == null) ...[
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ] else ...[
              const SizedBox(height: 4),
              const Text(
                'Toca para volver a tomar',
                style: TextStyle(color: Colors.white70, fontSize: 12),
              ),
            ]
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Verificación de Identidad'),
        centerTitle: true,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Ya casi terminamos',
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Por regulaciones financieras, necesitamos verificar tu identidad antes de abrir tu billetera. Tus datos están seguros.',
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 32),
              
              _buildPhotoSlot(
                'Cédula Original',
                'Toma una foto de la parte frontal',
                Icons.badge_outlined,
                _idImage,
                _takeIdPhoto,
              ),
              
              const SizedBox(height: 24),
              
              _buildPhotoSlot(
                'Selfie',
                'Tómate una foto clara de tu rostro',
                Icons.face,
                _selfieImage,
                _takeSelfie,
              ),
              
              const SizedBox(height: 48),
              
              AccessibleButton(
                label: 'Completar Verificación',
                isLoading: _isLoading,
                onPressed: (_idImage != null && _selfieImage != null) ? _submitBiometrics : null,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
