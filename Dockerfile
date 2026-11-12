# UniGest en un contenedor (lo usa Render para la demo pública).
# Etapa 1: compila el JAR con Maven. Las pruebas corren en GitHub Actions.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: solo el JRE y la aplicación, con un usuario sin privilegios.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 unigest
COPY --from=build /app/target/*.jar unigest.jar

# Memoria: el plan gratuito de Render da 512 MB; con la mitad para datos (256 MB)
# UniGest usa unos 320 MB en total, medido con la demo cargada.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -Xss512k"

# Arranque rápido con CDS (Class Data Sharing): al construir la imagen se arranca la
# aplicación una vez y se guardan sus clases ya cargadas en app.jsa. Medido en local,
# el arranque baja de 8,5 s a unos 4 s. El ensayo no necesita base de datos: se detiene
# al terminar de crear el contexto y Hibernate no consulta metadatos de la conexión.
# Si el archivo no sirve, Java arranca normal sin él.
RUN java -Djarmode=tools -jar unigest.jar extract --destination app \
 && rm unigest.jar \
 && cd app \
 && java -XX:ArchiveClassesAtExit=/app/app.jsa -Xlog:cds=off -Xlog:cds+dynamic=off \
      -Dspring.context.exit=onRefresh \
      -DDB_URL=jdbc:postgresql://localhost:1/sin-base -DDB_USER=x -DDB_PASSWORD=x \
      -Dspring.flyway.enabled=false -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -jar unigest.jar \
 && test -s /app/app.jsa
USER unigest

# Dentro del contenedor hay que escuchar en todas las interfaces; Render define PORT.
ENV BIND_ADDRESS=0.0.0.0 \
    PORT=8080 \
    SESSION_COOKIE_SECURE=true
EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=/app/app.jsa", "-Xlog:cds=off", "-jar", "/app/app/unigest.jar"]
