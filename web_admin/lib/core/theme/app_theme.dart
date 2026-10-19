import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Paleta del panel. Son los mismos colores de siempre (verde MET); aquí solo
/// quedan con nombre para que todas las pantallas los usen igual.
class MetColors {
  MetColors._();

  // Marca
  static const Color primary = Color(0xFF2E7D32);
  static const Color accent = Color(0xFF53A835);
  static const Color secondary = Color(0xFF9CB794);
  static const Color brandDark = Color(0xFF173F25);
  static const Color brandMid = Color(0xFF1F5A31);

  // Superficies
  static const Color canvas = Color(0xFFF5F7FA);
  static const Color surface = Colors.white;
  static const Color subtle = Color(0xFFF8FAF8);
  static const Color border = Color(0xFFE3E8E3);
  static const Color borderStrong = Color(0xFFD3DCD3);

  // Texto
  static const Color text = Color(0xFF1B241D);
  static const Color textMuted = Color(0xFF5E6A61);
  static const Color textFaint = Color(0xFF8C968E);

  // Estados
  static const Color success = primary;
  static const Color successBg = Color(0xFFE8F3E9);
  static const Color warning = Color(0xFFB26A00);
  static const Color warningBg = Color(0xFFFFF4E0);
  static const Color danger = Color(0xFFD32F2F);
  static const Color dangerBg = Color(0xFFFDECEC);
  static const Color info = Color(0xFF0B5FFF);
  static const Color infoBg = Color(0xFFE8EFFF);
  static const Color neutral = Color(0xFF5E6A61);
  static const Color neutralBg = Color(0xFFEEF1EE);

  static const LinearGradient brandGradient = LinearGradient(
    colors: [brandDark, brandMid, primary],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );
}

/// Eslogan de MET.
const String kMetSlogan = 'Impulso instantáneo, confianza permanente';

class AppTheme {
  // Se mantienen por compatibilidad con el código existente.
  static const Color primaryColorLight = MetColors.primary;
  static const Color secondaryColor = MetColors.secondary;
  static const Color errorColor = MetColors.danger;

