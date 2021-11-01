#!/bin/bash 

DIR="libs"
V_VERSION="3.0.0"
if [ -d "$DIR" ]; then
  echo "libs folders exists"
else
  mkdir libs
  # shellcheck disable=SC2164
  cd libs 
  wget https://versions.velocitypowered.com/download/$V_VERSION.jar
  exit 0
fi
