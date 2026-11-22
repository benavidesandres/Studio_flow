# Imágenes oficiales desde el espejo público de AWS: evita el límite de descargas de Docker Hub en Render.
# --- Build: compila el .jar con Maven (los tests ya se ejecutan en local) ---
FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# --- Runtime: solo el JRE y el .jar ---
FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# El plan gratuito de Render da 512 MB: la JVM se ajusta a la memoria del contenedor.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
# Render inyecta PORT; application.yml ya lo lee con ${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
