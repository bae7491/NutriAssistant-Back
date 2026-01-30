#!/bin/bash
  cd /home/ec2-user/app
  nohup java -jar build/libs/nutriassistant_back-0.0.1-SNAPSHOT.jar > /home/ec2-user/app.log 2>&1 &
