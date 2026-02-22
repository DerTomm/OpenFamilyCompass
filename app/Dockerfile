# Multi-stage build für OpenFamilyCompass Frontend (React Native Web)

# Stage 1: Build Stage
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Copy source code
COPY . .

# Build for web
RUN npx expo export:web

# Stage 2: Runtime Stage (Nginx)
FROM nginx:alpine

# Remove default nginx config
RUN rm -rf /etc/nginx/conf.d/*
RUN rm -rf /usr/share/nginx/html/*

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built app from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Add healthcheck script
RUN apk add --no-cache curl

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
