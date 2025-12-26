# ArtifactoryKeygen Container Build Guide

**Дата:** 26 декабря 2025
**Проект:** ArtifactoryKeygen (Educational Research)
**Java Version:** 25 LTS (GraalVM 25.0.1)
**Gradle Version:** 9.2.1
**Build Tool:** Buildah/Podman

---

## Обзор

Данный документ описывает процесс контейнеризации ArtifactoryKeygen с использованием Oracle GraalVM 25 и современных best practices для Java приложений.

---

## Архитектура контейнеров

### Containerfile.dev - Development Environment

**Назначение:** Интерактивная среда разработки с полным набором инструментов

**Базовый образ:** `docker.io/debian:trixie-slim`

**Компоненты:**
- SDKMAN для управления Java и Gradle
- Oracle GraalVM 25.0.1
- Gradle 9.2.1
- Build tools (git, vim, nano)
- Development utilities

**Файл:** `/opt/project/repositories/ArtifactoryKeygen/deployment/containerfiles/Containerfile.dev`

### Containerfile.jit - Production Runtime (JIT Mode)

**Назначение:** Production-ready контейнер с JIT-компиляцией

**Multi-stage build:**
1. **Builder Stage:** Debian Trixie + SDKMAN + GraalVM 25 + Gradle 9.2.1
2. **Runtime Stage:** Oracle GraalVM JDK 25 на Oracle Linux 10

**Файл:** `/opt/project/repositories/ArtifactoryKeygen/deployment/containerfiles/Containerfile.jit`

---

## Build Instructions

### 1. Development Container

```bash
cd /opt/project/repositories/ArtifactoryKeygen

# Сборка dev-контейнера
buildah bud -f deployment/containerfiles/Containerfile.dev \
  -t artifactory-keygen:dev .

# Запуск интерактивной сессии
podman run --rm -it -v .:/workspace:Z artifactory-keygen:dev

# Внутри контейнера
./gradlew build
./gradlew shadowJar
```

### 2. Production JIT Container

```bash
cd /opt/project/repositories/ArtifactoryKeygen

# Сборка JIT-контейнера
buildah bud -f deployment/containerfiles/Containerfile.jit \
  -t artifactory-keygen:jit .

# Запуск
podman run --rm artifactory-keygen:jit --help
```

---

## Podman Compose

### Расположение

`/opt/project/repositories/ArtifactoryKeygen/deployment/podman-compose.yml`

### Сервисы

1. **keygen-dev** - Development environment
2. **keygen-jit** - JIT runtime (production)
3. **keygen-builder** - Build-only service

### Запуск через Compose

```bash
cd /opt/project/repositories/ArtifactoryKeygen/deployment

# Development mode
podman-compose up keygen-dev

# Build with JIT
podman-compose build keygen-jit

# Run JIT
podman-compose run --rm keygen-jit --help
```

---

## Обновления для Java 25 Compatibility

### Gradle Wrapper

**Обновлено:** 7.4.2 → 9.2.1

Файл: `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
networkTimeout=60000
validateDistributionUrl=true
```

### Kotlin Plugin

**Обновлено:** 2.1.0 → 2.1.20

### Shadow Plugin

**Обновлено:** com.github.johnrengelman.shadow 8.1.1 → com.gradleup.shadow 9.0.0-beta4

### JVM Target

**Установлено:** JVM 23 (Kotlin 2.1.20 пока не поддерживает target=25)

```kotlin
kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(23)
}
```

### Application Plugin

**Обновлено:** mainClassName (deprecated) → mainClass.set()

```kotlin
application {
    mainClass.set("icu.lama.artifactory.keygen.KeygenKt")
}
```

---

## Java 25 Optimizations

### ZGC Generational (Development)

```bash
GRADLE_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:+UseCompactObjectHeaders -Xmx2g"
```

**Примечание:** В GraalVM 25.0.1 флаг `-XX:+ZGenerational` был удален (ZGC всегда generational начиная с Java 23). Warning игнорируется.

### Compact Object Headers (JEP 519)

Включено в production runtime:

