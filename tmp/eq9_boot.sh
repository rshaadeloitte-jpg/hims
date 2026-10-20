#!/bin/bash
cd /mnt/d/hims_new_deloitte/services/equipment-service
nohup /home/user/tools/jdk-21.0.12+8/bin/java -jar target/equipment-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev > /tmp/eq9boot.log 2>&1 &
for i in $(seq 1 40); do
  sleep 1
  curl -sf http://localhost:8088/actuator/health >/dev/null 2>&1 && break
done
curl -s http://localhost:8088/actuator/health
echo
grep -E "Started EquipmentServiceApplication" /tmp/eq9boot.log | head -2
