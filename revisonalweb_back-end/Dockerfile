# Build: empacota o WAR com Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache de dependências: só re-baixa quando o pom muda
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# Leptonica >= 1.83 exigida pelo lept4j 1.21 (simbolos *1); apt do Ubuntu noble para em 1.82.
# Compilada da fonte contra a mesma base do runtime (glibc compativel).
FROM tomcat:11.0-jdk17 AS leptonica-build
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential wget ca-certificates pkg-config \
       libpng-dev libjpeg-dev libtiff-dev zlib1g-dev libwebp-dev libopenjp2-7-dev libgif-dev \
    && wget -qO /tmp/leptonica.tar.gz https://github.com/DanBloomberg/leptonica/releases/download/1.85.0/leptonica-1.85.0.tar.gz \
    && tar -xzf /tmp/leptonica.tar.gz -C /tmp \
    && cd /tmp/leptonica-1.85.0 \
    && ./configure --prefix=/opt/leptonica --disable-static \
    && make -j"$(nproc)" \
    && make install

# Runtime: Tomcat 11 + Tesseract (OCR nativo exigido pelo Tess4J)
FROM tomcat:11.0-jdk17
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por \
    && rm -rf /var/lib/apt/lists/*
# JNA procura libtesseract.so/libleptonica.so sem sufixo de versao:
# - tesseract: symlink para a lib do apt (dispensa libtesseract-dev)
# - leptonica: usa a 1.85 compilada acima (a liblept.so.5 do apt segue para uso interno do tesseract)
COPY --from=leptonica-build /opt/leptonica/lib/libleptonica.so* /usr/local/lib/
RUN ln -sf /usr/lib/x86_64-linux-gnu/libtesseract.so.5 /usr/lib/x86_64-linux-gnu/libtesseract.so \
    && ldconfig
# Caminho do tessdata no Debian/Ubuntu do container (lido por ocr.tessdata-path)
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
