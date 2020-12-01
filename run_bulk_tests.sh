#!/usr/bin/env bash

set -x ## Enable logging of commands run

## Must be installed first, to be used..
mvn -e install -Darguments=-DskipTests

## Run just the bulk parse tests that do downloading
mvn -e -pl javaparser-core-testing test -Dtest=BulkParseTest* -P AlsoSlowTests

set +x ## Disable logging of commands run
