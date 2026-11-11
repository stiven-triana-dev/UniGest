# UniGest en un contenedor (lo usa Render para la demo pública).
# Etapa 1: compila el JAR con Maven. Las pruebas corren en GitHub Actions.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: solo el JRE y el JAR, con un usuario sin privilegios.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 unigest
COPY --from=build /app/target/*.jar app.jar
USER unigest

# Dentro del contenedor hay que escuchar en todas las interfaces; Render define PORT.
# Memoria: el plan gratuito de Render da 512 MB; con la mitad para datos (256 MB)
# UniGest usa unos 320 MB en total, medido con la demo cargada.
ENV BIND_ADDRESS=0.0.0.0 \
    PORT=8080 \
    SESSION_COOKIE_SECURE=true \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
