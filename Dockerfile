# ── Etapa 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Descargar dependencias primero (cache layer)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Etapa 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear directorio para montaje EFS
RUN mkdir -p /app/efs

# Copiar JAR generado
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Variables de entorno con valores por defecto
ENV AWS_S3_BUCKET=guias-despacho-bucket
ENV AWS_REGION=us-east-1
ENV EFS_MOUNT_PATH=/app/efs

ENTRYPOINT ["java", "-jar", "app.jar"]
