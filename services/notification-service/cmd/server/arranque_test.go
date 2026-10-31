package main

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/json"
	"encoding/pem"
	"errors"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"firebase.google.com/go/v4/messaging"
	"github.com/gin-gonic/gin"
)

// ---------------------------------------------------------------------------
// Ayudas
// ---------------------------------------------------------------------------

// entorno simula las variables de entorno del proceso.
func entorno(vars map[string]string) func(string) string {
	return func(nombre string) string { return vars[nombre] }
}

// fabricaFCM devuelve una fábrica de clientes que responde con client/err y anota con qué
// archivo de credenciales la llamaron.
func fabricaFCM(client MessagingClient, err error, credenciales *string) fcmFactory {
	return func(_ context.Context, credentialsFile string) (MessagingClient, error) {
		if credenciales != nil {
			*credenciales = credentialsFile
		}
		return client, err
	}
}

// cuentaDeServicioFalsa escribe un service_account.json con una llave RSA recién generada:
// Firebase lo acepta para crear el cliente sin conectarse a Google.
func cuentaDeServicioFalsa(t *testing.T) string {
	t.Helper()
	llave, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatalf("no se pudo generar la llave: %v", err)
	}
	der, err := x509.MarshalPKCS8PrivateKey(llave)
	if err != nil {
		t.Fatalf("no se pudo codificar la llave: %v", err)
	}
	cuenta, _ := json.Marshal(map[string]string{
		"type":           "service_account",
		"project_id":     "met-pruebas",
		"private_key_id": "llave-de-prueba",
		"private_key":    string(pem.EncodeToMemory(&pem.Block{Type: "PRIVATE KEY", Bytes: der})),
		"client_email":   "pruebas@met-pruebas.iam.gserviceaccount.com",
		"client_id":      "1234567890",
		"token_uri":      "https://oauth2.googleapis.com/token",
	})
	ruta := filepath.Join(t.TempDir(), "service_account.json")
	if err := os.WriteFile(ruta, cuenta, 0o600); err != nil {
		t.Fatalf("no se pudo escribir la cuenta de servicio: %v", err)
	}
	return ruta
}

// ---------------------------------------------------------------------------
// Tests: configuración
// ---------------------------------------------------------------------------

func TestLeeLaConfiguracionDelEntorno(t *testing.T) {
	cfg, err := loadConfig(entorno(map[string]string{
		"PORT":                           "9000",
		"NOTIFICATION_API_KEY":           "clave-del-backend",
		"GOOGLE_APPLICATION_CREDENTIALS": "/secretos/firebase.json",
		"FCM_OPTIONAL":                   "true",
	}))

	if err != nil {
		t.Fatalf("no se esperaba error: %v", err)
	}
	esperada := config{port: "9000", apiKey: "clave-del-backend", credentialsFile: "/secretos/firebase.json", fcmOptional: true}
	if cfg != esperada {
		t.Errorf("configuración = %+v, se esperaba %+v", cfg, esperada)
	}
}

func TestUsaElPuerto8090YFirebaseObligatorioPorDefecto(t *testing.T) {
	cfg, err := loadConfig(entorno(map[string]string{"NOTIFICATION_API_KEY": "clave-del-backend"}))

	if err != nil {
		t.Fatalf("no se esperaba error: %v", err)
	}
	if cfg.port != "8090" {
		t.Errorf("se esperaba el puerto 8090, llegó %q", cfg.port)
	}
	if cfg.fcmOptional {
		t.Error("sin FCM_OPTIONAL, Firebase debe ser obligatorio")
	}
}

func TestNoArrancaSinLlaveDeApi(t *testing.T) {
	_, err := loadConfig(entorno(map[string]string{"PORT": "9000"}))

	if err == nil || !strings.Contains(err.Error(), "NOTIFICATION_API_KEY") {
		t.Errorf("se esperaba un error que mencione NOTIFICATION_API_KEY, llegó %v", err)
	}
}

// ---------------------------------------------------------------------------
// Tests: elección del cliente de mensajería
// ---------------------------------------------------------------------------

func TestUsaElClienteDeFirebaseConLasCredencialesConfiguradas(t *testing.T) {
	var credenciales string
	real := successClient()

	client, err := messagingClientFor(context.Background(),
		config{credentialsFile: "/secretos/firebase.json"}, fabricaFCM(real, nil, &credenciales))

	if err != nil {
		t.Fatalf("no se esperaba error: %v", err)
	}
	if client != real {
		t.Error("se esperaba el cliente que creó Firebase")
	}
	if credenciales != "/secretos/firebase.json" {
		t.Errorf("se esperaba crear Firebase con /secretos/firebase.json, llegó %q", credenciales)
	}
}

func TestSinFirebaseYSinFcmOpcionalNoArranca(t *testing.T) {
	_, err := messagingClientFor(context.Background(), config{},
		fabricaFCM(nil, errors.New("credenciales inválidas"), nil))

	if err == nil {
		t.Fatal("en producción, sin Firebase el servicio no debe arrancar")
	}
}

func TestConFcmOpcionalArrancaConUnClienteQueRechazaCadaEnvio(t *testing.T) {
	client, err := messagingClientFor(context.Background(), config{fcmOptional: true},
		fabricaFCM(nil, errors.New("credenciales inválidas"), nil))

	if err != nil {
		t.Fatalf("con FCM_OPTIONAL=true debe arrancar, llegó %v", err)
	}
	if _, err := client.Send(context.Background(), &messaging.Message{Token: "token-1"}); err == nil {
		t.Error("el cliente deshabilitado debe rechazar cada envío")
	}
}

