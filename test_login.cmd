@echo off
curl.exe -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"documentType\":\"CC\",\"documentNumber\":\"1086578123\",\"pin\":\"1234\"}"
