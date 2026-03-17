import os

def replace_string_in_files(directory, old_string, new_string):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(('.java', '.xml', '.kt', '.kts', '.txt', '.json', '.cpp')):
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    if old_string in content:
                        new_content = content.replace(old_string, new_string)
                        with open(file_path, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        print(f"Updated: {file_path}")
                except Exception as e:
                    print(f"Error processing {file_path}: {e}")

if __name__ == "__main__":
    replace_string_in_files(r'd:\LUXPRO\app\src\main', 'com.luxpro.vip', 'com.luxpro.max')
