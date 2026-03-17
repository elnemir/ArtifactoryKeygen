# Changelog

Все значимые изменения в проекте ArtifactoryKeygen будут документированы в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
и этот проект следует [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0-SNAPSHOT] - 2025-12-26

### Added (2025-03)
- Поддержка Artifactory 7.133.9: обновлена зависимость `artifactory-addons-manager` до 7.133.9

### Added
- Поддержка Java 25 LTS
- Логирование через SLF4J и Logback
- Тестовая инфраструктура JUnit 5
- Расширенная обработка ошибок
- Kotlin module для Jackson

### Changed
- **BREAKING**: Обновлен Kotlin с 1.8.21 до 2.1.0
- **BREAKING**: Мигрировано с Jackson 1.x на Jackson 2.18.2
- Обновлен Gradle Shadow plugin с 6.0.0 до 8.1.1
- Обновлен BouncyCastle с 1.74 до 1.79
- Обновлен commons-codec с 1.9 до 1.17.1
- Обновлен SnakeYAML с 2.0 до 2.3
- Обновлен SLF4J с 2.0.7 до 2.0.16
- Обновлен JVM toolchain с Java 11 на Java 25
- Версия проекта обновлена с 1.0-SNAPSHOT до 2.0-SNAPSHOT

### Changed (Agent Module)
- Обновлен Javassist с 3.29.2-GA до 3.30.2-GA
- Обновлен JetBrains Annotations с 24.0.0 до 26.0.1
- Добавлена поддержка Java 25 через toolchain
- Версия модуля обновлена до 2.0-SNAPSHOT

### Fixed
- Исправлена несовместимость Jackson API:
  - `JsonFactory.createJsonGenerator()` → `JsonFactory.createGenerator()`
  - `objectMapper.serializationConfig.serializationInclusion` → `objectMapper.setSerializationInclusion()`
  - Добавлен явный вызов `generator.close()`
- Обновлены import statements для Jackson 2.x

### Security
- Обновлен .gitignore для исключения ключей и секретов
- Добавлено исключение для logs/ директории
- Добавлено исключение для test-results/

### Documentation
- Создан CHANGELOG.md
- Обновлена документация зависимостей
- Добавлены комментарии для JFrog библиотек

### Technical Debt
- TODO: Извлечь artifactory-addons-manager-7.133.9.jar из официального WAR (releases.jfrog.io)
- TODO: Добавить unit тесты с покрытием >70%
- TODO: Рефакторинг структуры кода (Command Pattern)
- TODO: Миграция на de-obfuscated LicenseManager

## [1.0-SNAPSHOT] - 2023-XX-XX

### Added
- Первоначальная реализация Keygen
- Первоначальная реализация Agent
- Поддержка Artifactory 7.90.7
- RSA-4096 генерация ключей
- Подпись лицензий с SHA256withRSA
- Java Agent для runtime патчинга

### Features
- Команда `genkey` - генерация RSA ключей
- Команда `gen` - создание лицензий
- Команда `verify` - проверка лицензий
- Команда `mkconfig` - создание конфигурации агента
- Команда `obf` - обфускация строк
- Автоматическая замена публичного ключа через Agent

### Dependencies
- Kotlin 1.8.21
- Jackson 1.7.9 (deprecated)
- BouncyCastle 1.74
- Javassist 3.29.2-GA
- Java 11 target

---

## Миграционный гайд: 1.0 → 2.0

### Для пользователей

**Требования**:
- Java 25 SDK установлен
- Gradle 8.x или используйте ./gradlew wrapper

**Компиляция**:
```bash
# Скачать зависимости
./gradlew --refresh-dependencies

# Собрать проект
./gradlew clean build

# Создать fat JAR
./gradlew shadowJar
```

**Использование** (без изменений):
```bash
java -jar ArtifactoryKeygen-2.0-SNAPSHOT-all.jar gen
```

### Для разработчиков

**Breaking Changes**:

1. **Jackson API**:
   ```kotlin
   // Старый код (1.0)
   import org.codehaus.jackson.JsonFactory
   val generator = factory.createJsonGenerator(bos, JsonEncoding.UTF8)

   // Новый код (2.0)
   import com.fasterxml.jackson.core.JsonFactory
   val generator = factory.createGenerator(bos, JsonEncoding.UTF8)
   generator.close() // Обязательно закрывать
   ```

2. **Java Version**:
   - Минимальная версия: Java 25
   - Используйте `jvmToolchain(25)` в build.gradle.kts

3. **Gradle**:
   - Shadow plugin 8.1.1 требует Gradle 8.x

**Новые возможности**:

1. **Logging**:
   ```kotlin
   import org.slf4j.LoggerFactory

   private val logger = LoggerFactory.getLogger(YourClass::class.java)
   logger.info("Your message")
   ```

2. **Testing**:
   ```kotlin
   import org.junit.jupiter.api.Test

   class YourTest {
       @Test
       fun `test something`() {
           // your test
       }
   }
   ```

---

**Авторы**: Community Contributors
**Лицензия**: Anti 996 License
**Статус**: Educational Project
