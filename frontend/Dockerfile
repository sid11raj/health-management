# Frontend: build the Vite app, then serve it with nginx.
# VITE_API_URL is a build-time variable: /api lets nginx proxy API calls
# to the backend container (see nginx.conf), so the browser never needs
# to know the backend host.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
