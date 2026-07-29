package main

import (
	"context"
	"errors"
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
	port := os.Getenv("PORT")
	if port == "" {
		port = "8090"
	}

	apiKey := os.Getenv("NOTIFICATION_API_KEY")
	if apiKey == "" {
		log.Fatal("NOTIFICATION_API_KEY no está configurada. El servicio no puede iniciarse sin una API Key.")
	}

	// Inicializar Firebase App (usando Application Default Credentials o un service_account.json)
	ctx := context.Background()
	var app *firebase.App
	var err error

	// FCM_OPTIONAL=true solo en desarrollo local: permite arrancar sin credenciales
	// de Firebase. En producción la variable no se define y el fallo es fatal.
	fcmOptional := os.Getenv("FCM_OPTIONAL") == "true"

	credentialsFile := os.Getenv("GOOGLE_APPLICATION_CREDENTIALS")
	if credentialsFile != "" {
		log.Printf("Inicializando Firebase con credenciales locales: %s", credentialsFile)
		opt := option.WithCredentialsFile(credentialsFile)
		app, err = firebase.NewApp(ctx, nil, opt)
	} else {
		log.Println("Inicializando Firebase usando Application Default Credentials (ADC)")
		app, err = firebase.NewApp(ctx, nil)
	}

	var client MessagingClient
	if err != nil {
		if !fcmOptional {
			log.Fatalf("Error al inicializar Firebase App: %v", err)
		}
		log.Printf("⚠️  Firebase no disponible (%v). FCM_OPTIONAL=true: los envíos push devolverán error.", err)
		client = disabledMessagingClient{}
	} else {
		fcmClient, mErr := app.Messaging(ctx)
		if mErr != nil {
			if !fcmOptional {
				log.Fatalf("Error al obtener el cliente FCM: %v", mErr)
			}
			log.Printf("⚠️  Cliente FCM no disponible (%v). FCM_OPTIONAL=true: los envíos push devolverán error.", mErr)
			client = disabledMessagingClient{}
		} else {
			log.Println("Cliente FCM inicializado correctamente")
			client = fcmClient
		}
	}

	router := NewRouter(apiKey, client)

	log.Printf("notification-service escuchando en :%s", port)
	if err := router.Run(":" + port); err != nil {
		log.Fatal(err)
	}
}
