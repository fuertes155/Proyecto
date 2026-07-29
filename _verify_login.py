import urllib.request, json, base64
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.serialization import load_der_public_key

BASE = "http://localhost:8080/api"

def get_public_key():
    resp = urllib.request.urlopen(f"{BASE}/v1/auth/public-key")
    pk_b64 = json.loads(resp.read())["publicKey"]
    der = base64.b64decode(pk_b64)
    return load_der_public_key(der)

def encrypt_pin(pin: str, public_key) -> str:
    ciphertext = public_key.encrypt(pin.encode(), padding.PKCS1v15())
    return base64.b64encode(ciphertext).decode()

def post(path, body, headers=None):
    req = urllib.request.Request(
        f"{BASE}{path}",
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json", **(headers or {})},
        method="POST",
    )
    try:
        resp = urllib.request.urlopen(req)
        return resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

public_key = get_public_key()

doc_number = "999888777"
pin = "1234"

register_body = {
    "documentType": "CC",
    "documentNumber": doc_number,
    "email": "verify.script@example.com",
    "phone": "3001234567",
    "firstName": "Verify",
    "lastName": "Script",
    "pin": encrypt_pin(pin, public_key),
    "termsAccepted": True,
}
status, body = post("/v1/auth/register", register_body)
print("REGISTER", status, body)

login_body = {
    "documentType": "CC",
    "documentNumber": doc_number,
    "deviceId": "verify-script-device",
    "pin": encrypt_pin(pin, get_public_key()),
}
status, body = post(
    "/v1/auth/login",
    login_body,
    headers={"X-Device-Attestation": "dev-attestation-token-123", "X-Platform": "web"},
)
print("LOGIN (before activation)", status, body)
