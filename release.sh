#!/bin/sh -ex

: ${1?"Usage: $0 <[pre]major|[pre]minor|[pre]patch|prerelease>"}

./mvnw scm:check-local-modification

current=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version --non-recursive | grep -v INFO || echo 0.0.0)
release=$(semver ${current} -i $1 --preid RC)
next=$(semver ${release} -i patch)

git checkout -b release/${release}

./mvnw versions:set -D newVersion=${release}
git commit -S -am "Release ${release}"
./mvnw clean deploy scm:tag -P release -D tag=${release} -D pushChanges=false -D skipTests -D dependency-check.skip

./mvnw versions:set -D newVersion=${next}-SNAPSHOT
git commit -S -am "Development ${next}-SNAPSHOT"

git push --set-upstream origin release/${release}
git push --tags

git checkout main
git branch -D release/${release}
