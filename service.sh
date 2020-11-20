#!/bin/bash

BASEDIR="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

run_maven() {

    docker run --rm \
               -w /build \
               -v $BASEDIR:/build \
               -v ~/.m2:/root/.m2 \
               maven:3-adoptopenjdk-11-openj9 $@
}

case $1 in
    check-deploy)
        gcloud meta list-files-for-upload
        ;;
    push-image)
        mvn compile jib:build
        ;;
    build-image)
        mvn compile jib:dockerBuild
        ;;
    *)
        echo -e "Invalid option"
        ;;
esac
