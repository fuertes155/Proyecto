import re
import os

files_to_process = [
    r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\home\\presentation\\widgets\\animated_virtual_card.dart',
    r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\home\\presentation\\widgets\\expense_chart.dart'
]

def fix_file(path):
    if not os.path.exists(path): return
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    content = content.replace('Theme.of(context).colorScheme.onSurface87', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.87)')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for p in files_to_process:
    fix_file(p)
