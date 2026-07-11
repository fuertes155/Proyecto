@echo off
REM ==========================================
REM = WARNING: DEV ONLY. DO NOT USE IN PROD! =
REM ==========================================
setlocal DisableDelayedExpansion
set "JAVA_HOME=C:\Users\Samuel\.vscode\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64"
set "DB_HOST=127.0.0.1"
set "DB_PORT=5433"
set "DB_NAME=met"
set "DB_USER=met"
set "DB_PASSWORD=MetDev2026Secure"
set "JWT_SECRET=super_secret_jwt_key_that_is_at_least_256_bits_long!"
set "AES_KEY=met-dev-aes-key-32-chars-long!!!"
set "SPRING_PROFILES_ACTIVE=dev"
set "REDIS_PASSWORD=RedisDev2026Secure"
set "CORS_ORIGINS=*"
set "GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE"
"C:\Users\Samuel\.m2\wrapper\dists\apache-maven-3.9.15\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\bin\mvn.cmd" spring-boot:run
