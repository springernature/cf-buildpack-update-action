#!/usr/bin/env sh

echo "Hello $1"

echo "Who am I?"
whoami

echo "Where am I?"
pwd

echo "ls -lha $(pwd)"
ls -lha "$(pwd)"

# $GITHUB_API_URL

time=$(date)
echo "::set-output name=time::$time"

java -jar /app/buildpack-update-action-1.0-SNAPSHOT-all.jar