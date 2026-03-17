# JVM options for Access (jfac). Required for license patch: LegacyLicenseManager is loaded in Access JVM.
# Copy this to app/access/tomcat/bin/setenv.sh in the container, or mount it at runtime.
# The same agent JAR must be loaded in both Artifactory (jfrt) and Access (jfac) — two separate processes.
CATALINA_OPTS="$CATALINA_OPTS -javaagent:/opt/jfrog/artifactory/app/access/tomcat/lib/ArtifactoryAgent.jar"
