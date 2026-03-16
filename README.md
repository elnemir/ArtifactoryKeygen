# ArtifactoryKeygen — генератор ключей для JFrog Artifactory

![Version](https://img.shields.io/badge/version-2.0--SNAPSHOT-blue)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.x-02303A?logo=gradle&logoColor=white)
![License](https://img.shields.io/badge/license-Educational-orange)
[![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://996.icu)

> Образовательный проект для исследования механизмов лицензирования JFrog Artifactory

---

## Содержание

- [Описание проекта](#описание-проекта)
- [Важные предупреждения](#важные-предупреждения)
- [Архитектура системы](#архитектура-системы)
- [Требования](#требования)
- [Структура проекта](#структура-проекта)
- [Установка и сборка](#установка-и-сборка)
- [Использование](#использование)
- [Конфигурация](#конфигурация)
- [Технические детали](#технические-детали)
- [Troubleshooting](#troubleshooting)
- [Лицензия](#лицензия)

---

## Описание проекта

**ArtifactoryKeygen** — образовательный проект по информационной безопасности и реверс-инжинирингу. Демонстрирует:

- Механизмы лицензирования enterprise-систем (JFrog Artifactory)
- Криптографию (RSA 4096, X.509)
- Техники reverse engineering Java-приложений
- Java Agent для runtime-модификации классов и bytecode instrumentation

### Компоненты

| Компонент           | Назначение                    | Технологии              |
|---------------------|-------------------------------|-------------------------|
| **ArtifactoryKeygen** | Генерация лицензионных ключей | Kotlin, BouncyCastle, Jackson |
| **ArtifactoryAgent**  | Java-агент для runtime-патчинга | Java 25, Javassist     |

**Тестировано на:** Artifactory 7.133.9 (целевая версия), 7.104.6, 7.9.2, 7.125.10.

---

## Важные предупреждения

**ВНИМАНИЕ. НЕ ИСПОЛЬЗУЙТЕ ПРОЕКТ ДЛЯ:**

- Незаконного обхода лицензий JFrog Artifactory
- Коммерческого использования без лицензии JFrog
- Нарушения законодательства

**ВСЕ ПОСЛЕДСТВИЯ ИСПОЛЬЗОВАНИЯ НЕСЁТ ПОЛЬЗОВАТЕЛЬ.**

Проект создан **исключительно в образовательных целях**: академические исследования, изучение защиты ПО, практика reverse engineering.

[![LICENSE](https://img.shields.io/badge/license-Anti%20996-blue.svg)](https://github.com/996icu/996.ICU/blob/master/LICENSE)

---

## Архитектура системы

```
Keygen (gen/genkey/verify/mkconfig) → RSA 4096, подпись лицензий
Agent (-javaagent) → патчинг org.jfrog.license.a.a и org.jfrog.license.api.a → подмена публичного ключа
Artifactory → проверка лицензии с подставленным ключом
```

---

## Требования

- **Сборка:** Java JDK 25+, Gradle 8.x (Gradle Wrapper в проекте)
- **Запуск:** JVM 11+, Artifactory 7.x
- **Библиотека:** `artifactory-addons-manager-7.133.9.jar` в `libs/` (извлекается из WAR Artifactory 7.133.9)

---

## Структура проекта

```
ArtifactoryKeygen/
├── src/main/kotlin/icu/lama/artifactory/keygen/   # Генератор (Keygen.kt)
├── src/main/java/org/jfrog/                       # Декомпилированные классы JFrog
├── ArtifactoryAgent/                              # Java-агент
├── libs/                                          # artifactory-addons-manager-7.133.9.jar
├── deployment/                                    # Containerfile, podman-compose
├── docs/                                          # Документация и отчёты
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Установка и сборка

### 1. Клонирование и подготовка libs

```bash
git clone https://github.com/dantte-lp/ArtifactoryKeygen.git
cd ArtifactoryKeygen
```

Положите в `libs/` файл `artifactory-addons-manager-7.133.9.jar`, извлечённый из `artifactory.war` (WEB-INF/lib) дистрибутива Artifactory 7.133.9.

### 2. Сборка

```bash
./gradlew :shadowJar
./gradlew :ArtifactoryAgent:shadowJar
# или
./gradlew clean build shadowJar
```

Артефакты: `build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar`, `ArtifactoryAgent/build/libs/ArtifactoryAgent-2.0-SNAPSHOT-all.jar`.

---

## Использование

### ArtifactoryKeygen

| Команда | Описание |
|--------|----------|
| `gen` | Сгенерировать лицензию (интерактивно, можно несколько продуктов) |
| `pub` | Показать текущий публичный ключ (RSA) |
| `pri` | Показать текущий приватный ключ |
| `genkey` | Сгенерировать новую пару ключей (RSA 4096) |
| `verify <файл\|строка>` | Проверить лицензию текущим публичным ключом |
| `obf <текст>` | Обфусцировать строку (ObfuscatedString JFrog) |
| `mkconfig` | Создать конфигурацию для ArtifactoryAgent (HEX XML) |
| `verifyAgent` | Проверка Agent при подключении к процессу |
| `help` | Справка по командам |

**Примеры:**

```bash
java -jar build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar gen
java -jar build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar verify license.json
java -jar build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar mkconfig
```

### ArtifactoryAgent

1. Скопировать JAR в каталог Tomcat:  
   `cp ArtifactoryAgent/build/libs/ArtifactoryAgent-2.0-SNAPSHOT-all.jar /opt/jfrog/artifactory/app/artifactory/tomcat/lib/`

2. В `setenv.sh` (или аналог) добавить:
   ```bash
   CATALINA_OPTS="$CATALINA_OPTS -javaagent:/path/to/ArtifactoryAgent-2.0-SNAPSHOT-all.jar"
   ```
   При необходимости передать конфиг с публичным ключом:  
   `-javaagent:/path/to/agent.jar=<HEX из mkconfig>`

3. Перезапустить Artifactory.

В логах должны появиться сообщения о загрузке агента и патчинге классов.

---

## Конфигурация

### Конфиг агента (mkconfig)

Команда `mkconfig` выводит HEX-строку UTF-8 XML вида `<config><publicKey>...</publicKey></config>`. Эту строку передают в аргументе `-javaagent`; агент парсит её и подменяет публичный ключ для проверки лицензий.

---

## Технические детали

- **Keygen:** Kotlin 2.1.20, BouncyCastle 1.79, Jackson 2.18.2, JVM 23+
- **Agent:** Java 25, Javassist, патчинг `org.jfrog.license.a.a` (toString + поле `b`) и `org.jfrog.license.api.a` (статичные поля c/d)
- **Криптография:** RSA 4096, SHA256withRSA, X.509

---

## Troubleshooting

- **Библиотека не найдена:** убедитесь, что в `libs/` лежит `artifactory-addons-manager-7.133.9.jar`, версия в `build.gradle.kts` совпадает; при необходимости `./gradlew --stop`.
- **Агент не загружается:** проверьте путь к JAR в `setenv.sh`, права доступа, версию Java (11+).
- **Лицензия не принимается:** проверьте, что агент загружен, публичный ключ в конфиге совпадает с ключом, которым подписана лицензия; используйте `verify` для проверки.

### Агент ломает старт Artifactory

Если с включённым агентом (`-javaagent:...`) Artifactory не стартует (ошибки при загрузке контекста, сервисов и т.п.):

1. **Временно отключите патчинг** (агент всё равно загрузится, но не будет менять классы):
   ```bash
   CATALINA_OPTS="$CATALINA_OPTS -Dartifactory.agent.disabled=true"
   ```
   Если после этого Artifactory стартует — проблема в логике патчей или в окружении (classloader, версия Artifactory).

2. **Чтобы починить агент, пришлите:**
   - **Полный лог запуска** с момента старта JVM до падения (все строки, где есть `Artifactory Agent ::` и полный stack trace ошибки).
   - **Версию Artifactory** (например 7.133.9) и способ запуска (встроенный Tomcat из дистрибутива или свой Tomcat).
   - **Подтверждение:** без `-javaagent` Artifactory стартует нормально.

---

## Лицензия

Проект распространяется под [Anti-996 License](https://github.com/996icu/996.ICU/blob/master/LICENSE). Использование только в образовательных целях. Не аффилирован с JFrog Ltd.

---

**Версия:** 2.0-SNAPSHOT | **Статус:** Образовательный проект
