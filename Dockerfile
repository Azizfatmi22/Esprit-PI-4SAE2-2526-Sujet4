FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install --legacy-peer-deps

COPY . .

# FIX ANGULAR BUILD
RUN node --max-old-space-size=4096 ./node_modules/@angular/cli/bin/ng build --configuration production

FROM nginx:alpine

COPY --from=builder /app/dist/formini-app/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]