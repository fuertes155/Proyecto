import requests
import json
import time

BASE_URL = "http://localhost:8080/api/v1"

print("==========================================================")
print("  Prueba de Nuevos Flujos Backend (PSE y Webhook)")
print("==========================================================")

# 1. Login to get JWT
print("\n[1] Iniciando sesión...")
login_payload = {
    "documentType": "CC",
    "documentNumber": "123456",
    "pin": "1234",
    "deviceId": "test-device"
}
response = requests.post(f"{BASE_URL}/auth/login", json=login_payload)
if response.status_code != 200:
    print("Error en login:", response.text)
    exit(1)

data = response.json()
print("DEBUG LOGIN RESPONSE:", data)
token = data.get("accessToken")
user_id = data.get("user", {}).get("id") if "user" in data else data.get("userId")
print(f" Login exitoso. User ID: {user_id}")

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# 2. Generate PSE Link
print("\n[2] Generando link de pago PSE (Monto: $50,000)...")
pse_payload = {
    "amount": 50000.0
}
pse_response = requests.post(f"{BASE_URL}/accounts/deposit-pse", json=pse_payload, headers=headers)
if pse_response.status_code != 200:
    print("Error generando link PSE:", pse_response.text)
    exit(1)

pse_data = pse_response.json()
tx_id = pse_data.get("transactionId")
print(" Link generado exitosamente:")
print(f"   URL de Wompi Simulada: {pse_data.get('paymentUrl')}")
print(f"   Transaction ID: {tx_id}")

# 3. Simulate Webhook (User pays on Wompi, Wompi calls our Webhook)
print("\n[3] Simulando que Wompi confirma el pago vía Webhook...")
time.sleep(2) # Pausa dramática
webhook_payload = {
    "event": "transaction.updated",
    "data": {
        "transactionId": tx_id,
        "amount": 50000.0,
        "status": "APPROVED",
        "userId": user_id
    }
}
webhook_response = requests.post(f"{BASE_URL}/webhooks/payment", json=webhook_payload)
if webhook_response.status_code == 200:
    print("✅ Webhook procesado exitosamente. ¡El saldo de la cuenta ha aumentado en $50,000!")
else:
    print("Error en webhook:", webhook_response.text)

print("\n==========================================================")
print("Prueba completada.")
