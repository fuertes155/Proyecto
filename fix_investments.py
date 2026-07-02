import re
import os
import glob

base_path = r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\investment\\presentation'
dart_files = glob.glob(os.path.join(base_path, '**', '*.dart'), recursive=True)

def process_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix bad replacements
    content = content.replace('Theme.of(context).colorScheme.onSurface12', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.12)')
    content = content.replace('Theme.of(context).colorScheme.onSurface24', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.24)')
    content = content.replace('Theme.of(context).colorScheme.onSurface38', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.38)')
    content = content.replace('Theme.of(context).colorScheme.onSurface54', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.54)')
    content = content.replace('Theme.of(context).colorScheme.onSurface70', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.7)')
    content = content.replace('Theme.of(context).colorScheme.onSurface87', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.87)')
    
    # Also remove some const wrappers that caused issues like: const BoxBorder(Theme.of...)
    content = re.sub(r'const\s+BorderSide\((.*?Theme\.of.*?)\)', r'BorderSide(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Divider\((.*?Theme\.of.*?)\)', r'Divider(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Padding\((.*?Theme\.of.*?)\)', r'Padding(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Theme\.of', r'Theme.of', content, flags=re.DOTALL)
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for path in dart_files:
    process_file(path)
