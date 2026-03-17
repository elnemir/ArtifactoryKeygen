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
Agent (-javaagent) → патчинг org.jfrog.license (a.a, api.a — подмена ключа; legacy.LegacyLicenseManager — возврат null при ошибке decrypt → переход на формат с подписью)
Artifactory → проверка лицензии с подставленным ключом
```

---

## Требования

- **Сборка:** Java JDK 25+, Gradle 8.x (Gradle Wrapper в проекте)
- **Запуск:** JVM 11+, Artifactory 7.x
- **Библиотека:** для сборки и работы Keygen (gen, verify, mkconfig) **JAR не нужен**. Папка `libs/` опциональна — только если нужна команда `verifyAgent` (проверка агента с подставленным ключом при наличии artifactory-addons-manager в classpath).

---

## Структура проекта

```
ArtifactoryKeygen/
├── src/main/kotlin/icu/lama/artifactory/keygen/   # Генератор (Keygen.kt, модели лицензии, подпись/верификация)
├── src/main/java/org/jfrog/                       # ObfuscatedString, BCProviderFactory (без зависимости от JAR)
├── ArtifactoryAgent/                              # Java-агент
├── libs/                                          # опционально: artifactory-addons-manager для verifyAgent
├── deployment/                                    # Containerfile, podman-compose
├── docs/                                          # Документация и отчёты
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Установка и сборка

### 1. Клонирование

```bash
git clone https://github.com/dantte-lp/ArtifactoryKeygen.git
cd ArtifactoryKeygen
```

Для сборки и работы Keygen (gen, verify, mkconfig) дополнительные JAR не требуются.

### 2. Сборка

```bash
./gradlew :shadowJar
# или
./gradlew clean build shadowJar
```

Артефакт: `build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar` (содержит **и Keygen, и Agent**).

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

Один и тот же файл `build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar` можно использовать как **CLI** (`java -jar ...`) и как **Java Agent** (`-javaagent:...`).

Платформа JFrog в Docker/standalone запускает **два отдельных процесса (две JVM)**:
- **Artifactory (jfrt)** — томкат `app/artifactory/tomcat`
- **Access (jfac)** — томкат `app/access/tomcat`

Проверка лицензии в формате с подписью и класс `LegacyLicenseManager` используются в **Access (jfac)**. Поэтому **агент нужно подключать к обеим JVM**: и к Artifactory, и к Access. Если агент указан только у Artifactory, в логах будет «Patching class» только при загрузке jfrt, а ошибка «Invalid license» будет идти из jfac, где классы не патчатся.

1. Скопировать JAR агента в оба каталога (или в общее место и указать один путь):
   ```bash
   cp build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar /opt/jfrog/artifactory/app/artifactory/tomcat/lib/ArtifactoryAgent.jar
   cp build/libs/ArtifactoryKeygen-2.0-SNAPSHOT-all.jar /opt/jfrog/artifactory/app/access/tomcat/lib/ArtifactoryAgent.jar
   ```

2. **Artifactory (jfrt):** в `app/artifactory/tomcat/bin/setenv.sh` (или аналог) добавить:
   ```bash
   CATALINA_OPTS="$CATALINA_OPTS -javaagent:/opt/jfrog/artifactory/app/artifactory/tomcat/lib/ArtifactoryAgent.jar"
   ```
   При необходимости передать конфиг: `-javaagent:.../agent.jar=<HEX из mkconfig>`

3. **Access (jfac):** в `app/access/tomcat/bin/setenv.sh` добавить ту же опцию для своего томката:
   ```bash
   CATALINA_OPTS="$CATALINA_OPTS -javaagent:/opt/jfrog/artifactory/app/access/tomcat/lib/ArtifactoryAgent.jar"
   ```
   В Docker-образе файл может генерироваться из шаблона; тогда нужно либо смонтировать свой `setenv.sh` для Access, либо задать переменную окружения, которую скрипт запуска Access подхватывает (например `JF_ACCESS_OPTS` или аналог — зависит от версии образа). В репозитории есть пример `JfrogDockerfile/setenv-access.sh` — его можно копировать в образ в `app/access/tomcat/bin/setenv.sh` или монтировать при запуске контейнера.

