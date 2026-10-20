#!/bin/bash
# TAS-16 live boot: equipment-service dev profile against shared PostgreSQL 16.15
cd /mnt/d/hims_new_deloitte/services/equipment-service || exit 1
pgrep -f '^/home/user/tools/jdk' | xargs -r kill 2>/dev/null
sleep 2
nohup /home/user/tools/jdk-21.0.12+8/bin/java -jar target/equipment-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev > /tmp/eq16_boot.log 2>&1 &
echo "started pid $!"
for i in $(seq 1 60); do
  if curl -sf -u admin:admin http://localhost:8088/actuator/health > /dev/null 2>&1; then
    echo "UP after ${i}s"
    echo "boot-log ERROR lines: $(grep -c 'ERROR' /tmp/eq16_boot.log)"
    grep -i 'flyway\|migrat' /tmp/eq16_boot.log | head -5
    exit 0
  fi
  sleep 1
done
echo "SERVICE DID NOT COME UP"
tail -40 /tmp/eq16_boot.log
exit 1
