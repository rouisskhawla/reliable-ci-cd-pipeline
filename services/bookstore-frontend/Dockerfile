FROM node:24-alpine AS build

ARG BUILD_CONFIG=production

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=optional --no-audit --silent

COPY angular.json ./
COPY tsconfig*.json ./
COPY src ./src
COPY public ./public

RUN npx ng build --configuration=${BUILD_CONFIG}

FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/bookstore-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]