  static ThemeData get lightTheme {
    OutlineInputBorder inputBorder(Color color, [double width = 1.2]) => OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: color, width: width),
        );
    final buttonShape = RoundedRectangleBorder(borderRadius: BorderRadius.circular(12));
    // Con fuente explícita: el estilo de botón reemplaza al del texto y, sin ella,
    // los botones salían en la fuente por defecto del navegador en vez de Outfit.
    final buttonText = GoogleFonts.outfit(fontSize: 15, fontWeight: FontWeight.w600);

    return ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: MetColors.canvas,
      colorScheme: ColorScheme.fromSeed(
        seedColor: MetColors.primary,
        primary: MetColors.primary,
        secondary: MetColors.secondary,
        error: MetColors.danger,
        brightness: Brightness.light,
        surface: MetColors.surface,
        surfaceContainer: const Color(0xFFF0F2F5),
        surfaceContainerHigh: const Color(0xFFE2E8F0),
        onSurface: MetColors.text,
        outline: MetColors.borderStrong,
        outlineVariant: MetColors.border,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.transparent,
        foregroundColor: MetColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      textTheme: GoogleFonts.outfitTextTheme(
        const TextTheme(
          headlineLarge: TextStyle(fontSize: 32, fontWeight: FontWeight.w800, color: MetColors.text, letterSpacing: -0.5),
          headlineMedium: TextStyle(fontSize: 26, fontWeight: FontWeight.w800, color: MetColors.text, letterSpacing: -0.4),
          titleLarge: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: MetColors.text),
          titleMedium: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: MetColors.text),
          bodyLarge: TextStyle(fontSize: 16, height: 1.5, color: MetColors.text),
          bodyMedium: TextStyle(fontSize: 14, height: 1.45, color: MetColors.text),
          bodySmall: TextStyle(fontSize: 12.5, height: 1.4, color: MetColors.textMuted),
          labelLarge: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: MetColors.text),
        ),
      ),
      cardTheme: CardThemeData(
        color: MetColors.surface,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: MetColors.border),
        ),
      ),
      dividerTheme: const DividerThemeData(color: MetColors.border, thickness: 1, space: 1),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: MetColors.primary,
          foregroundColor: Colors.white,
          disabledBackgroundColor: MetColors.primary.withValues(alpha: 0.45),
          disabledForegroundColor: Colors.white,
          elevation: 0,
          minimumSize: const Size(0, 48),
          padding: const EdgeInsets.symmetric(horizontal: 20),
          textStyle: buttonText,
          shape: buttonShape,
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: MetColors.primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(0, 44),
          padding: const EdgeInsets.symmetric(horizontal: 18),
          textStyle: buttonText,
          shape: buttonShape,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: MetColors.primary,
          minimumSize: const Size(0, 44),
          padding: const EdgeInsets.symmetric(horizontal: 18),
          side: const BorderSide(color: MetColors.borderStrong),
          textStyle: buttonText,
          shape: buttonShape,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: MetColors.primary,
          minimumSize: const Size(0, 40),
          textStyle: buttonText,
          shape: buttonShape,
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(foregroundColor: MetColors.textMuted),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: MetColors.subtle,
        isDense: false,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        hintStyle: const TextStyle(color: MetColors.textFaint, fontSize: 14.5),
        labelStyle: const TextStyle(color: MetColors.textMuted, fontSize: 14.5),
        prefixIconColor: MetColors.textFaint,
        suffixIconColor: MetColors.textMuted,
        border: inputBorder(MetColors.border),
        enabledBorder: inputBorder(MetColors.border),
        disabledBorder: inputBorder(MetColors.border),
        focusedBorder: inputBorder(MetColors.primary, 2),
        errorBorder: inputBorder(MetColors.danger.withValues(alpha: 0.6)),
        focusedErrorBorder: inputBorder(MetColors.danger, 2),
        errorStyle: const TextStyle(fontSize: 12.5),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: MetColors.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        titleTextStyle: GoogleFonts.outfit(fontSize: 20, fontWeight: FontWeight.w700, color: MetColors.text),
        contentTextStyle: GoogleFonts.outfit(fontSize: 14.5, height: 1.5, color: MetColors.textMuted),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: const Color(0xFF1E2A21),
        contentTextStyle: GoogleFonts.outfit(fontSize: 14.5, color: Colors.white),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        elevation: 0,
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.all(Colors.white),
        trackColor: WidgetStateProperty.resolveWith(
          (states) => states.contains(WidgetState.selected) ? MetColors.primary : MetColors.borderStrong,
        ),
        trackOutlineColor: WidgetStateProperty.all(Colors.transparent),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: MetColors.surface,
        selectedColor: MetColors.successBg,
        side: const BorderSide(color: MetColors.border),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        labelStyle: GoogleFonts.outfit(fontSize: 13.5, fontWeight: FontWeight.w600, color: MetColors.text),
        showCheckmark: false,
      ),
      tooltipTheme: TooltipThemeData(
        decoration: BoxDecoration(color: const Color(0xFF1E2A21), borderRadius: BorderRadius.circular(8)),
        textStyle: GoogleFonts.outfit(fontSize: 12.5, color: Colors.white),
        waitDuration: const Duration(milliseconds: 400),
      ),
      tabBarTheme: TabBarThemeData(
        labelColor: MetColors.primary,
        unselectedLabelColor: MetColors.textMuted,
        indicatorColor: MetColors.primary,
        dividerColor: MetColors.border,
        labelStyle: GoogleFonts.outfit(fontSize: 14.5, fontWeight: FontWeight.w700),
        unselectedLabelStyle: GoogleFonts.outfit(fontSize: 14.5, fontWeight: FontWeight.w600),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(color: MetColors.primary),
      scrollbarTheme: ScrollbarThemeData(
        thickness: WidgetStateProperty.all(6),
        radius: const Radius.circular(8),
        thumbColor: WidgetStateProperty.all(MetColors.borderStrong),
      ),
    );
  }
}
