FROM nginx:alpine

WORKDIR /usr/share/nginx/html

# Copy frontend files
COPY . .

# Use custom nginx config
RUN echo "include /etc/nginx/conf.d/*.conf;" >> /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
