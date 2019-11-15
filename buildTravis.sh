#!/bin/bash
# This script will build the project.

export JAVA_OPTS="-Xmx1024M -XX:MaxPermSize=512M -XX:ReservedCodeCacheSize=512M"
export GRADLE_OPTS="-Dorg.gradle.daemon=true"

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  if [ "$TRAVIS_TAG" == "" ]; then
    echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']'
    ./gradlew test build -s
  else
    echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] Tag ['$TRAVIS_TAG']'
    ./gradlew test build :bintrayUpload
  fi
else
  if [ "$TRAVIS_TAG" == "" ]; then
    echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag'
    ./gradlew test build -s
  else
    echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and Tag ['$TRAVIS_TAG']'
    ./gradlew test build :bintrayUpload -s
  fi
fi
