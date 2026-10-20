#!/bin/bash
# -Xlint:all zero-warnings check for PmServiceTest (fat-jar classpath trick + test jars from .m2)
# Usage: bash tmp/lint_pm_test.sh   → prints "LINT-CLEAN: zero warnings" on success.
set -e
JAR=/mnt/d/hims_new_deloitte/services/equipment-service/target/equipment-service-0.1.0-SNAPSHOT.jar
JDK=/home/user/tools/jdk-21.0.12+8/bin
M2=/home/user/.m2/repository
rm -rf /tmp/eqjar /tmp/eqout
mkdir -p /tmp/eqjar /tmp/eqout
cd /tmp/eqjar
"$JDK/jar" -xf "$JAR"
CP="/tmp/eqjar/BOOT-INF/classes:/tmp/eqjar/BOOT-INF/lib/*"
CP="$CP:$M2/org/junit/jupiter/junit-jupiter-api/5.12.2/junit-jupiter-api-5.12.2.jar"
CP="$CP:$M2/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar"
CP="$CP:$M2/org/mockito/mockito-junit-jupiter/5.17.0/mockito-junit-jupiter-5.17.0.jar"
CP="$CP:$M2/org/assertj/assertj-core/3.27.3/assertj-core-3.27.3.jar"
CP="$CP:$M2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"
CP="$CP:$M2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar"
CP="$CP:$M2/org/springframework/spring-test/6.2.8/spring-test-6.2.8.jar"
CP="$CP:$M2/net/bytebuddy/byte-buddy/1.17.6/byte-buddy-1.17.6.jar"
CP="$CP:$M2/net/bytebuddy/byte-buddy-agent/1.17.6/byte-buddy-agent-1.17.6.jar"
CP="$CP:$M2/org/objenesis/objenesis/3.3/objenesis-3.3.jar"
"$JDK/javac" -Xlint:all -d /tmp/eqout -cp "$CP" \
  /mnt/d/hims_new_deloitte/services/equipment-service/src/test/java/com/hims/equipment/application/PmServiceTest.java
echo "LINT-CLEAN: zero warnings"
