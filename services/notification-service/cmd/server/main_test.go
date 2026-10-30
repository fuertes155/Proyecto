package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"firebase.google.com/go/v4/messaging"
	"github.com/gin-gonic/gin"
)

// ---------------------------------------------------------------------------
// Mock de MessagingClient
// ---------------------------------------------------------------------------

// mockMessagingClient implementa MessagingClient para tests.
type mockMessagingClient struct {
	// sendFn permite personalizar el comportamiento de Send en cada test.
	sendFn func(ctx context.Context, message *messaging.Message) (string, error)
}

func (m *mockMessagingClient) Send(ctx context.Context, message *messaging.Message) (string, error) {
	return m.sendFn(ctx, message)
}

// successClient devuelve un mock que simula un envío exitoso.
func successClient() *mockMessagingClient {
	return &mockMessagingClient{
		sendFn: func(ctx context.Context, message *messaging.Message) (string, error) {
			return "projects/test-project/messages/mock-message-id-123", nil
		},
	}
}

// errorClient devuelve un mock que simula un fallo de FCM.
func errorClient() *mockMessagingClient {
	return &mockMessagingClient{
		sendFn: func(ctx context.Context, message *messaging.Message) (string, error) {
			return "", errors.New("FCM: QuotaExceeded")
		},
	}
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

func setupRouter(apiKey string, client MessagingClient) *gin.Engine {
	gin.SetMode(gin.TestMode)
	return NewRouter(apiKey, client)
}

func toJSON(t *testing.T, v any) *bytes.Buffer {
	t.Helper()
	b, err := json.Marshal(v)
	if err != nil {
		t.Fatalf("error al convertir a JSON: %v", err)
	}
	return bytes.NewBuffer(b)
}

// ---------------------------------------------------------------------------
// Tests: secureCompare
// ---------------------------------------------------------------------------

func TestComparacionSegura(t *testing.T) {
	tests := []struct {
		name string
		a, b string
		want bool
	}{
		{"iguales", "mi-api-key-secreta", "mi-api-key-secreta", true},
		{"distintas misma longitud", "aaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbbb", false},
		{"a vacía", "", "algo", false},
		{"b vacía", "algo", "", false},
		{"ambas vacías", "", "", true},
		{"distintas longitudes", "corta", "key-mucho-mas-larga", false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := secureCompare(tt.a, tt.b); got != tt.want {
				t.Errorf("secureCompare(%q, %q) = %v, se esperaba %v", tt.a, tt.b, got, tt.want)
			}
		})
	}
}

// ---------------------------------------------------------------------------
// Tests: GET /health
// ---------------------------------------------------------------------------

func TestRutaDeSalud(t *testing.T) {
	router := setupRouter("test-key", successClient())

	req, _ := http.NewRequest(http.MethodGet, "/health", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("se esperaba 200, llegó %d", w.Code)
	}

	var body map[string]string
	if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
		t.Fatalf("respuesta JSON inválida: %v", err)
	}
	if body["status"] != "UP" {
		t.Errorf("se esperaba status=UP, llegó %q", body["status"])
	}
}

// ---------------------------------------------------------------------------
// Tests: Middleware de autenticación
// ---------------------------------------------------------------------------

func TestLlaveDeApiFaltante(t *testing.T) {
	router := setupRouter("test-key", successClient())

	req, _ := http.NewRequest(http.MethodPost, "/api/v1/notifications/push", nil)
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusUnauthorized {
		t.Errorf("se esperaba 401, llegó %d", w.Code)
	}

	var body map[string]string
	json.Unmarshal(w.Body.Bytes(), &body)
	if body["code"] != "MISSING_API_KEY" {
		t.Errorf("se esperaba code=MISSING_API_KEY, llegó %q", body["code"])
	}
}

func TestLlaveDeApiInvalida(t *testing.T) {
	router := setupRouter("clave-correcta", successClient())

	req, _ := http.NewRequest(http.MethodPost, "/api/v1/notifications/push", nil)
	req.Header.Set("X-Api-Key", "clave-incorrecta")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusForbidden {
		t.Errorf("se esperaba 403, llegó %d", w.Code)
	}

	var body map[string]string
	json.Unmarshal(w.Body.Bytes(), &body)
	if body["code"] != "INVALID_API_KEY" {
		t.Errorf("se esperaba code=INVALID_API_KEY, llegó %q", body["code"])
	}
}

// ---------------------------------------------------------------------------
// Tests: POST /api/v1/notifications/push
// ---------------------------------------------------------------------------

func TestEnviarPushExitoso(t *testing.T) {
	router := setupRouter("test-key", successClient())

	payload := PushRequest{
		UserID: "fcm-token-abc-123",
		Title:  "Transferencia exitosa",
		Body:   "Tu transferencia de $50.000 fue procesada.",
		Data:   map[string]string{"type": "TRANSFER"},
	}

	req, _ := http.NewRequest(http.MethodPost, "/api/v1/notifications/push", toJSON(t, payload))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Api-Key", "test-key")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("se esperaba 200, llegó %d — cuerpo: %s", w.Code, w.Body.String())
	}

	var resp PushResponse
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		t.Fatalf("respuesta JSON inválida: %v", err)
	}
	if resp.Status != "SENT" {
		t.Errorf("se esperaba status=SENT, llegó %q", resp.Status)
	}
	if resp.MessageID == "" {
		t.Error("se esperaba un messageId no vacío")
	}
}

func TestEnviarPushRechazaCamposFaltantes(t *testing.T) {
	router := setupRouter("test-key", successClient())

	// Payload incompleto: falta body y userId
	payload := map[string]string{"title": "Solo título"}

	req, _ := http.NewRequest(http.MethodPost, "/api/v1/notifications/push", toJSON(t, payload))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Api-Key", "test-key")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("se esperaba 400, llegó %d", w.Code)
	}

	var body map[string]string
	json.Unmarshal(w.Body.Bytes(), &body)
	if body["code"] != "VALIDATION_ERROR" {
		t.Errorf("se esperaba code=VALIDATION_ERROR, llegó %q", body["code"])
	}
}

func TestEnviarPushRechazaCuerpoVacio(t *testing.T) {
	router := setupRouter("test-key", successClient())

	req, _ := http.NewRequest(http.MethodPost, "/api/v1/notifications/push", bytes.NewBufferString("{}"))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Api-Key", "test-key")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("se esperaba 400, llegó %d", w.Code)
	}
}

func TestEnviarPushFallaDeFcm(t *testing.T) {
	router := setupRouter("test-key", errorClient())

	payload := PushRequest{
		UserID: "fcm-token-abc-123",
		Title:  "Test",
		Body:   "Mensaje de prueba",
	}

	req, _ := http.NewRequest(http.MethodPost, "/api/v1/notifications/push", toJSON(t, payload))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Api-Key", "test-key")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusInternalServerError {
		t.Errorf("se esperaba 500, llegó %d", w.Code)
	}

	var body map[string]string
	json.Unmarshal(w.Body.Bytes(), &body)
	if body["code"] != "FCM_ERROR" {
		t.Errorf("se esperaba code=FCM_ERROR, llegó %q", body["code"])
	}
}

func TestEnviarPushTipoDeContenidoInvalido(t *testing.T) {
	router := setupRouter("test-key", successClient())

	req, _ := http.NewRequest(http.MethodPost, "/api/v1/notifications/push",
		bytes.NewBufferString("not-json"))
	req.Header.Set("Content-Type", "text/plain")
	req.Header.Set("X-Api-Key", "test-key")
	w := httptest.NewRecorder()
	router.ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("se esperaba 400, llegó %d", w.Code)
	}
}
