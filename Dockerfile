FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 创建上传和临时目录
RUN mkdir -p /app/uploads /app/temp /app/logs

# 设置目录权限
RUN chmod -R 777 /app/uploads /app/temp /app/logs

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"] 