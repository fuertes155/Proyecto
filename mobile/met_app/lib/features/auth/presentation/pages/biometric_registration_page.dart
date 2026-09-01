import 'dart:convert';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
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
  final GlobalKey _signatureBoundaryKey = GlobalKey();

  // Bytes de cada foto (no `File`: el navegador no tiene `dart:io`). Se leen al
  // momento de capturar y se envían en base64 al backend.
  Uint8List? _idImage;
  Uint8List? _idBackImage;
  Uint8List? _selfieImage;
  final List<Offset?> _signaturePoints = [];
  bool _isLoading = false;

  static const _allowedImageExtensions = ['.jpg', '.jpeg', '.png'];

  bool get _hasSignature => _signaturePoints.any((p) => p != null);

  bool _isValidPhoto(XFile file) {
    final mimeType = file.mimeType;
    if (mimeType != null) return mimeType.startsWith('image/');
    final name = file.name.toLowerCase();
    return _allowedImageExtensions.any(name.endsWith);
  }

  void _rejectNonPhoto() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Debes tomar/subir una foto (JPG o PNG). No se permiten archivos PDF u otros documentos.')),
    );
  }

  /// Abre la cámara del sistema, que es quien pide el permiso al usuario la
  /// primera vez. Si el usuario lo niega, se lo informamos en vez de fallar
  /// en silencio.
  Future<XFile?> _pickFromCamera({CameraDevice device = CameraDevice.rear}) async {
    try {
      return await _picker.pickImage(
        source: ImageSource.camera,
        preferredCameraDevice: device,
        imageQuality: 50,
      );
    } on PlatformException catch (e) {
      if (mounted) {
        final isPermissionDenied = e.code == 'camera_access_denied' ||
            e.code == 'camera_access_denied_without_prompt' ||
            e.code == 'camera_access_restricted';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              isPermissionDenied
                  ? 'Necesitamos permiso de cámara para continuar. Actívalo en Ajustes > Apps > Met > Permisos.'
                  : 'No se pudo acceder a la cámara: ${e.message ?? e.code}',
            ),
          ),
        );
      }
      return null;
    }
  }

  Future<void> _takeIdPhoto() async {
    final XFile? image = await _pickFromCamera();
    if (image == null) return;
    if (!_isValidPhoto(image)) return _rejectNonPhoto();
    final bytes = await image.readAsBytes();
    if (!mounted) return;
    setState(() => _idImage = bytes);
  }

  Future<void> _takeIdBackPhoto() async {
    final XFile? image = await _pickFromCamera();
    if (image == null) return;
    if (!_isValidPhoto(image)) return _rejectNonPhoto();
    final bytes = await image.readAsBytes();
    if (!mounted) return;
    setState(() => _idBackImage = bytes);
  }

  Future<void> _takeSelfie() async {
    final XFile? image = await _pickFromCamera(device: CameraDevice.front);
    if (image == null) return;
    if (!_isValidPhoto(image)) return _rejectNonPhoto();
    final bytes = await image.readAsBytes();
    if (!mounted) return;
    setState(() => _selfieImage = bytes);
  }

  void _clearSignature() {
    setState(() => _signaturePoints.clear());
  }

  Future<Uint8List?> _captureSignaturePng() async {
    final boundary = _signatureBoundaryKey.currentContext?.findRenderObject() as RenderRepaintBoundary?;
    if (boundary == null) return null;
    final image = await boundary.toImage(pixelRatio: 2.0);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    return byteData?.buffer.asUint8List();
  }

  Future<void> _submitBiometrics() async {
    if (_idImage == null || _idBackImage == null || _selfieImage == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Por favor, toma las tres fotos requeridas.')),
      );
      return;
    }
    if (!_hasSignature) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Por favor, dibuja tu firma digital igual a la de tu cédula.')),
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      final signatureBytes = await _captureSignaturePng();
      if (signatureBytes == null) {
        throw Exception('No se pudo capturar la firma. Intenta de nuevo.');
      }

      final dio = ref.read(apiClientProvider);
      final authState = ref.read(authStateProvider);
      final userId = authState.valueOrNull?.id;

      if (userId == null) {
        throw Exception('Usuario no autenticado.');
      }

      final idBase64 = base64Encode(_idImage!);
      final idBackBase64 = base64Encode(_idBackImage!);
      final selfieBase64 = base64Encode(_selfieImage!);
      final signatureBase64 = base64Encode(signatureBytes);

      await dio.post('/v1/auth/biometric', data: {
        'userId': userId,
        'documentImageBase64': idBase64,
        'documentBackImageBase64': idBackBase64,
        'selfieImageBase64': selfieBase64,
        'signatureImageBase64': signatureBase64,
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

  Widget _buildPhotoSlot(String title, String subtitle, IconData icon, Uint8List? imageFile, VoidCallback onTap) {
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
                  image: MemoryImage(imageFile),
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

  Widget _buildSignaturePad() {
    final primaryColor = Theme.of(context).colorScheme.primary;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Firma Digital',
          style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 4),
        Text(
          'Debe ser igual, tal cual, a la firma que aparece en tu cédula.',
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 12),
        RepaintBoundary(
          key: _signatureBoundaryKey,
          child: Container(
            height: 180,
            width: double.infinity,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(
                color: _hasSignature ? primaryColor : Theme.of(context).colorScheme.outline.withOpacity(0.5),
                width: 2,
              ),
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(22),
              child: GestureDetector(
                onPanStart: (details) => setState(() => _signaturePoints.add(details.localPosition)),
                onPanUpdate: (details) => setState(() => _signaturePoints.add(details.localPosition)),
                onPanEnd: (_) => setState(() => _signaturePoints.add(null)),
                child: CustomPaint(
                  painter: _SignaturePainter(_signaturePoints),
                  size: Size.infinite,
                  child: !_hasSignature
                      ? Center(
                          child: Text(
                            'Dibuja tu firma aquí',
                            style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                          ),
                        )
                      : null,
                ),
              ),
            ),
          ),
        ),
        Align(
          alignment: Alignment.centerRight,
          child: TextButton.icon(
            onPressed: _hasSignature ? _clearSignature : null,
            icon: const Icon(Icons.refresh_rounded, size: 18),
            label: const Text('Limpiar firma'),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          tooltip: 'Volver a inicio de sesión',
          onPressed: () => context.go('/login'),
        ),
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
                'Cédula (reverso)',
                'Toma una foto de la parte trasera',
                Icons.badge_outlined,
                _idBackImage,
                _takeIdBackPhoto,
              ),

              const SizedBox(height: 24),

              _buildPhotoSlot(
                'Selfie',
                'Tómate una foto clara de tu rostro',
                Icons.face,
                _selfieImage,
                _takeSelfie,
              ),

              const SizedBox(height: 24),

              _buildSignaturePad(),

              const SizedBox(height: 48),

              AccessibleButton(
                label: 'Completar Verificación',
                isLoading: _isLoading,
                onPressed: (_idImage != null && _idBackImage != null && _selfieImage != null && _hasSignature)
                    ? _submitBiometrics
                    : null,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SignaturePainter extends CustomPainter {
  final List<Offset?> points;

  _SignaturePainter(this.points);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.black
      ..strokeCap = StrokeCap.round
      ..strokeWidth = 3;

    for (int i = 0; i < points.length - 1; i++) {
      final current = points[i];
      final next = points[i + 1];
      if (current != null && next != null) {
        canvas.drawLine(current, next, paint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _SignaturePainter oldDelegate) => true;
}
