#!/bin/bash
# This script will build the project.

if [ "$TRAVIS_TAG" == "" ]; then
    echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']'
    ./gradlew test build -s
else
    echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and Tag ['$TRAVIS_TAG']'
    ./gradlew test build :bintrayUpload -s
fi
