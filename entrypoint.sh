#!/usr/bin/env sh

echo "Hello $1"

echo "Where am I?"
pwd
echo "Who am I?"
whoami
echo "file system"
find / -type d
time=$(date)
echo "::set-output name=time::$time"
