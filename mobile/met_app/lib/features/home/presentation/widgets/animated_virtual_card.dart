import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../providers/home_provider.dart';

class AnimatedVirtualCard extends ConsumerStatefulWidget {
  const AnimatedVirtualCard({super.key, required this.obscureBalance, required this.onToggleObscure});

  final bool obscureBalance;
  final VoidCallback onToggleObscure;

  @override
  ConsumerState<AnimatedVirtualCard> createState() => _AnimatedVirtualCardState();
}

class _AnimatedVirtualCardState extends ConsumerState<AnimatedVirtualCard> with SingleTickerProviderStateMixin {
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

    return GestureDetector(
      onPanUpdate: _onPanUpdate,
      onPanEnd: _onPanEnd,
      child: Transform(
        transform: Matrix4.identity()
          ..setEntry(3, 2, 0.001) 
          ..rotateY(rotationY),
        alignment: FractionalOffset.center,
        child: showFront ? _buildFront(context) : Transform(
          alignment: FractionalOffset.center,
          transform: Matrix4.identity()..rotateY(pi),
          child: _buildBack(context),
        ),
      ),
    );
  }

  Widget _buildFront(BuildContext context) {
    return Container(
      width: double.infinity,
      height: 220,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          colors: [Color(0xFF53A835), Color(0xFF2E631B)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF53A835).withOpacity(0.4),
            blurRadius: 20,
            offset: const Offset(0, 10),
          )
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Saldo Total',
                style: TextStyle(
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.9),
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
              ),
              GestureDetector(
                onTap: widget.onToggleObscure,
                child: Icon(
                  widget.obscureBalance ? Icons.visibility_off : Icons.visibility,
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.9),
                  size: 20,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Consumer(
            builder: (context, ref, child) {
              final homeState = ref.watch(homeProvider);
              return homeState.when(
                data: (summary) {
                  final formattedPrincipal = NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 2).format(summary.principalBalance);
                  final formattedInterest = NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 2).format(summary.interestBalance);
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Capital (Bloqueado): ${widget.obscureBalance ? '••••••••' : formattedPrincipal}',
                        style: const TextStyle(color: Colors.white70, fontSize: 16),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Intereses (Disponible): ${widget.obscureBalance ? '••••••••' : formattedInterest}',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                          letterSpacing: -0.5,
                        ),
                      ),
                    ],
                  );
                },
                loading: () => const CircularProgressIndicator(color: Colors.white),
                error: (err, stack) => GestureDetector(
                  onTap: () => ref.read(homeProvider.notifier).refresh(),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.refresh, color: Colors.white, size: 18),
                      SizedBox(width: 6),
                      Text('Toca para reintentar', style: TextStyle(color: Colors.white)),
                    ],
                  ),
                ),
              );
            },
          ),
          const Spacer(),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Cuenta Met •••• 4092',
                style: TextStyle(
                  color: Theme.of(context).colorScheme.onSurface.withOpacity(0.8),
                  fontSize: 14,
                ),
              ),
              Icon(
                Icons.account_balance_wallet,
                color: Theme.of(context).colorScheme.onSurface,
                size: 24,
              ),
            ],
          )
        ],
      ),
    );
  }

  Widget _buildBack(BuildContext context) {
    return Container(
      width: double.infinity,
      height: 220,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          colors: [Color(0xFF2E631B), Color(0xFF53A835)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF53A835).withOpacity(0.4),
            blurRadius: 20,
            offset: const Offset(0, 10),
          )
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 30),
          Container(
            height: 45,
            width: double.infinity,
            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.87),
          ),
          const SizedBox(height: 20),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.onSurface,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    'CVV  123',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurface.withOpacity(0.87), fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
          ),
          const Spacer(),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
            child: Text(
              'Ahorro MET Débito',
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurface.withOpacity(0.9),
                fontWeight: FontWeight.w600,
              ),
            ),
          )
        ],
      ),
    );
  }
}
