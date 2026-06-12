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

	router := gin.New()
	router.Use(gin.Recovery(), gin.Logger())

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "UP"})
	})

	v1 := router.Group("/api/v1")
	{
		v1.POST("/notifications/push", sendPushHandler)
	}

	log.Printf("notification-service listening on :%s", port)
	if err := router.Run(":" + port); err != nil {
		log.Fatal(err)
	}
}

func sendPushHandler(c *gin.Context) {
	start := time.Now()

	var req PushRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"code": "VALIDATION_ERROR", "message": err.Error()})
		return
	}

	// Integración futura: FCM, APNs, SNS
	messageID := "msg-" + time.Now().Format("20060102150405")

	c.JSON(http.StatusAccepted, PushResponse{
		MessageID: messageID,
		Status:    "QUEUED",
		LatencyMs: time.Since(start).Milliseconds(),
	})
}
