## Optional: artifactory-addons-manager JAR

Сборка и работа Keygen (**gen**, **verify**, **mkconfig**) **не требуют** этого JAR.

Положить сюда `artifactory-addons-manager-7.133.9.jar` (извлечь из `artifactory.war` → WEB-INF/lib) нужно только если вы хотите:
- собирать проект с зависимостью на классы JFrog для команды **verifyAgent** (проверка агента с подставленным ключом);
- или использовать старый код, зависящий от этого JAR.

В текущей конфигурации корневой `build.gradle.kts` не подключает JAR из `libs/`.
