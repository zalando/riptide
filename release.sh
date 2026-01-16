#!/bin/sh -ex

: ${1?"Usage: $0 <[pre]major|[pre]minor|[pre]patch|prerelease>"}

./mvnw scm:check-local-modification

current=$(./mvnw org.apache.maven.plugins:maven-help-plugin:3.4.1:evaluate -Dexpression=project.version --non-recursive | grep -v INFO || echo 0.0.0)
# Strip -SNAPSHOT suffix before calculating release version
current_clean=$(echo "${current}" | sed 's/-SNAPSHOT$//')

# Calculate release version
# Special case: if command is 'prerelease' and we're already on a prerelease version,
# just use it as-is (the -SNAPSHOT was indicating development towards this release)
if [ "$1" = "prerelease" ] && echo "${current_clean}" | grep -q -- '-RC\.'; then
    release="${current_clean}"
else
    # Normal increment
    release=$(semver ${current_clean} -i $1 --preid RC)
fi

# Calculate next development version
if echo "${release}" | grep -q -- '-'; then
    # Has prerelease identifier, increment it for next version
    next=$(semver ${release} -i prerelease --preid RC)
else
    # No prerelease, increment patch for next development cycle
    next=$(semver ${release} -i patch)
fi

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
