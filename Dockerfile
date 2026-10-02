# =========================
# Etapa 1: Build
# =========================
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copiamos Maven Wrapper
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Damos permisos de ejecución al Maven Wrapper
RUN chmod +x mvnw

# Descargamos las dependencias
RUN ./mvnw dependency:go-offline -B

# Copiamos el código fuente
COPY src ./src

# Compilamos el proyecto
RUN ./mvnw clean package -DskipTests


# =========================
# Etapa 2: Runtime
# =========================
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/backend.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
