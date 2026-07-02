import re
import os
import glob

base_path = r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\admin\\presentation'
dart_files = glob.glob(os.path.join(base_path, '**', '*.dart'), recursive=True)

def process_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Generic replaces
    content = content.replace('Color(0xFF121212)', 'Theme.of(context).scaffoldBackgroundColor')
    content = content.replace('Color(0xFF1E1E1E)', 'Theme.of(context).colorScheme.surfaceContainer')
    content = content.replace('Colors.white', 'Theme.of(context).colorScheme.onSurface')
    content = content.replace('Colors.black', 'Theme.of(context).colorScheme.onSurface')
    content = content.replace('Colors.white54', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.54)')
    content = content.replace('Colors.white70', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.7)')
    content = content.replace('Colors.white.withOpacity', 'Theme.of(context).colorScheme.onSurface.withOpacity')
    
    # Remove consts
    content = re.sub(r'const\s+Icon\((.*?Theme\.of.*?)\)', r'Icon(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+TextStyle\((.*?Theme\.of.*?)\)', r'TextStyle(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Text\((.*?Theme\.of.*?)\)', r'Text(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Container\((.*?Theme\.of.*?)\)', r'Container(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+BoxDecoration\((.*?Theme\.of.*?)\)', r'BoxDecoration(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Row\((.*?Theme\.of.*?)\)', r'Row(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Column\((.*?Theme\.of.*?)\)', r'Column(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Expanded\((.*?Theme\.of.*?)\)', r'Expanded(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+SizedBox\((.*?Theme\.of.*?)\)', r'SizedBox(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+CircleAvatar\((.*?Theme\.of.*?)\)', r'CircleAvatar(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Card\((.*?Theme\.of.*?)\)', r'Card(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+ListTile\((.*?Theme\.of.*?)\)', r'ListTile(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+LinearProgressIndicator\((.*?Theme\.of.*?)\)', r'LinearProgressIndicator(\1)', content, flags=re.DOTALL)

    content = re.sub(r'const\s+([A-Za-z0-9_]+\s*\((?:[^)(]+|\((?:[^)(]+|\([^)(]*\))*\))*\bTheme\.of\b)', r'\1', content, flags=re.DOTALL)
    
    # Generic const remover
    lines = content.split('\n')
    for i in range(len(lines)):
        if 'Theme.of' in lines[i]:
            lines[i] = lines[i].replace('const ', '')
    for i in range(1, len(lines)):
        if 'Theme.of' in lines[i]:
            lines[i-1] = lines[i-1].replace('const ', '')
            lines[i-2] = lines[i-2].replace('const ', '')

    content = '\n'.join(lines)

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for path in dart_files:
    process_file(path)
