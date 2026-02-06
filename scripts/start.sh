#!/bin/bash
export SPRING_PROFILES_ACTIVE=prod

set -a
source /home/ec2-user/app/.env
set +a

cd /home/ec2-user/app
nohup java -jar build/libs/nutriassistant-back-0.0.1-SNAPSHOT.jar > /home/ec2-user/app.log 2>&1 &
