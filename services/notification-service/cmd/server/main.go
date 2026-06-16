package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"
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

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8090"
	}

	apiKey := os.Getenv("NOTIFICATION_API_KEY")
	if apiKey == "" {
		log.Fatal("NOTIFICATION_API_KEY no está configurada. El servicio no puede iniciarse sin una API Key.")
	}

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

	// TODO: Integrar con FCM (Firebase Cloud Messaging) para Android
	// TODO: Integrar con APNs para iOS
	// TODO: Integrar con AWS SNS como alternativa
	messageID := "msg-" + time.Now().Format("20060102150405")

	c.JSON(http.StatusAccepted, PushResponse{
		MessageID: messageID,
		Status:    "QUEUED",
		LatencyMs: time.Since(start).Milliseconds(),
	})
}
