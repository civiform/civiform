FROM openjdk:slim
RUN apt-get update
RUN apt-get install -y wget
RUN wget https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar -O /fmt.jar
RUN apt-get install -y npm
RUN npm install -g typescript typescript-formatter 
VOLUME /code
CMD ["sh", "-c", "java -jar /fmt.jar --replace $(find /code -name '*.java'); tsfmt -r $(find /code -name '*.ts' -not -path 'node_modules' -not -path 'node-modules')"]
