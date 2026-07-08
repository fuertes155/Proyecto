import 'dart:io';

void main() {
  final dir = Directory('lib/features/investment/presentation/pages');
  final files = dir.listSync().whereType<File>();
  
  for (final file in files) {
    if (!file.path.endsWith('.dart')) continue;
    
    String content = file.readAsStringSync();
    
    // Add AppTheme import if not exists
    if (!content.contains('app_theme.dart')) {
      content = content.replaceFirst(
        "import '../../data/models/investment_models.dart';",
        "import '../../../../core/theme/app_theme.dart';\nimport '../../data/models/investment_models.dart';"
      );
    }
    
    // Replace primary colors with AppTheme.primaryColor
    content = content.replaceAll('const Color(0xFF53A835)', 'AppTheme.primaryColor');
    content = content.replaceAll('Color(0xFF53A835)', 'AppTheme.primaryColor');
    
    // Replace background colors with generic app colors
    content = content.replaceAll('backgroundColor: const Color(0xFF0F1623),', '');
    content = content.replaceAll('backgroundColor: Color(0xFF0F1623),', '');
    content = content.replaceAll('backgroundColor: const Color(0xFF1A2332),', '');
    content = content.replaceAll('backgroundColor: Color(0xFF1A2332),', '');
    
    content = content.replaceAll('const Color(0xFF0F1623)', 'const Color(0xFF121212)');
    content = content.replaceAll('Color(0xFF0F1623)', 'const Color(0xFF121212)');
    
    content = content.replaceAll('const Color(0xFF1A2332)', 'const Color(0xFF1E1E1E)');
    content = content.replaceAll('Color(0xFF1A2332)', 'const Color(0xFF1E1E1E)');
    
    content = content.replaceAll('const Color(0xFF1C3320)', 'const Color(0xFF1E291E)');
    content = content.replaceAll('Color(0xFF1C3320)', 'const Color(0xFF1E291E)');
    
    content = content.replaceAll('const Color(0xFF1C2B1C)', 'const Color(0xFF1E291E)');
    content = content.replaceAll('Color(0xFF1C2B1C)', 'const Color(0xFF1E291E)');

    content = content.replaceAll('const Color(0xFF141D2B)', 'const Color(0xFF121212)');
    content = content.replaceAll('Color(0xFF141D2B)', 'const Color(0xFF121212)');

    file.writeAsStringSync(content);
    print('Updated \${file.path}');
  }
}
