FROM node:20-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY index.html ./
COPY tsconfig.json vitest.config.ts playwright.config.ts vite.config.ts ./
COPY .env.example ./
COPY src ./src

ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV NODE_OPTIONS=--max-old-space-size=4096

# Docker 构建阶段跳过类型检查并关闭压缩，避免低内存环境下被 OOM Kill；
# 类型检查在 CI/Test 阶段执行。
RUN npx vite build --minify=false

FROM nginx:1.27-alpine

COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
