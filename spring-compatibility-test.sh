#!/bin/sh -e

versions="\
4.1.0.RELEASE \
4.1.1.RELEASE \
4.1.2.RELEASE \
4.1.3.RELEASE \
4.1.4.RELEASE \
4.1.5.RELEASE \
4.1.6.RELEASE \
4.1.7.RELEASE \
4.1.8.RELEASE \
4.1.9.RELEASE \
4.2.0.RELEASE \
4.2.1.RELEASE \
4.2.2.RELEASE \
4.2.3.RELEASE \
4.2.4.RELEASE \
4.2.5.RELEASE \
4.2.6.RELEASE \
4.2.7.RELEASE \
4.2.8.RELEASE \
4.2.9.RELEASE \
4.3.0.RELEASE \
4.3.1.RELEASE \
4.3.2.RELEASE \
4.3.3.RELEASE \
4.3.4.RELEASE \
4.3.5.RELEASE \
4.3.6.RELEASE \
4.3.7.RELEASE"

for version in ${versions}; do
    logfile=$(mktemp)
    echo ${version}: ${logfile}
    ./mvnw clean test-compile -D spring.version=${version} -l ${logfile}
done
