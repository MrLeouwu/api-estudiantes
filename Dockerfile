FROM eclipse-temurin:11-jre

WORKDIR /app

COPY target/scala-2.13/*assembly*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]