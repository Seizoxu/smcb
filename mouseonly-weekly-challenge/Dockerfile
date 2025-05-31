FROM eclipse-temurin:17-jdk

WORKDIR /backend
COPY . .

RUN ./gradlew shadowJar
RUN cp build/libs/smcb-*.jar smcb.jar

CMD ["java", "-jar", "smcb.jar"]
