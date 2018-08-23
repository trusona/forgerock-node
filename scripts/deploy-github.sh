#!/bin/bash

ORG=trusona
REPO=forgerock-module

FILE=$(ls target/*-all.jar)
FILE=${FILE##*/}

if [ -z "${TRAVIS_TAG}" ]; then
  echo "This script requires TRAVIS_TAG to be set"
  exit 1
fi
if [ -z "${GITHUB_TOKEN}" ]; then
  echo "This script requires GITHUB_TOKEN to be set"
  exit 1
fi

release_id=$(curl \
  -H "Content-type: application/json" \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  "https://api.github.com/repos/${ORG}/${REPO}/releases" \
  -d "{\"tag_name\": \"${TRAVIS_TAG}\", \"name\": \"${TRAVIS_TAG}\"}" | jq .id)

echo "created release with id ${release_id}"

curl \
  -H "Content-Type: application/java-archive" \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  "https://uploads.github.com/repos/${ORG}/${REPO}/releases/${release_id}/assets?name=${FILE}" \
  -d "@target/${FILE}"
