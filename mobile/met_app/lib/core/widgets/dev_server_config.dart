import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../config/app_config.dart';

/// Botón flotante SOLO-DEBUG para cambiar la URL del backend sin recompilar.
///
/// Pensado para dispositivos físicos: con Tailscale la IP `100.x.y.z` de la PC
/// no cambia aunque cambies de red, así que se configura una vez y listo.
/// En builds release no renderiza nada.
class DevServerFab extends StatelessWidget {
  const DevServerFab({super.key});

  @override
  Widget build(BuildContext context) {
    if (!kDebugMode) return const SizedBox.shrink();

    return SafeArea(
      child: Align(
        alignment: Alignment.topRight,
        child: Padding(
          padding: const EdgeInsets.only(top: 4, right: 4),
          child: IconButton(
            tooltip: 'Servidor (dev)',
            icon: const Icon(Icons.dns_outlined, color: Colors.white70, size: 22),
            onPressed: () => showDialog<void>(
              context: context,
              builder: (_) => const _DevServerDialog(),
            ),
          ),
        ),
      ),
    );
  }
}

class _DevServerDialog extends StatefulWidget {
  const _DevServerDialog();

  @override
  State<_DevServerDialog> createState() => _DevServerDialogState();
}

class _DevServerDialogState extends State<_DevServerDialog> {
  late final TextEditingController _controller;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    // Muestra el override activo si existe; si no, la URL efectiva actual como guía.
    _controller = TextEditingController(
      text: AppConfig.devApiBaseUrlOverride ?? AppConfig.apiBaseUrl,
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _save(String? value) async {
    setState(() => _saving = true);
    await AppConfig.setDevApiBaseUrl(value);
    if (!mounted) return;
    setState(() => _saving = false);
    Navigator.of(context).pop();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('URL guardada. Cierra y vuelve a abrir la app para aplicar.'),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Servidor (solo dev)'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'URL base del backend. Ej. con Tailscale:\n'
            'http://100.101.102.103:8080/api',
            style: TextStyle(fontSize: 12),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _controller,
            autocorrect: false,
            enableSuggestions: false,
            keyboardType: TextInputType.url,
            inputFormatters: [FilteringTextInputFormatter.deny(RegExp(r'\s'))],
            decoration: const InputDecoration(
              hintText: 'http://IP:8080/api',
              isDense: true,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Por defecto: ${AppConfig.apiBaseUrl}',
            style: const TextStyle(fontSize: 11, color: Colors.grey),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => _save(null),
          child: const Text('Restablecer'),
        ),
        TextButton(
          onPressed: _saving ? null : () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: _saving ? null : () => _save(_controller.text),
          child: _saving
              ? const SizedBox(
                  width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Guardar'),
        ),
      ],
    );
  }
}
