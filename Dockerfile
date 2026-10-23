# Imagen de un microservicio. Se construye desde la raíz del repositorio indicando cuál:
#   docker build --build-arg SERVICE=auth-service -t viadeep/auth-service .
# Servicios: auth-service, incident-service, evidence-service, notification-service.

FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /src
# Primero solo los pom.xml, para que Docker reutilice las dependencias descargadas si el código cambia.
COPY pom.xml .
COPY services/auth-service/pom.xml services/auth-service/
COPY services/incident-service/pom.xml services/incident-service/
COPY services/evidence-service/pom.xml services/evidence-service/
COPY services/notification-service/pom.xml services/notification-service/
RUN mvn -B -q -pl services/${SERVICE} -am dependency:go-offline
COPY services services
RUN mvn -B -q -pl services/${SERVICE} -am package -DskipTests \
    && cp services/${SERVICE}/target/${SERVICE}-*.jar /app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 viadeep
COPY --from=build /app.jar /app/app.jar
USER viadeep
# Cada servicio lee su puerto de PORT (8081 auth, 8082 incidencias, 8083 evidencias, 8084 notificaciones).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
