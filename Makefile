SHELL := /bin/bash

# Performance Test
perf-test:
	mvn clean install -Pperformance

release:
	mvn -f netty-server/pom.xml  assembly:assembly