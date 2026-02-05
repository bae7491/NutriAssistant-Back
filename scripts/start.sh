#!/bin/bash
export SPRING_PROFILES_ACTIVE=prod


export DB_HOST="kt-team18-db.cboy0kogog8k.ap-northeast-2.rds.amazonaws.com"
export DB_PORT="3306"
export DB_NAME="nutriassistant"
export DB_USERNAME="admin"
export DB_PASSWORD="ktaivle18"
export FAST_API_URL="172.31.41.226"

cd /home/ec2-user/app
nohup java -jar build/libs/nutriassistant-back-0.0.1-SNAPSHOT.jar > /home/ec2-user/app.log 2>&1 &
