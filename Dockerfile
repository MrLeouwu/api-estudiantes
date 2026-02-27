FROM eclipse-temurin:17

# Install sbt
RUN apt-get update && \
    apt-get install -y wget && \
    wget https://github.com/sbt/sbt/releases/download/v1.9.9/sbt-1.9.9.tgz && \
    tar xzf sbt-1.9.9.tgz -C /opt && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /app

COPY project/ ./project/
COPY build.sbt ./
COPY src/ ./src/
COPY init.sql ./

RUN sbt assembly

EXPOSE 8080

CMD ["java", "-cp", "target/scala-2.13/api-estudiantes-assembly-0.1.0.jar", "main.Main"]
