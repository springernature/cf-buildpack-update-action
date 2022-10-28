#!/usr/bin/env sh

for i in $(seq 1 5); do
  if [ "$i" -eq "3" ]; then
    echo >&2 "$i <-- err"
  else
    echo "$i <-- std"
  fi
  sleep 0.1
done
