#!/bin/bash
# This script will build the project.

echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']'
./gradlew test build -DGRADLETEST_VERSION="4.4,4.9,5.0-rc-3" -s
