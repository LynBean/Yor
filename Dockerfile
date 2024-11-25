# LynBot Dockerfile
FROM maven:3.8-openjdk-18 AS builder

ADD . /lynbot/
WORKDIR /lynbot

# Build LynBot
RUN mvn clean install

# Build final image using alpine (Distroless) for smaller image size
FROM alpine:3.18.0
COPY --from=builder /lynbot/target/LynBot-Snapshot-Jar-with-Dependencies.jar /lynbot/LynBot.jar

# Install useful packages
RUN apk update
RUN apk add --no-cache openjdk17

# Entrypoint of LynBot
WORKDIR /lynbot
CMD [ "/usr/bin/java", "-jar", "/lynbot/LynBot.jar" ]