FROM alpine:3.14

RUN apk update \
&& apk upgrade \
&& apk add --no-cache openjdk11 procps bash tzdata curl

ARG JAR_FILE
COPY ${JAR_FILE} app.jar

CMD ["java", "-Dspring.profiles.active=local", "-jar", "/app.jar"]
#CMD ["/bin/bash"]