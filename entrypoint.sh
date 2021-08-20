#!/usr/bin/env sh

echo "Hello $1"

echo "Who am I?"
whoami

echo "Where am I?"
pwd

echo "ls -lha $(pwd)"
ls -lha $(pwd)

time=$(date)
echo "::set-output name=time::$time"
