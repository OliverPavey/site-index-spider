#!/usr/bin/env bash

export HOMEPAGE_URL=$1
export OUTPUT_FILE=$2
java -cp . -jar build/libs/siteindex-0.0.1-SNAPSHOT.jar