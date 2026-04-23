FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S forgeql \
    && adduser -S forgeql -G forgeql

COPY --from=build /workspace/target/ForgeQL-*.jar /app/app.jar

USER forgeql

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]