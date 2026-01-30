#!/bin/bash
  cd /home/ec2-user/app
  nohup java -jar build/libs/*.jar > /home/ec2-user/app.log 2>&1 &
