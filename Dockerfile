FROM clojure:temurin-21-tools-deps-jammy AS clj-build
WORKDIR /workspace

COPY deps.edn /workspace/
RUN clojure -P

COPY . .

RUN clojure -T:build uber :entrypoint clojure-finance-auth.core

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system clojure && adduser --system --ingroup clojure clojure
RUN apt-get update && apt-get install -y dumb-init && rm -rf /var/lib/apt/lists/*

COPY --from=clj-build --chown=clojure:clojure /workspace/target/server.jar ./target/server.jar

USER clojure
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["java", "-jar", "target/server.jar"]