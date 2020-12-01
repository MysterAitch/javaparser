#!/usr/bin/env bash

mvn -e compile
mvn -e -pl javaparser-core-testing test -Dtest=BulkParseTest* test
