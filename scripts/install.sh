#!/bin/bash
  cd /home/ec2-user/app
  chmod +x gradlew
  ./gradlew build -x test
