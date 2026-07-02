import re
import os

files_to_process = [
    r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\home\\presentation\\widgets\\expense_chart.dart',
    r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\home\\presentation\\widgets\\animated_virtual_card.dart',
    r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\auth\\presentation\\pages\\home_widgets\\_more_actions_sheet.dart',
    r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\auth\\presentation\\pages\\home_widgets\\_sheet_action_tile.dart',
    r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\auth\\presentation\\pages\\home_widgets\\quick_actions.dart'
]

def process_file(path):
    if not os.path.exists(path):
        return
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Generic replaces
    content = content.replace('Color(0xFF1E1E1E)', 'Theme.of(context).colorScheme.surfaceContainer')
    content = content.replace('Colors.white', 'Theme.of(context).colorScheme.onSurface')
    content = content.replace('Colors.black', 'Theme.of(context).colorScheme.onSurface')
    content = content.replace('Colors.white54', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.54)')
    content = content.replace('Colors.white70', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.7)')
    
    # Remove consts from Icon, Text, TextStyle that now use Theme.of
    content = re.sub(r'const\s+Icon\((.*?Theme\.of.*?)\)', r'Icon(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+TextStyle\((.*?Theme\.of.*?)\)', r'TextStyle(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Text\((.*?Theme\.of.*?)\)', r'Text(\1)', content, flags=re.DOTALL)

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for path in files_to_process:
    process_file(path)
