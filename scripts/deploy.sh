#!/usr/bin/env bash

set -e

script_dir=$(dirname $0)

mvn deploy -DciBuildNumber=${TRAVIS_BUILD_NUMBER} && ${script_dir}/deploy-github.sh
