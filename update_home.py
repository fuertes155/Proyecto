import re

path = r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\auth\\presentation\\pages\\home_page.dart'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace hardcoded colors with Theme properties
content = content.replace('backgroundColor: const Color(0xFF121212),', 'backgroundColor: Theme.of(context).scaffoldBackgroundColor,')
content = content.replace('color: const Color(0xFF1E1E1E),', 'color: Theme.of(context).colorScheme.surfaceContainer,')

# Colors.white
content = content.replace('color: Colors.white,', 'color: Theme.of(context).colorScheme.onSurface,')
content = content.replace('color: Colors.white70', 'color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7)')
content = content.replace('color: Colors.white54', 'color: Theme.of(context).colorScheme.onSurface.withOpacity(0.54)')

# Colors.white.withOpacity
content = content.replace('Colors.white.withOpacity', 'Theme.of(context).colorScheme.onSurface.withOpacity')

# Remove const from Icon, Text, TextStyle that we just injected Theme.of(context) into
content = re.sub(r'const\s+Icon\((.*?Theme\.of.*?)\)', r'Icon(\1)', content, flags=re.DOTALL)
content = re.sub(r'const\s+TextStyle\((.*?Theme\.of.*?)\)', r'TextStyle(\1)', content, flags=re.DOTALL)
content = re.sub(r'const\s+Text\((.*?Theme\.of.*?)\)', r'Text(\1)', content, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
