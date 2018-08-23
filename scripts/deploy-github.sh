#!/bin/bash

set -e

ORG=trusona
REPO=forgerock-node

FILE=$(ls target/*-all.jar)
FILENAME=${FILE##*/}

if [ -z "${TRAVIS_TAG}" ]; then
  echo "This script requires TRAVIS_TAG to be set"
  exit 1
fi
if [ -z "${GITHUB_TOKEN}" ]; then
  echo "This script requires GITHUB_TOKEN to be set"
  exit 1
fi

release_id=$(curl -v \
  -H "Content-type: application/json" \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  "https://api.github.com/repos/${ORG}/${REPO}/releases" \
  -d "{\"tag_name\": \"${TRAVIS_TAG}\", \"name\": \"${TRAVIS_TAG}\"}" | jq .id)

echo "created release with id ${release_id}"

curl -v \
  -H "Content-Type: application/java-archive" \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  "https://uploads.github.com/repos/${ORG}/${REPO}/releases/${release_id}/assets?name=${FILENAME}" \
  -d "@${FILE}"
