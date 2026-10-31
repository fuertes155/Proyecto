package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"github.com/gin-gonic/gin"
	"google.golang.org/api/option"
)

// ---------------------------------------------------------------------------
// Tipos de dominio
// ---------------------------------------------------------------------------

// PushRequest es el payload que recibe el endpoint de notificaciones.
type PushRequest struct {
	UserID  string            `json:"userId" binding:"required"`
	Title   string            `json:"title" binding:"required"`
	Body    string            `json:"body" binding:"required"`
	Data    map[string]string `json:"data"`
	Channel string            `json:"channel"`
}

// PushResponse es la respuesta que se devuelve al backend de Java.
type PushResponse struct {
	MessageID string `json:"messageId"`
	Status    string `json:"status"`
	LatencyMs int64  `json:"latencyMs"`
}

// ---------------------------------------------------------------------------
// Interfaz de mensajería (permite mock en tests)
// ---------------------------------------------------------------------------

// MessagingClient abstrae el cliente FCM de Firebase para permitir
// inyección de dependencias y testing unitario sin conexión real a Firebase.
type MessagingClient interface {
	Send(ctx context.Context, message *messaging.Message) (string, error)
}

// disabledMessagingClient se usa en desarrollo local cuando no hay credenciales
// de Firebase: el servicio arranca (para que el resto del stack funcione) pero
// cada envío devuelve un error explícito en lugar de fallar silenciosamente.
type disabledMessagingClient struct{}

func (disabledMessagingClient) Send(_ context.Context, _ *messaging.Message) (string, error) {
	return "", errors.New("FCM deshabilitado: no hay credenciales de Firebase configuradas")
}

// ---------------------------------------------------------------------------
// Construcción del router (separada de main para poder testearse)
// ---------------------------------------------------------------------------

// NewRouter construye y devuelve el router Gin con todos los endpoints
// configurados. Recibe el cliente de mensajería como dependencia para
// facilitar el testing sin Firebase real.
func NewRouter(apiKey string, client MessagingClient) *gin.Engine {
	router := gin.New()
	router.Use(gin.Recovery(), gin.Logger())

	// Health check público — no requiere API Key
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "UP"})
	})

	// Grupo protegido con API Key
	v1 := router.Group("/api/v1")
	v1.Use(apiKeyMiddleware(apiKey))
	{
		v1.POST("/notifications/push", sendPushHandler(client))
	}

	return router
}

// ---------------------------------------------------------------------------
// Handlers
// ---------------------------------------------------------------------------

// sendPushHandler devuelve un HandlerFunc de Gin que envía notificaciones
// push a través del cliente de mensajería inyectado.
func sendPushHandler(client MessagingClient) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()

		var req PushRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"code": "VALIDATION_ERROR", "message": err.Error()})
			return
		}

		// Construir el mensaje para Firebase (FCM).
		// UserID se trata como FCM Token del dispositivo. En producción se
		// mapearía userId → FCM tokens desde una tabla en BD.
		message := &messaging.Message{
			Notification: &messaging.Notification{
				Title: req.Title,
				Body:  req.Body,
			},
			Data:  req.Data,
			Token: req.UserID,
		}

		response, err := client.Send(c.Request.Context(), message)
		if err != nil {
			log.Printf("Error al enviar notificación push: %v", err)
			c.JSON(http.StatusInternalServerError, gin.H{
				"code":    "FCM_ERROR",
				"message": "Fallo al enviar notificación a través de Firebase",
				"details": err.Error(),
			})
			return
		}

		c.JSON(http.StatusOK, PushResponse{
			MessageID: response,
			Status:    "SENT",
			LatencyMs: time.Since(start).Milliseconds(),
		})
	}
}

// ---------------------------------------------------------------------------
// Middleware de autenticación
// ---------------------------------------------------------------------------

// apiKeyMiddleware verifica que la solicitud incluya el header
// X-Api-Key con el valor correcto. Devuelve 401 si falta y 403 si es inválida.
func apiKeyMiddleware(expectedKey string) gin.HandlerFunc {
	return func(c *gin.Context) {
		providedKey := c.GetHeader("X-Api-Key")

		if providedKey == "" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"code":    "MISSING_API_KEY",
				"message": "Se requiere el header X-Api-Key",
			})
			return
		}

		// Comparación en tiempo constante para evitar timing attacks
		if !secureCompare(providedKey, expectedKey) {
			c.AbortWithStatusJSON(http.StatusForbidden, gin.H{
				"code":    "INVALID_API_KEY",
				"message": "API Key inválida o no autorizada",
			})
			return
		}

		c.Next()
	}
}

