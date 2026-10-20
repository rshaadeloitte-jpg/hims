#!/bin/bash
# -Xlint:all zero-warnings check for the TAS-16 opening-balance main classes
# (fat-jar classpath trick; main classes need no test jars).
# Usage: bash tmp/lint_ob_main.sh -> prints "LINT-CLEAN: zero warnings" on success.
set -e
JAR=/mnt/d/hims_new_deloitte/services/equipment-service/target/equipment-service-0.1.0-SNAPSHOT.jar
JDK=/home/user/tools/jdk-21.0.12+8/bin
SRC=/mnt/d/hims_new_deloitte/services/equipment-service/src/main/java/com/hims/equipment
rm -rf /tmp/eqjar16 /tmp/eqout16
mkdir -p /tmp/eqjar16 /tmp/eqout16
cd /tmp/eqjar16
"$JDK/jar" -xf "$JAR"
CP="/tmp/eqjar16/BOOT-INF/classes:/tmp/eqjar16/BOOT-INF/lib/*"
"$JDK/javac" -Xlint:all -d /tmp/eqout16 -cp "$CP" \
  "$SRC/domain/model/EquipmentOpeningBalance.java" \
  "$SRC/domain/repository/EquipmentOpeningBalanceRepository.java" \
  "$SRC/application/OpeningBalanceService.java" \
  "$SRC/api/OpeningBalanceController.java"
echo "LINT-CLEAN: zero warnings"
