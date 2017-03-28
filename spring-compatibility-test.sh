#!/bin/sh -e

grep -oP "SPRING_VERSION=\K(.+)" .travis.yml | tac | while read version; do
    logfile=$(mktemp)
    echo ${version}: ${logfile}
    ./mvnw clean test-compile -D spring.version=${version} -l ${logfile}
done