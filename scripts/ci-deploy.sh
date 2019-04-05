#!/usr/bin/env bash

set -ev

script_dir=$(dirname $0)

./gradlew artifactoryPublish
${script_dir}/ci-github-publish.sh
