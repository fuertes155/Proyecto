import os
import re

# Fix admin_login_page.dart
path_admin = r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\admin\\presentation\\pages\\admin_login_page.dart'
with open(path_admin, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Theme.of(context).colorScheme.onSurface54', 'Theme.of(context).colorScheme.onSurface.withOpacity(0.54)')

with open(path_admin, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix login_page.dart
path_login = r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\auth\\presentation\\pages\\login_page.dart'
with open(path_login, 'r', encoding='utf-8') as f:
    content = f.read()

lines = content.split('\n')
for i in range(len(lines)):
    if 'Theme.of(context).colorScheme.onSurface' in lines[i] and 'const' in lines[i]:
        lines[i] = lines[i].replace('const ', '')
    elif 'Theme.of(context).colorScheme.onSurface' in lines[i]:
        # remove const from the previous line if it has it
        if i > 0 and 'const ' in lines[i-1]:
             lines[i-1] = lines[i-1].replace('const ', '')

content = '\n'.join(lines)
with open(path_login, 'w', encoding='utf-8') as f:
    f.write(content)
