FROM eclipse-temurin:17-jdk-focal

WORKDIR /emailservice

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src

ENV PORT=6005
EXPOSE 6005

CMD ["./mvnw", "spring-boot:run"]