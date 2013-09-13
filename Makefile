SHELL := /bin/bash

# Performance Test
perf-test:
	mvn clean install -Pperformance
