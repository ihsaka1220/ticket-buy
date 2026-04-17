FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Configure Aliyun Maven mirror
RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/settings.xml

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || echo "Dependency download completed with warnings"

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B || mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "app.jar"]
