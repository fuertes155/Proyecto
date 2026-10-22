import 'package:flutter/material.dart';

/// Paso de un formulario cuyo botón va al fondo con un `Spacer`.
///
/// Con espacio, se ve igual que antes (el botón abajo); con el teclado abierto en un celular
/// pequeño, el paso se desplaza en vez de desbordarse y tapar el botón.
class ScrollableStep extends StatelessWidget {
  const ScrollableStep({super.key, required this.child});

  /// Una `Column` que puede llevar `Spacer`, pero no listas ni otros desplazables.
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) => SingleChildScrollView(
        child: ConstrainedBox(
          constraints: BoxConstraints(minHeight: constraints.maxHeight),
          child: IntrinsicHeight(child: child),
        ),
      ),
    );
  }
}
