# ──────────────────────────────────────────────
# Stage 1: Build Flutter Web
# ──────────────────────────────────────────────
FROM ghcr.io/cirruslabs/flutter:stable AS builder

WORKDIR /app

# Copiar dependencias primero para cachear la capa
COPY pubspec.yaml pubspec.lock ./
RUN flutter pub get

# Copiar el resto del código y compilar
COPY . .
RUN flutter config --enable-web
RUN flutter build web --release \
    --dart-define=API_BASE_URL=${API_BASE_URL} \
    --dart-define=HMAC_SECRET=${HMAC_SECRET}

# ──────────────────────────────────────────────
# Stage 2: Servir con Nginx
# ──────────────────────────────────────────────
FROM nginx:1.27-alpine

# Eliminar config por defecto
RUN rm /etc/nginx/conf.d/default.conf

# Copiar config específica para SPA (maneja rutas de Flutter)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos compilados
COPY --from=builder /app/build/web /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
