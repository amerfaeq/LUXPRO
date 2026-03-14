import base64
import binascii

sha1_hex = "A4:C5:1E:9B:F0:BF:A0:14:60:21:3F:0A:FF:79:DC:BD:5F:27:A8:36".replace(":", "")
sha1_bytes = binascii.unhexlify(sha1_hex)
base64_hash = base64.b64encode(sha1_bytes).decode('utf-8')

print(f"SHA-1: {sha1_hex}")
print(f"Base64 Key Hash: {base64_hash}")
