# Artifactory (jfrt) JVM — agent for license key + parser patches
CATALINA_OPTS="-javaagent:/opt/jfrog/artifactory/app/artifactory/ArtifactoryAgent.jar"
CATALINA_OPTS="$CATALINA_OPTS -Djf.product.home=/opt/jfrog/artifactory"
# Note: Access (jfac) runs in a separate JVM and needs the same agent — use setenv-access.sh there.