// ---------------------------------------------------------------------------
// Tests: Firebase real (sin red)
// ---------------------------------------------------------------------------

func TestCreaElClienteDeFirebaseConUnaCuentaDeServicio(t *testing.T) {
	client, err := newFirebaseClient(context.Background(), cuentaDeServicioFalsa(t))

	if err != nil {
		t.Fatalf("no se esperaba error: %v", err)
	}
	if client == nil {
		t.Error("se esperaba un cliente de mensajería")
	}
}

func TestFallaSiElArchivoDeCredencialesNoExiste(t *testing.T) {
	_, err := newFirebaseClient(context.Background(), filepath.Join(t.TempDir(), "no-existe.json"))

	if err == nil {
		t.Error("se esperaba un error con un archivo de credenciales inexistente")
	}
}

// ---------------------------------------------------------------------------
// Tests: arranque completo
// ---------------------------------------------------------------------------

func TestArrancaEscuchandoEnElPuertoConfiguradoConLasRutasListas(t *testing.T) {
	gin.SetMode(gin.TestMode)
	var direccion string
	var router *gin.Engine

	err := run(entorno(map[string]string{"PORT": "9000", "NOTIFICATION_API_KEY": "clave-del-backend"}),
		fabricaFCM(successClient(), nil, nil),
		func(r *gin.Engine, addr string) error { router, direccion = r, addr; return nil })

	if err != nil {
		t.Fatalf("no se esperaba error: %v", err)
	}
	if direccion != ":9000" {
		t.Errorf("se esperaba escuchar en :9000, llegó %q", direccion)
	}
	w := httptest.NewRecorder()
	router.ServeHTTP(w, httptest.NewRequest(http.MethodGet, "/health", nil))
	if w.Code != http.StatusOK {
		t.Errorf("la ruta de salud debe responder 200, llegó %d", w.Code)
	}
}

func TestNoEmpiezaAEscucharSiLaConfiguracionOFirebaseFallan(t *testing.T) {
	casos := []struct {
		name  string
		vars  map[string]string
		fabri fcmFactory
	}{
		{"sin llave de API", map[string]string{}, fabricaFCM(successClient(), nil, nil)},
		{"Firebase caído", map[string]string{"NOTIFICATION_API_KEY": "clave"}, fabricaFCM(nil, errors.New("sin credenciales"), nil)},
	}

	for _, tc := range casos {
		t.Run(tc.name, func(t *testing.T) {
			escucho := false
			err := run(entorno(tc.vars), tc.fabri, func(*gin.Engine, string) error { escucho = true; return nil })

			if err == nil {
				t.Error("se esperaba un error que detenga el proceso")
			}
			if escucho {
				t.Error("no debe empezar a escuchar")
			}
		})
	}
}

func TestDevuelveElErrorDelServidorHTTP(t *testing.T) {
	ocupado := errors.New("el puerto 9000 ya está en uso")

	err := run(entorno(map[string]string{"NOTIFICATION_API_KEY": "clave"}), fabricaFCM(successClient(), nil, nil),
		func(*gin.Engine, string) error { return ocupado })

	if !errors.Is(err, ocupado) {
		t.Errorf("se esperaba el error del servidor, llegó %v", err)
	}
}

// ---------------------------------------------------------------------------
// Tests: mensaje que se entrega a Firebase
// ---------------------------------------------------------------------------

func TestEntregaAFirebaseElTituloElTextoLosDatosYElTokenDelDispositivo(t *testing.T) {
	var enviado *messaging.Message
	client := &mockMessagingClient{sendFn: func(_ context.Context, m *messaging.Message) (string, error) {
		enviado = m
		return "mensaje-1", nil
	}}
	payload := PushRequest{
		UserID: "fcm-token-abc-123",
		Title:  "Transferencia recibida",
		Body:   "Te llegaron $50.000",
		Data:   map[string]string{"type": "TRANSFER", "transactionId": "tx-9"},
	}

	req := httptest.NewRequest(http.MethodPost, "/api/v1/notifications/push", toJSON(t, payload))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Api-Key", "test-key")
	w := httptest.NewRecorder()
	setupRouter("test-key", client).ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("se esperaba 200, llegó %d — cuerpo: %s", w.Code, w.Body.String())
	}
	if enviado.Token != "fcm-token-abc-123" || enviado.Notification.Title != "Transferencia recibida" ||
		enviado.Notification.Body != "Te llegaron $50.000" || enviado.Data["transactionId"] != "tx-9" {
		t.Errorf("mensaje entregado a Firebase = %+v / %+v", enviado, enviado.Notification)
	}
}

func TestConFirebaseDeshabilitadoCadaEnvioRespondeErrorDeFcm(t *testing.T) {
	payload := PushRequest{UserID: "token-1", Title: "Hola", Body: "Prueba"}

	req := httptest.NewRequest(http.MethodPost, "/api/v1/notifications/push", toJSON(t, payload))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Api-Key", "test-key")
	w := httptest.NewRecorder()
	setupRouter("test-key", disabledMessagingClient{}).ServeHTTP(w, req)

	if w.Code != http.StatusInternalServerError {
		t.Errorf("se esperaba 500, llegó %d", w.Code)
	}
	if !strings.Contains(w.Body.String(), "FCM deshabilitado") {
		t.Errorf("se esperaba el detalle del cliente deshabilitado, llegó %s", w.Body.String())
	}
}
