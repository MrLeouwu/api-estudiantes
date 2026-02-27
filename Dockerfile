FROM eclipse-temurin:11-jre

RUN apt-get update && apt-get install -y curl gnupg && \
    curl -fL "https://github.com/coursier/coursier/releases/latest/download/cs-x86_64-pc-linux.gz" | gzip -d > /usr/local/bin/cs && \
    chmod +x /usr/local/bin/cs && \
    cs install sbt && \
    export PATH="/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/root/.local/share/coursier/bin"

ENV PATH="/root/.local/share/coursier/bin:${PATH}"

WORKDIR /app

COPY project ./project
COPY build.sbt ./
COPY src ./src
COPY init.sql .

RUN sbt assembly

EXPOSE 8080

CMD ["java", "-cp", "/app/target/scala-2.13/api-estudiantes-assembly-0.1.0.jar", "main.Main"]
