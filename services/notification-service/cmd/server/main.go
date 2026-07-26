package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"time"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"github.com/gin-gonic/gin"
	"google.golang.org/api/option"
)

type PushRequest struct {
	UserID  string            `json:"userId" binding:"required"`
	Title   string            `json:"title" binding:"required"`
	Body    string            `json:"body" binding:"required"`
	Data    map[string]string `json:"data"`
	Channel string            `json:"channel"`
}

type PushResponse struct {
	MessageID string `json:"messageId"`
	Status    string `json:"status"`
	LatencyMs int64  `json:"latencyMs"`
}

var fcmClient *messaging.Client

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

	credentialsFile := os.Getenv("GOOGLE_APPLICATION_CREDENTIALS")
	if credentialsFile != "" {
		log.Printf("Inicializando Firebase con credenciales locales: %s", credentialsFile)
		opt := option.WithCredentialsFile(credentialsFile)
		app, err = firebase.NewApp(ctx, nil, opt)
	} else {
		log.Println("Inicializando Firebase usando Application Default Credentials (ADC)")
		app, err = firebase.NewApp(ctx, nil)
	}

	if err != nil {
		log.Fatalf("Error al inicializar Firebase App: %v", err)
	}

	fcmClient, err = app.Messaging(ctx)
	if err != nil {
		log.Fatalf("Error al obtener el cliente FCM: %v", err)
	}
	log.Println("Cliente FCM inicializado correctamente")

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
		v1.POST("/notifications/push", sendPushHandler)
	}

	log.Printf("notification-service escuchando en :%s", port)
	if err := router.Run(":" + port); err != nil {
		log.Fatal(err)
	}
}

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

func sendPushHandler(c *gin.Context) {
	start := time.Now()

	var req PushRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"code": "VALIDATION_ERROR", "message": err.Error()})
		return
	}

	// 1. Construir el mensaje para Firebase (FCM)
	// Para este ejemplo, asumimos que UserID que nos envía el backend de Java
	// es directamente el FCM Token del dispositivo, o usar un Topic si es broadcast.
	// En un escenario real, tendríamos una DB aquí en Go (o un cache) mapeando UserID -> FCM Tokens.
	message := &messaging.Message{
		Notification: &messaging.Notification{
			Title: req.Title,
			Body:  req.Body,
		},
		Data:  req.Data,
		Token: req.UserID, // Asumiendo Token directo por simplicidad de la integración
	}

	// 2. Enviar el mensaje a través del cliente FCM
	response, err := fcmClient.Send(c.Request.Context(), message)
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
