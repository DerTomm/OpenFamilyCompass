# OpenFamilyCompass Frontend - Expo Development Server für Expo Go

FROM node:20-alpine

WORKDIR /app

# Install dependencies for Expo
RUN apk add --no-cache git

# Copy package files
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Copy source code
COPY . .

# Expose Expo ports
# 8081 = Metro bundler
# 19000 = Expo Dev Server (deprecated but some clients use it)
# 19001 = Expo Dev Server
EXPOSE 8081 19000 19001

# Set environment variables
ENV EXPO_DEVTOOLS_LISTEN_ADDRESS=0.0.0.0
ENV REACT_NATIVE_PACKAGER_HOSTNAME=0.0.0.0

# Healthcheck (check if Metro bundler is running)
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/status || exit 1

# Start Expo development server
# Use --lan for local network access (best for Docker)
CMD ["npx", "expo", "start", "--lan", "--port", "8081"]
