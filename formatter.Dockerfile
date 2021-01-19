FROM openjdk:slim
RUN apt-get update
RUN apt-get install -y wget
RUN wget https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar -O /fmt.jar
VOLUME /code
CMD ["sh", "-c", "java -jar /fmt.jar --replace $(find /code -name '*.java')"]
