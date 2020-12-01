#!/usr/bin/env bash

mvn -e mvn install -Darguments=-DskipTests
mvn -e -pl javaparser-core-testing test -Dtest=BulkParseTest* test
