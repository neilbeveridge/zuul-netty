SHELL := /bin/bash

REMOTE_HOST=ec2-54-215-240-14.us-west-1.compute.amazonaws.com

# Performance Test
perf-test:
	mvn clean install -Pperformance

release:
	mvn -f netty-server/pom.xml  assembly:assembly

deploy:
	mvn -Dmaven.test.skip=true clean install
	mvn -Dmaven.test.skip=true -pl netty-server assembly:assembly
#	scp -i ~/Downloads/nbev-zuul.pem netty-server/target/netty-server-1.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@$(REMOTE_HOST):

server:
	mvn -f netty-server/pom.xml compile exec:java -Dmain.class=com.netflix.zuul.proxy.ProxyServer -Dexec.args="8080 zuul-core/src/main/filters/pre"

client:
	mvn -f netty-mock-server/pom.xml compile exec:java -Dmain.class=com.netflix.zuul.proxy.MockEndpoint -Dexec.args="8081 EXAMPLE_RESPONSE"
