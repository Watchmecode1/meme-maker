FROM openjdk:21
ARG JAR_FILE=target/meme-maker-1.0.0.jar
COPY ${JAR_FILE} meme-maker.jar
ENTRYPOINT ["java", "-jar", "/meme-maker.jar"]
EXPOSE 8080