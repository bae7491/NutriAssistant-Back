#!/bin/bash
  cd /home/ec2-user/app
  chmod +x gradlew || true
  ./gradlew build -x test
