# Stage 1: Build the application
FROM eclipse-temurin:21-alpine AS builder

WORKDIR /emailservice

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package

# Stage 2: Create minimal runtime image
FROM eclipse-temurin:21-alpine

RUN apk add --no-cache curl

WORKDIR /emailservice

COPY --from=builder /emailservice/target/*.jar emailservice.jar

ENV JAVA_OPTS=""

EXPOSE 6005

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar emailservice.jar"]
