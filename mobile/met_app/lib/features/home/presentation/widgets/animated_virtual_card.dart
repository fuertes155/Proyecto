import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../domain/entities/home_summary.dart';
import '../../../../core/utils/currency_formatter.dart';
import '../providers/home_provider.dart';

/// Tarjeta de saldo de la pantalla de Inicio. Al deslizarla horizontalmente se voltea y muestra
/// el número de cuenta completo para copiarlo.
class AnimatedVirtualCard extends ConsumerStatefulWidget {
  const AnimatedVirtualCard({super.key, required this.obscureBalance, required this.onToggleObscure});

  final bool obscureBalance;
  final VoidCallback onToggleObscure;

  @override
  ConsumerState<AnimatedVirtualCard> createState() => _AnimatedVirtualCardState();
}

class _AnimatedVirtualCardState extends ConsumerState<AnimatedVirtualCard> with SingleTickerProviderStateMixin {
  static const _cardHeight = 212.0;
  static const _gradient = LinearGradient(
    colors: [Color(0xFF3B8F2A), Color(0xFF1D5716)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  double _dragOffset = 0;
  bool _isReversed = false;
  late AnimationController _springController;
  late Animation<double> _springAnimation;

  @override
  void initState() {
    super.initState();
    _springController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    _springAnimation = Tween<double>(begin: 0, end: 0).animate(
      CurvedAnimation(parent: _springController, curve: Curves.elasticOut),
    );

    _springController.addListener(() {
      setState(() {
        _dragOffset = _springAnimation.value;
      });
    });
  }

  @override
  void dispose() {
    _springController.dispose();
    super.dispose();
  }

  void _onPanUpdate(DragUpdateDetails details) {
    setState(() {
      _dragOffset += details.delta.dx;
    });
  }

  void _onPanEnd(DragEndDetails details) {
    final threshold = MediaQuery.of(context).size.width / 3;
    double targetOffset = 0;

    if (_dragOffset.abs() > threshold || details.velocity.pixelsPerSecond.dx.abs() > 500) {
      if (_dragOffset > 0 || details.velocity.pixelsPerSecond.dx > 500) {
        _isReversed = !_isReversed;
        targetOffset = MediaQuery.of(context).size.width;
      } else if (_dragOffset < 0 || details.velocity.pixelsPerSecond.dx < -500) {
        _isReversed = !_isReversed;
        targetOffset = -MediaQuery.of(context).size.width;
      }
    }

    _springAnimation = Tween<double>(begin: _dragOffset, end: targetOffset).animate(
      CurvedAnimation(parent: _springController, curve: Curves.easeOutBack),
    );

    _springController.forward(from: 0).then((_) {
      if (targetOffset != 0) {
        setState(() {
          _dragOffset = 0;
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    double rotationY = (_dragOffset / MediaQuery.of(context).size.width) * pi;
    if (_isReversed) {
      rotationY += pi;
    }

    bool showFront = (rotationY % (2 * pi)).abs() < pi / 2 || (rotationY % (2 * pi)).abs() > 3 * pi / 2;
    final homeState = ref.watch(homeProvider);

    return GestureDetector(
      onPanUpdate: _onPanUpdate,
      onPanEnd: _onPanEnd,
      child: Transform(
        transform: Matrix4.identity()
          ..setEntry(3, 2, 0.001)
          ..rotateY(rotationY),
        alignment: FractionalOffset.center,
        child: showFront
            ? _CardShell(child: _buildFront(homeState))
            : Transform(
                alignment: FractionalOffset.center,
                transform: Matrix4.identity()..rotateY(pi),
                child: _CardShell(child: _buildBack(homeState.valueOrNull)),
              ),
      ),
    );
  }

  Widget _buildFront(AsyncValue<HomeSummary> homeState) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                'Saldo total',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.82), fontSize: 14, fontWeight: FontWeight.w500),
              ),
            ),
            Semantics(
              // Nodo propio: si no, el lector de pantalla lo funde con todo el texto de la tarjeta.
              container: true,
              button: true,
              label: widget.obscureBalance ? 'Mostrar saldo' : 'Ocultar saldo',
              child: InkResponse(
                onTap: widget.onToggleObscure,
                radius: 22,
                child: Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.14),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    widget.obscureBalance ? Icons.visibility_off_outlined : Icons.visibility_outlined,
                    color: Colors.white,
                    size: 18,
                  ),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        homeState.when(
          data: (summary) => _buildBalances(summary),
          loading: () => const _LoadingBalances(),
          error: (err, stack) => Padding(
            padding: const EdgeInsets.symmetric(vertical: 28),
            child: TextButton.icon(
              onPressed: () => ref.read(homeProvider.notifier).refresh(),
              style: TextButton.styleFrom(
                foregroundColor: Colors.white,
                textStyle: Theme.of(context).textTheme.labelLarge?.copyWith(fontSize: 14),
              ),
              icon: const Icon(Icons.refresh_rounded, size: 18),
              label: const Text('No pudimos cargar tu saldo. Reintentar'),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildBalances(HomeSummary summary) {
    final total = summary.principalBalance + summary.interestBalance;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Un monto nunca se corta con "…": si no cabe, se reduce.
        FittedBox(
          fit: BoxFit.scaleDown,
          alignment: Alignment.centerLeft,
          child: widget.obscureBalance
              ? const Text('\$ ••••••',
                  style: TextStyle(color: Colors.white, fontSize: 34, fontWeight: FontWeight.w700, letterSpacing: 1))
              : _MoneyText(amount: total),
        ),
        const SizedBox(height: 18),
        IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(child: _Stat(label: 'Capital invertido', amount: summary.principalBalance, obscure: widget.obscureBalance)),
              Container(width: 1, color: Colors.white.withValues(alpha: 0.22), margin: const EdgeInsets.symmetric(horizontal: 14)),
              Expanded(child: _Stat(label: 'Disponible para retirar', amount: summary.interestBalance, obscure: widget.obscureBalance)),
            ],
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: Text(
                _maskedAccount(summary.accountNumber),
                style: TextStyle(color: Colors.white.withValues(alpha: 0.78), fontSize: 13, letterSpacing: 0.3),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: 12),
            const _BrandBadge(),
          ],
        ),
      ],
    );
  }

  Widget _buildBack(HomeSummary? summary) {
    final account = summary?.accountNumber ?? '';
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                'Número de cuenta',
                style: TextStyle(color: Colors.white.withValues(alpha: 0.82), fontSize: 14, fontWeight: FontWeight.w500),
              ),
            ),
            const _BrandBadge(),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerLeft,
                child: Text(
                  account.isEmpty ? '—' : _groupDigits(account),
                  style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w600, letterSpacing: 2),
                ),
              ),
            ),
            if (account.isNotEmpty)
              IconButton(
                tooltip: 'Copiar número de cuenta',
                icon: const Icon(Icons.copy_rounded, color: Colors.white, size: 20),
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: account));
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Número de cuenta copiado')),
                  );
                },
              ),
          ],
        ),
        const SizedBox(height: 6),
        Text(
          'Cuenta de ahorros · ${_statusLabel(summary?.accountStatus)}',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.78), fontSize: 13),
        ),
        const SizedBox(height: 26),
        Text(
          'Desliza la tarjeta para volver al saldo',
          style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
        ),
      ],
    );
  }

  static String _maskedAccount(String accountNumber) {
    if (accountNumber.length < 4) return 'Cuenta de ahorros';
    return 'Cuenta de ahorros  ••••  ${accountNumber.substring(accountNumber.length - 4)}';
  }

  static String _groupDigits(String value) {
    final buffer = StringBuffer();
    for (var i = 0; i < value.length; i++) {
      if (i > 0 && i % 4 == 0) buffer.write(' ');
      buffer.write(value[i]);
    }
    return buffer.toString();
  }

  static String _statusLabel(String? status) {
    switch (status) {
      case 'ACTIVE':
        return 'Activa';
      case 'BLOCKED':
      case 'SUSPENDED':
        return 'Bloqueada';
      case null:
        return '—';
      default:
        return status;
    }
  }
}

