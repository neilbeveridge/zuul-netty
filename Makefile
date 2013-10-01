SHELL := /bin/bash

REMOTE_HOST=ec2-54-215-236-136.us-west-1.compute.amazonaws.com

# Performance Test
perf-test:
	mvn clean install -Pperformance

release:
	mvn -f netty-server/pom.xml  assembly:assembly

deploy:
	mvn -Dmaven.test.skip=true clean install
	mvn -Dmaven.test.skip=true -pl netty-server assembly:assembly
	scp -i ~/Downloads/nbev-zuul.pem netty-server/target/netty-server-1.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@$(REMOTE_HOST):

