# Replaced minimal ENTRYPOINT image with a multi-stage Dockerfile for building a Vite + React (TypeScript) app and serving it with nginx
# Build stage. Node 22 matches CI; Vite 7 requires Node 20.19+ / 22.12+.
FROM node:25-alpine AS builder

WORKDIR /app

# copy package manifests first for better caching
COPY package*.json ./

# install dependencies (use npm ci when lockfile exists, otherwise npm install)
RUN npm set progress=false && \
    if [ -f package-lock.json ]; then \
      npm ci --silent; \
    else \
      npm install --silent; \
    fi

# copy rest of the sources and build
COPY . .
ENV NODE_ENV=production
RUN npm run build

# Production image
FROM nginx:stable-alpine AS production

# Remove default nginx static files
RUN rm -rf /usr/share/nginx/html/*

# Copy built files from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom nginx config to enable SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
