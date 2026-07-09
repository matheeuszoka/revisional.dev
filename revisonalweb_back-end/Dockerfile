# Build: empacota o WAR com Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache de dependências: só re-baixa quando o pom muda
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# Runtime: Tomcat 11 + Tesseract (OCR nativo exigido pelo Tess4J)
FROM tomcat:11.0-jdk17
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por \
       libtesseract-dev libleptonica-dev \
    && rm -rf /var/lib/apt/lists/*
# Caminho do tessdata no Debian/Ubuntu do container (lido por ocr.tessdata-path)
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
