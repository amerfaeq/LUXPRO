import sys
with open(sys.argv[1], 'r', encoding='utf-16le') as f:
    lines = f.readlines()
    for line in lines[-100:]:
        print(line, end='')
