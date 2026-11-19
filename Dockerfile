FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 unigest
COPY --from=build /app/target/*.jar unigest.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"

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

ENV BIND_ADDRESS=0.0.0.0 \
    PORT=8080 \
    SESSION_COOKIE_SECURE=true \
    SPRING_MAIN_LAZY_INITIALIZATION=true
EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=/app/app.jsa", "-Xlog:cds=off", "-jar", "/app/app/unigest.jar"]
