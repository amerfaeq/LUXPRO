import os

def replace_string_in_files(directory, old_string, new_string, extensions=('.java', '.xml', '.kt', '.kts', '.cpp', '.hpp', '.h')):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(extensions):
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                        content = f.read()
                    
                    if old_string in content:
                        new_content = content.replace(old_string, new_string)
                        with open(file_path, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        print(f"Updated: {file_path}")
                except Exception as e:
                    print(f"Error processing {file_path}: {e}")

if __name__ == "__main__":
    # 1. Update package declarations and class lookups
    replace_string_in_files(r'd:\LUXPRO\app\src\main', 'com.android.support', 'com.luxpro.max')
    replace_string_in_files(r'd:\LUXPRO\app\src\main', 'com/android/support', 'com/luxpro/max')
    
    # 2. Ensure library name is InternalSystem
    replace_string_in_files(r'd:\LUXPRO\app\src\main\java', 'System.loadLibrary("MyLibName")', 'System.loadLibrary("InternalSystem")')
    
    # 3. Clean up any remaining com.luxpro.vip just in case
    replace_string_in_files(r'd:\LUXPRO\app\src\main', 'com.luxpro.vip', 'com.luxpro.max')
    replace_string_in_files(r'd:\LUXPRO\app\src\main', 'com/luxpro/vip', 'com/luxpro/max')
