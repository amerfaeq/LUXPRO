import sys
import codecs

input_file = sys.argv[1]
output_file = input_file.replace('.txt', '_utf8.txt')

try:
    with codecs.open(input_file, 'r', encoding='utf-16le') as f_in:
        content = f_in.read()
    with codecs.open(output_file, 'w', encoding='utf-8') as f_out:
        f_out.write(content)
    print(f"Successfully converted to {output_file}")
except Exception as e:
    print(f"Error: {e}")
