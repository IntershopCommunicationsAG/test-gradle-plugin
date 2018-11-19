#!/bin/bash
# This script will build the project.

echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']'
./gradlew test build -s
