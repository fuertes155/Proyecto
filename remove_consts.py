import re
import os
import glob
import subprocess

base_path = r'd:\\Proyecto Finanzas\\mobile\\met_app\\lib\\features\\investment\\presentation'
dart_files = glob.glob(os.path.join(base_path, '**', '*.dart'), recursive=True)

def process_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Remove all "const " string occurrences, except inside imports or similar
    content = content.replace('const ', '')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for path in dart_files:
    process_file(path)
