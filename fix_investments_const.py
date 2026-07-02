import re
import os
import glob

base_path = r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\investment\\presentation'
dart_files = glob.glob(os.path.join(base_path, '**', '*.dart'), recursive=True)

def process_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Generic const remover
    # We look for const <Word>(... Theme.of ...) and just remove const
    # A simplified approach since nested parentheses are hard to match perfectly with regex:
    # Just look for any const word( followed by anything then Theme.of inside the same block of lines
    content = re.sub(r'const\s+([A-Za-z0-9_]+\s*\((?:[^)(]+|\((?:[^)(]+|\([^)(]*\))*\))*\bTheme\.of\b)', r'\1', content, flags=re.DOTALL)
    
    # Just in case, let's manually remove const for specific classes
    content = re.sub(r'const\s+Container\((.*?Theme\.of.*?)\)', r'Container(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+BoxDecoration\((.*?Theme\.of.*?)\)', r'BoxDecoration(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+LinearGradient\((.*?Theme\.of.*?)\)', r'LinearGradient(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+EdgeInsetsGeometry\((.*?Theme\.of.*?)\)', r'EdgeInsetsGeometry(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+Border\((.*?Theme\.of.*?)\)', r'Border(\1)', content, flags=re.DOTALL)
    content = re.sub(r'const\s+BorderSide\((.*?Theme\.of.*?)\)', r'BorderSide(\1)', content, flags=re.DOTALL)

    # Some fallback for anything that still says 'const' right before a widget that has Theme.of
    # Let's try replacing all "const " on lines that have Theme.of
    lines = content.split('\n')
    for i in range(len(lines)):
        if 'Theme.of' in lines[i]:
            lines[i] = lines[i].replace('const ', '')
            
    # Also if a previous line had const
    for i in range(1, len(lines)):
        if 'Theme.of' in lines[i]:
            lines[i-1] = lines[i-1].replace('const ', '')
            lines[i-2] = lines[i-2].replace('const ', '')
            
    content = '\n'.join(lines)
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for path in dart_files:
    process_file(path)
