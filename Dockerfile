# syntax=docker/dockerfile:1

FROM eclipse-temurin:24-jdk-noble AS build
WORKDIR /src
COPY . .
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package -pl start -am \
    && java -Djarmode=tools -jar start/target/tixora.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:24-jre-noble AS runtime
LABEL org.opencontainers.image.source="https://github.com/kitkas1412/tixora" \
      org.opencontainers.image.title="tixora"
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
