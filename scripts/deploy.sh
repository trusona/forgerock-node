#!/bin/bash

set -e

mvn install deploy -DciBuildNumber=${TRAVIS_BUILD_NUMBER}
./deploy-github.sh