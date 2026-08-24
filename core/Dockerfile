FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY core/pom.xml core/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -B dependency:go-offline

COPY core/ core/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -B -pl core -am clean package -DskipTests -o

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/core/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]