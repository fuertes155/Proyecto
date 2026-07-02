import os

path_login = r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\auth\\presentation\\pages\\login_page.dart'
with open(path_login, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('child: const AspectRatio(', 'child: AspectRatio(')

with open(path_login, 'w', encoding='utf-8') as f:
    f.write(content)
