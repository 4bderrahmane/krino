FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
LABEL authors="4bderrahmane"

RUN useradd -m springuser || true
USER springuser
WORKDIR /home/springuser

ARG JAR_FILE=/workspace/target/*.jar
COPY --from=build ${JAR_FILE} ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "./app.jar"]