#!/bin/sh
# Maven wrapper script (generated)
# Usage: ./mvnw [goals]

set -e

if [ -z "$JAVA_HOME" ]; then
  JAVACMD=java
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

JAR="$( cd "$( dirname "$0" )"; pwd )/.mvn/wrapper/maven-wrapper.jar"

if [ ! -f "$JAR" ]; then
  # Download wrapper jar on first run
  WRAPPER_URL=$(grep wrapperUrl .mvn/wrapper/maven-wrapper.properties | cut -d= -f2)
  echo "Downloading Maven wrapper..."
  curl -fsSL -o "$JAR" "$WRAPPER_URL" 2>/dev/null || wget -q -O "$JAR" "$WRAPPER_URL"
fi

exec "$JAVACMD" -jar "$JAR" "$@"
