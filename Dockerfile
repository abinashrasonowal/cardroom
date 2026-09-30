# Whitejack: one image, one JVM. The React UI is built in a Node stage, bundled into the Spring
# Boot jar as static files, and served on :8080 alongside the API and the /ws WebSocket.
#
#   docker build -t whitejack .
#   docker run -p 8080:8080 -e WHITEJACK_COOKIESECRET=$(openssl rand -hex 32) whitejack

# ---- 1. UI: npm ci + vite build -------------------------------------------------------------
FROM node:22-alpine AS ui
WORKDIR /ui
# Lockfile first, so dependency installs are cached until package*.json changes.
COPY whitejack-ui/package.json whitejack-ui/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY whitejack-ui/ ./
RUN npm run build

# ---- 2. Server: Gradle builds the fat jar around the prebuilt UI ----------------------------
# The official Gradle image, not ./gradlew: the build does not depend on the wrapper jar.
FROM gradle:8.14-jdk17 AS server
WORKDIR /src
# Build scripts first, so the dependency download is cached until a *.gradle.kts changes.
COPY settings.gradle.kts build.gradle.kts ./
COPY engine-contract/build.gradle.kts engine-contract/
COPY engine-core/build.gradle.kts engine-core/
COPY games/high-card/build.gradle.kts games/high-card/
COPY games/hearts/build.gradle.kts games/hearts/
COPY games/gin-rummy/build.gradle.kts games/gin-rummy/
COPY games/poker/build.gradle.kts games/poker/
COPY whitejack-bots/build.gradle.kts whitejack-bots/
COPY whitejack-server/build.gradle.kts whitejack-server/
COPY whitejack-ui/build.gradle.kts whitejack-ui/
# Plain RUN, no BuildKit cache mount, so the file builds on the legacy builder too.
RUN gradle --no-daemon -q :whitejack-server:dependencies > /dev/null
COPY engine-contract/ engine-contract/
COPY engine-core/ engine-core/
COPY games/ games/
COPY whitejack-bots/ whitejack-bots/
COPY whitejack-server/ whitejack-server/
COPY --from=ui /ui/dist whitejack-ui/dist
# Tests run in CI and on developer machines; the image build only packages.
RUN gradle --no-daemon :whitejack-server:bootJar -PprebuiltUi -x test \
 && cp whitejack-server/build/libs/whitejack-server.jar /whitejack.jar

# ---- 3. Runtime: JRE only, non-root ---------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system whitejack && useradd --system --gid whitejack --home /app whitejack
WORKDIR /app
COPY --from=server --chown=whitejack:whitejack /whitejack.jar app.jar
USER whitejack
EXPOSE 8080
# Size the heap from the container's memory limit, not the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
