#!/bin/bash

set -e

script_dir=$(dirname $0)

mvn install deploy -DciBuildNumber=${TRAVIS_BUILD_NUMBER}
"${script_dir}/deploy-github.sh"