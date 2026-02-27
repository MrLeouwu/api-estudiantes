FROM eclipse-temurin:11-jre

RUN apt-get update && apt-get install -y curl gnupg && \
    curl -fL "https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz" | gzip -d > /usr/local/bin/cs && \
    chmod +x /usr/local/bin/cs && \
    cs install sbt && \
    export PATH="$PATH:/root/.local/share/coursier/bin"

ENV PATH="/root/.local/share/coursier/bin:${PATH}"

WORKDIR /app

COPY api-estudiantes/project ./project
COPY api-estudiantes/build.sbt ./build.sbt
COPY api-estudiantes/src ./src
COPY api-estudiantes/init.sql ./init.sql

RUN sbt assembly

EXPOSE 8080

CMD ["java", "-cp", "/app/target/scala-2.13/api-estudiantes-assembly-0.1.0.jar", "main.Main"]
