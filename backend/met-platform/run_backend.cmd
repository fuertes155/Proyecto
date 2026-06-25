@echo off
set JAVA_HOME=C:\Users\Samuel\.vscode\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64
set DB_PASSWORD=Met$Dev#2026!Secure
set JWT_SECRET=met-dev-jwt-secret-key-32chars-min-length-required-2026
set AES_KEY=met-dev-aes-key-32-chars-long!!!
"C:\Users\Samuel\.m2\wrapper\dists\apache-maven-3.9.15\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=dev