4. Перезапустить платформу.

В логах при старте **обоих** процессов должны появиться блоки «Artifactory Agent :: Is now LOADED!» и «Patching class: org.jfrog.license...» (в т.ч. `org.jfrog.license.legacy.LegacyLicenseManager`). Если только один такой блок — агент подключён только к одной JVM.

---

## Конфигурация

### Конфиг агента (mkconfig)

Команда `mkconfig` выводит HEX-строку UTF-8 XML вида `<config><publicKey>...</publicKey></config>`. Эту строку передают в аргументе `-javaagent`; агент парсит её и подменяет публичный ключ для проверки лицензий.

---

## Технические детали

- **Keygen:** Kotlin 2.1.20, BouncyCastle 1.79, Jackson 2.18.2, JVM 23+
- **Agent:** Java 25, Javassist. Патчи: `org.jfrog.license.a.a` (toString + поле `b`), `org.jfrog.license.api.a` (статичные c/d), `org.jfrog.license.legacy.LegacyLicenseManager` (при ошибке decrypt возврат null → платформа пробует формат с подписью)
- **Криптография:** RSA 4096, SHA256withRSA, X.509

---

## Troubleshooting

- **Библиотека не найдена:** для Keygen (gen/verify) JAR в `libs/` не нужен. Если ошибка касается другого модуля или `verifyAgent`, см. раздел про агент.
- **Агент не загружается:** проверьте путь к JAR в `setenv.sh`, права доступа, версию Java (11+).
- **Лицензия не принимается:** проверьте, что агент загружен, публичный ключ в конфиге совпадает с ключом, которым подписана лицензия; используйте `verify` для проверки.

### «Invalid license» / «Failed to decrypt license: last block incomplete in decryption»

Ошибка возникает в сервисе **Access (jfac)**: платформа сначала пытается загрузить лицензию как **legacy** (зашифрованный формат) и вызывает `LegacyLicenseManager.decrypt`. Keygen выдаёт лицензию **нового формата** (base64 JSON с подписью), поэтому decrypt падает.

**Что сделано в агенте:** добавлен патч `LegacyLicenseManager`: при исключении в `load()` метод возвращает `null` вместо выброса. Тогда `LicenseManager.loadLicense` может перейти к загрузке лицензии в формате с подписью (который проверяется уже с подставленным публичным ключом).

**Почему патчинг «не срабатывает» в логах:** в типичном запуске **Artifactory (jfrt)** и **Access (jfac)** — это два разных процесса (два PID). Агент подключается только к той JVM, в которой указан `-javaagent`. Если вы добавили агент только в `app/artifactory/.../setenv.sh`, то патчатся только классы jfrt; лицензию же проверяет **jfac**, и там агента нет → «Invalid license». Решение: добавить `-javaagent` также в опции JVM **Access** (см. раздел ArtifactoryAgent выше).

**Что проверить:**
1. Для Keygen JAR в `libs/` не требуется. Для агента этот JAR не нужен в classpath при запуске — агент патчит классы уже в JVM.
2. Пересоберите агент: `./gradlew :ArtifactoryAgent:shadowJar`. Подключите один и тот же JAR к **обоим** процессам: Artifactory и Access.
3. В логах при старте должны быть **два** набора сообщений «Artifactory Agent :: Is now LOADED!» и «Patching class: ...» (один раз при старте jfrt, один раз при старте jfac). В каждом наборе должна быть строка `Patching class: org.jfrog.license.legacy.LegacyLicenseManager`. Если её нет вообще — агент не подключён к процессу Access (jfac).
4. Вставляйте лицензию из Keygen **одной строкой base64**, без лишних пробелов и переносов.

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
