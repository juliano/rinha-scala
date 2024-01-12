FROM sbtscala/scala-sbt:eclipse-temurin-jammy-20.0.2_9_1.9.6_3.3.1 AS builder
WORKDIR /app
ADD ./build.sbt build.sbt
ADD ./project/plugins.sbt project/plugins.sbt
ADD ./src src/

RUN sbt assembly

FROM amazoncorretto:20-alpine3.18
COPY --from=builder /app/target/scala-3.3.1/rinha.jar rinha.jar
CMD "java" "-jar" "rinha.jar"