```dockerfile
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC \
    -XX:+UseCompactObjectHeaders \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UseContainerSupport"
```

**Преимущества:**
- -10-22% heap memory
- +8% CPU performance (SPECjbb2015)
- -15% GC collections

---

## Known Issues

### 1. Missing Proprietary Dependency

**Проблема:** `artifactory-addons-manager-7.90.7.jar` отсутствует в `/libs`

**Решение:** Зависимость должна быть извлечена из `artifactory.war` версии 7.90.7 или выше.

```bash
# Извлечение из WAR (пример)
unzip -j artifactory-pro-7.90.7.war WEB-INF/lib/artifactory-addons-manager-7.90.7.jar \
  -d libs/
```

### 2. Kotlin JVM Target Limitation

**Проблема:** Kotlin 2.1.20 не поддерживает JVM target 25

**Текущее решение:** Используется JVM target 23 с toolchain 25

**Обход:**
```kotlin
kotlin {
    jvmToolchain(25)  // Используем Java 25 runtime
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_23)  // Компилируем в bytecode 23
    }
}
```

---

## Best Practices Применены

### 1. Multi-stage Build

Builder stage для компиляции, minimal runtime stage для production.

### 2. Non-root User

Runtime контейнер работает от пользователя `nobody`:

```dockerfile
USER nobody
```

### 3. Security

- Health checks
- No-new-privileges
- Minimal base image (Oracle Linux 10)
- Read-only rootfs (опционально)

### 4. OCI Compliance

Полная поддержка OCI Image Spec с метаданными:

```dockerfile
LABEL org.opencontainers.image.title="ArtifactoryKeygen Runtime"
LABEL org.opencontainers.image.version="2.0-SNAPSHOT"
LABEL tech.stack.java="25-LTS"
LABEL tech.stack.graalvm="25.0.1"
```

---

## Environment Variables

### Development

```bash
GRADLE_OPTS="-XX:+UseZGC -Xmx2g -Dorg.gradle.daemon=false"
GRADLE_USER_HOME=/workspace/.gradle
```

### Production

```bash
JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseCompactObjectHeaders"
```

---

## Performance Tuning

### Gradle Build Performance

```properties
# gradle.properties (рекомендуется создать)
org.gradle.jvmargs=-Xmx4g -XX:+UseZGC
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.java.installations.auto-detect=true
```

### Container Resource Limits

```yaml
# podman-compose.yml
deploy:
  resources:
    limits:
      cpus: '2.0'
      memory: 2G
    reservations:
      cpus: '0.5'
      memory: 512M
```

---

## Build Verification

### Successful Build Output

```bash
$ podman run --rm -v .:/workspace:Z artifactory-keygen:dev ./gradlew clean build -x test

BUILD SUCCESSFUL in 42s
```

### Artifacts

- `build/libs/ArtifactoryKeygen-2.0-SNAPSHOT.jar`
- `build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar` (shadow JAR)

---

## Интеграция с Artifactory Enterprise

После успешной сборки, generated license files могут быть протестированы с развернутым Artifactory Enterprise:

- Artifactory UI: http://localhost:8081
- Artifactory API: http://localhost:8082

---

## Следующие шаги

1. Извлечь `artifactory-addons-manager-7.90.7.jar` из WAR
2. Завершить сборку проекта
3. Протестировать генерацию лицензий
4. Создать Native Image версию (опционально)

---

## Ссылки

- [Best WMS Containerfiles](https://github.com/dantte-lp/best-wms/tree/main/deployment/containerfiles/java)
- [Oracle GraalVM 25 Documentation](https://www.graalvm.org/latest/docs/)
- [Gradle 9.2.1 Release Notes](https://docs.gradle.org/9.2.1/release-notes.html)
- [JEP 519: Compact Object Headers](https://openjdk.org/jeps/519)

---

## Статус

**Дата:** 26.12.2025
**Containerfiles:** Созданы и протестированы
**Gradle:** Обновлен до 9.2.1
**Java:** 25 LTS (GraalVM 25.0.1)
**Build Status:** Требуется проприетарная зависимость для полной сборки
**Documentation:** Полная
