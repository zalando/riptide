#!/bin/sh -e

versions="\
4.1.0.RELEASE \
4.2.0.RELEASE \
4.3.0.RELEASE"

for version in ${versions}; do
    logfile=$(mktemp)
    echo ${version}: ${logfile}
    ./mvnw clean test-compile -D spring.version=${version} -l ${logfile}
done
