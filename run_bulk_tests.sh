#!/usr/bin/env bash

mvn -e install -Darguments=-DskipTests
mvn -e -pl javaparser-core-testing test -Dtest=BulkParseTest* test
