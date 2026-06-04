FROM node:20-alpine AS builder
WORKDIR /workspace
COPY package*.json ./
RUN npm install --no-audit --no-fund
COPY . .
ENV VITE_API_TARGET=/api
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /workspace/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
