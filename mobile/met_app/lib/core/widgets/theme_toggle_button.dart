import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';

/// A beautiful animated toggle switch between light and dark mode.
class ThemeToggleButton extends ConsumerStatefulWidget {
  const ThemeToggleButton({super.key});

  @override
  ConsumerState<ThemeToggleButton> createState() => _ThemeToggleButtonState();
}

class _ThemeToggleButtonState extends ConsumerState<ThemeToggleButton>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _slideAnimation;
  late Animation<double> _fadeInAnimation;
  late Animation<double> _fadeOutAnimation;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 400),
      vsync: this,
    );

    _slideAnimation = CurvedAnimation(
      parent: _controller,
      curve: Curves.easeInOutCubic,
    );

    _fadeInAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.5, 1.0),
      ),
    );

    _fadeOutAnimation = Tween<double>(begin: 1.0, end: 0.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.0, 0.5),
      ),
    );

    _scaleAnimation = Tween<double>(begin: 0.8, end: 1.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: Curves.easeOutBack,
      ),
    );

    // Set initial state based on current theme
    final isDark = ref.read(themeModeProvider) == ThemeMode.dark;
    if (isDark) {
      _controller.value = 1.0;
    } else {
      _controller.value = 0.0;
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _toggle() {
    HapticFeedback.lightImpact();
    final current = ref.read(themeModeProvider);
    if (current == ThemeMode.dark) {
      _controller.reverse();
      ref.read(themeModeProvider.notifier).state = ThemeMode.light;
    } else {
      _controller.forward();
      ref.read(themeModeProvider.notifier).state = ThemeMode.dark;
    }
  }

  @override
  Widget build(BuildContext context) {
    final themeMode = ref.watch(themeModeProvider);
    final isDark = themeMode == ThemeMode.dark;

    // Colors
    final trackColorLight = const Color(0xFFE8E8E8);
    final trackColorDark = const Color(0xFF2C3A5B);
    final knobColorLight = const Color(0xFFF5A623);
    final knobColorDark = const Color(0xFF5B73C0);

    const double trackWidth = 64.0;
    const double trackHeight = 32.0;
    const double knobSize = 26.0;
    const double padding = 3.0;

    return GestureDetector(
      onTap: _toggle,
      child: AnimatedBuilder(
        animation: _controller,
        builder: (context, child) {
          final trackColor = Color.lerp(trackColorLight, trackColorDark, _slideAnimation.value)!;
          final knobColor = Color.lerp(knobColorLight, knobColorDark, _slideAnimation.value)!;
          final knobLeft = padding + _slideAnimation.value * (trackWidth - knobSize - padding * 2);

          return Container(
            width: trackWidth,
            height: trackHeight,
            decoration: BoxDecoration(
              color: trackColor,
              borderRadius: BorderRadius.circular(trackHeight / 2),
              boxShadow: [
                BoxShadow(
                  color: isDark
                      ? const Color(0xFF5B73C0).withOpacity(0.3)
                      : const Color(0xFFF5A623).withOpacity(0.3),
                  blurRadius: 8,
                  spreadRadius: 1,
                )
              ],
            ),
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                // Sun icon (visible in light mode)
                Positioned(
                  right: 7,
                  top: 0,
                  bottom: 0,
                  child: Opacity(
                    opacity: 1 - _slideAnimation.value,
                    child: const Icon(
                      Icons.wb_sunny_outlined,
                      size: 14,
                      color: Color(0xFFBFBFBF),
                    ),
                  ),
                ),
                // Moon icon (visible in dark mode)
                Positioned(
                  left: 7,
                  top: 0,
                  bottom: 0,
                  child: Opacity(
                    opacity: _slideAnimation.value,
                    child: const Icon(
                      Icons.nightlight_round,
                      size: 14,
                      color: Color(0xFF8FA3D3),
                    ),
                  ),
                ),
                // Animated Knob
                Positioned(
                  left: knobLeft,
                  top: padding,
                  child: Transform.scale(
                    scale: _scaleAnimation.value.clamp(0.9, 1.0),
                    child: Container(
                      width: knobSize,
                      height: knobSize,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: knobColor,
                        boxShadow: [
                          BoxShadow(
                            color: knobColor.withOpacity(0.6),
                            blurRadius: 8,
                            offset: const Offset(0, 2),
                          )
                        ],
                      ),
                      child: Center(
                        child: isDark
                            ? Icon(
                                Icons.nightlight_round,
                                size: 14,
                                color: Colors.white.withOpacity(0.9),
                              )
                            : Icon(
                                Icons.wb_sunny_rounded,
                                size: 14,
                                color: Colors.white.withOpacity(0.9),
                              ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
