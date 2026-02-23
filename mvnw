#!/bin/sh
# Maven Wrapper script for Unix
# Licensed to the Apache Software Foundation (ASF)

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(dirname "$0")}"
exec "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/mvnw" "$@"