// secureCompare compara dos strings en tiempo constante para prevenir
// ataques de temporización (timing attacks).
func secureCompare(a, b string) bool {
	if len(a) != len(b) {
		return false
	}
	var result byte
	for i := 0; i < len(a); i++ {
		result |= a[i] ^ b[i]
	}
	return result == 0
}

// ---------------------------------------------------------------------------
// Punto de entrada
// ---------------------------------------------------------------------------

func main() {
	serve := func(router *gin.Engine, addr string) error { return router.Run(addr) }
	if err := run(os.Getenv, newFirebaseClient, serve); err != nil {
		log.Fatal(err)
	}
}

// run arma el servicio y lo deja escuchando. Recibe sus dependencias (variables de entorno,
// creación del cliente FCM y servidor HTTP) para poder probar el arranque sin Firebase ni red;
// el error que devuelve es el que en main detiene el proceso.
func run(getenv func(string) string, newFCM fcmFactory, serve func(*gin.Engine, string) error) error {
	cfg, err := loadConfig(getenv)
	if err != nil {
		return err
	}
	client, err := messagingClientFor(context.Background(), cfg, newFCM)
	if err != nil {
		return err
	}
	log.Printf("notification-service escuchando en :%s", cfg.port)
	return serve(NewRouter(cfg.apiKey, client), ":"+cfg.port)
}

// config de arranque, leída de las variables de entorno.
type config struct {
	port            string
	apiKey          string
	credentialsFile string
	// FCM_OPTIONAL=true solo en desarrollo local: permite arrancar sin credenciales
	// de Firebase. En producción la variable no se define y el fallo es fatal.
	fcmOptional bool
}

func loadConfig(getenv func(string) string) (config, error) {
	cfg := config{
		port:            getenv("PORT"),
		apiKey:          getenv("NOTIFICATION_API_KEY"),
		credentialsFile: getenv("GOOGLE_APPLICATION_CREDENTIALS"),
		fcmOptional:     getenv("FCM_OPTIONAL") == "true",
	}
	if cfg.port == "" {
		cfg.port = "8090"
	}
	if cfg.apiKey == "" {
		return config{}, errors.New("NOTIFICATION_API_KEY no está configurada. El servicio no puede iniciarse sin una API Key")
	}
	return cfg, nil
}

// fcmFactory crea el cliente FCM; en las pruebas se reemplaza por uno falso.
type fcmFactory func(ctx context.Context, credentialsFile string) (MessagingClient, error)

// newFirebaseClient inicializa Firebase (con un service_account.json o con Application Default
// Credentials) y devuelve su cliente de mensajería.
func newFirebaseClient(ctx context.Context, credentialsFile string) (MessagingClient, error) {
	var opts []option.ClientOption
	if credentialsFile != "" {
		log.Printf("Inicializando Firebase con credenciales locales: %s", credentialsFile)
		opts = append(opts, option.WithCredentialsFile(credentialsFile))
	} else {
		log.Println("Inicializando Firebase usando Application Default Credentials (ADC)")
	}
	app, err := firebase.NewApp(ctx, nil, opts...)
	if err != nil {
		return nil, fmt.Errorf("error al inicializar Firebase App: %w", err)
	}
	client, err := app.Messaging(ctx)
	if err != nil {
		return nil, fmt.Errorf("error al obtener el cliente FCM: %w", err)
	}
	return client, nil
}

// messagingClientFor devuelve el cliente FCM real. Si Firebase no está disponible, con
// FCM_OPTIONAL=true usa uno deshabilitado (cada envío responde con error) y sin él falla.
func messagingClientFor(ctx context.Context, cfg config, newFCM fcmFactory) (MessagingClient, error) {
	client, err := newFCM(ctx, cfg.credentialsFile)
	if err == nil {
		log.Println("Cliente FCM inicializado correctamente")
		return client, nil
	}
	if !cfg.fcmOptional {
		return nil, err
	}
	log.Printf("⚠️  Firebase no disponible (%v). FCM_OPTIONAL=true: los envíos push devolverán error.", err)
	return disabledMessagingClient{}, nil
}
