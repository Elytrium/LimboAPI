#!/bin/bash 

DIR="libs"
VELOCITY_BUILD="92"
OUTPUT_NAME="3.0.0.jar"
if [ -d "$DIR" ]; then
  echo "libs folders exists"
else
  mkdir libs
  # shellcheck disable=SC2164
  cd libs
  wget https://ci.velocitypowered.com/job/velocity-3.0.0/$VELOCITY_BUILD/artifact/proxy/build/libs/velocity-proxy-3.1.0-SNAPSHOT-all.jar -O $OUTPUT_NAME
  exit 0
fi
