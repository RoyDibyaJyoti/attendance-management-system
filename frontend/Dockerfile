# ==============================================================================
# AMCS Frontend Production Dockerfile
# Multi-stage: Build React/Vite app -> Serve with hardened Nginx
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Frontend Assets
# ------------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency specifications and install cleanly
COPY package.json package-lock.json ./
RUN npm ci

# Copy frontend source code and build production bundle
COPY . .
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Production Nginx Server & Reverse Proxy
# ------------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# Install wget for container healthcheck
RUN apk add --no-cache wget

# Copy Nginx server configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled SPA static files from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Expose standard HTTP port
EXPOSE 80

# Health check probing /healthz
HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
