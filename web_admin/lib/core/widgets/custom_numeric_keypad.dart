import 'package:flutter/material.dart';

class CustomNumericKeypad extends StatelessWidget {
  final void Function(String) onKeyPressed;
  final VoidCallback onBackspace;
  final VoidCallback? onBiometric;
  final bool showBiometric;

  const CustomNumericKeypad({
    super.key,
    required this.onKeyPressed,
    required this.onBackspace,
    this.onBiometric,
    this.showBiometric = false,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return GridView.count(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisCount: 3,
          childAspectRatio: 2,
          mainAxisSpacing: 8,
          crossAxisSpacing: 8,
          padding: EdgeInsets.zero,
          children: [
            _buildKey('1'),
            _buildKey('2'),
            _buildKey('3'),
            _buildKey('4'),
            _buildKey('5'),
            _buildKey('6'),
            _buildKey('7'),
            _buildKey('8'),
            _buildKey('9'),
            showBiometric
                ? _buildIconButton(Icons.fingerprint, onBiometric ?? () {})
                : const SizedBox.shrink(),
            _buildKey('0'),
            _buildIconButton(Icons.backspace_outlined, onBackspace),
          ],
        );
      },
    );
  }

  Widget _buildKey(String value) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => onKeyPressed(value),
        borderRadius: BorderRadius.circular(12),
        splashColor: Colors.white.withOpacity(0.3),
        highlightColor: Colors.white.withOpacity(0.1),
        child: Container(
          alignment: Alignment.center,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.white.withOpacity(0.2)),
            borderRadius: BorderRadius.circular(12),
            color: Colors.white.withOpacity(0.1),
          ),
          child: Text(
            value,
            style: const TextStyle(
              fontSize: 28,
              fontWeight: FontWeight.w500,
              color: Colors.white,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildIconButton(IconData icon, VoidCallback onTap) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        splashColor: Colors.white.withOpacity(0.3),
        highlightColor: Colors.white.withOpacity(0.1),
        child: Container(
          alignment: Alignment.center,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.white.withOpacity(0.1)),
            borderRadius: BorderRadius.circular(12),
            color: Colors.transparent,
          ),
          child: Icon(
            icon,
            color: Colors.white,
            size: 28,
          ),
        ),
      ),
    );
  }
}
