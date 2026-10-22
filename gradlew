#!/bin/sh
# Gradle start up script for POSIX generated for Dawa & Hayah Android Native
set -e
APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -x "$JAVA_HOME/bin/java" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi
if [ ! -f "$WRAPPER_JAR" ]; then
    if command -v gradle >/dev/null 2>&1; then
        exec gradle "$@"
    else
        echo "Error: gradle-wrapper.jar not found and system 'gradle' is not installed. In GitHub Actions, gradle/actions/setup-gradle@v4 automatically provisions Gradle." >&2
        exit 1
    fi
fi
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