class _CardShell extends StatelessWidget {
  const _CardShell({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      // Altura mínima, no fija: con texto grande (accesibilidad) la tarjeta crece en vez de desbordarse.
      constraints: const BoxConstraints(minHeight: _AnimatedVirtualCardState._cardHeight),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(28),
        gradient: _AnimatedVirtualCardState._gradient,
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF1D5716).withValues(alpha: 0.28),
            blurRadius: 24,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: Stack(
          children: [
            // Formas decorativas suaves para dar profundidad sin distraer del saldo.
            const Positioned(
              top: -70,
              right: -50,
              child: _Circle(size: 190, opacity: 0.08),
            ),
            const Positioned(
              bottom: -90,
              right: 60,
              child: _Circle(size: 170, opacity: 0.05),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(22, 18, 22, 18),
              child: child,
            ),
          ],
        ),
      ),
    );
  }
}

class _Circle extends StatelessWidget {
  const _Circle({required this.size, required this.opacity});

  final double size;
  final double opacity;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.white.withValues(alpha: opacity)),
    );
  }
}

class _BrandBadge extends StatelessWidget {
  const _BrandBadge();

  @override
  Widget build(BuildContext context) {
    // Marca tipográfica: sobre el degradado se ve más sobria que el logo ilustrado.
    return Text(
      'MET',
      semanticsLabel: 'Met',
      style: TextStyle(
        color: Colors.white.withValues(alpha: 0.92),
        fontSize: 16,
        fontWeight: FontWeight.w800,
        letterSpacing: 3,
      ),
    );
  }
}

/// Monto en pesos con los centavos más pequeños, al estilo de los extractos bancarios.
class _MoneyText extends StatelessWidget {
  const _MoneyText({required this.amount});

  final double amount;

  @override
  Widget build(BuildContext context) {
    final cents = (amount * 100).round();
    final whole = NumberFormat('#,##0', 'es_CO').format(cents ~/ 100);
    final decimals = (cents % 100).toString().padLeft(2, '0');
    return Text.rich(
      TextSpan(
        children: [
          TextSpan(
            text: '\$ $whole',
            style: const TextStyle(fontSize: 34, fontWeight: FontWeight.w700, letterSpacing: -0.5),
          ),
          TextSpan(
            text: ',$decimals',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: Colors.white.withValues(alpha: 0.75)),
          ),
        ],
      ),
      style: const TextStyle(color: Colors.white),
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.amount, required this.obscure});

  final String label;
  final double amount;
  final bool obscure;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        const SizedBox(height: 2),
        FittedBox(
          fit: BoxFit.scaleDown,
          alignment: Alignment.centerLeft,
          child: Text(
            obscure ? '\$ ••••' : formatPesos(amount),
            style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.w600),
          ),
        ),
      ],
    );
  }
}

class _LoadingBalances extends StatelessWidget {
  const _LoadingBalances();

  @override
  Widget build(BuildContext context) {
    Widget bar(double width, double height) => Container(
          width: width,
          height: height,
          decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.18), borderRadius: BorderRadius.circular(8)),
        );
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 6),
        bar(190, 32),
        const SizedBox(height: 22),
        Row(children: [bar(110, 30), const SizedBox(width: 28), bar(110, 30)]),
        const SizedBox(height: 18),
        bar(160, 14),
      ],
    );
  }
}
