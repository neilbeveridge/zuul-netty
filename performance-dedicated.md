# Performance Testing on Dedicated Hardware

## Highlights

-   We have successfully achieved the objective of tuning the Zuul Netty port to reach linear scalability compared to its Tomcat version and highlight the benefits of nonblocking IO. To achieve this we did some TCP tweaks and added overrides for the netty’s IO threads.
-   The load test scenario was simulated using the “wrk benchmark” tool which was simulating requests through KeepAlive connections against a target stub which simulated a random 1k payload with a 50ms latency. In a realistic scenario we assume this implementation will be behind a Netscaler/VIP which would reuse connections instead of frequent connections open/close.
    Note: We observed that comparatively tomcat’s APR was much more efficient in cases where there was no KeepAlive, i.e every connection was doing the TCP handshakes.
-   The scalability point with respect to Tomcat vs Netty was determined by discounting the overheads/latencies incurred in the time spent in processing the response content. Based on the table below, we can conclude that tomcat’s scalability point was achieved at 1300 concurrent connections with a throughput of around 16K TPS, whereas the Netty implementation was linearly scalable [we tested upto 2000 connections ~ 28K TPS].
-   Netty response times for maximum load of 2000 connections was around 71 ms, that is a total of ~21ms spent on the wire and in the proxy. The difference in the response time at each load level was almost uniform, this can be attributed to the overhead of processing the increasing server load [overhead of the HTTP codec/network latencies within the stream, @max load we were using 680Mbps on a 1G NIC and network saturation effects contributed to an increase in latency at this load level].
-   Tomcat response time started to increase significantly with increasing connections, which is a composite effect of blocking connections, frequent GC and high number of busy worker threads. This is reflected in the Operating system’s run queue length and the JVM’s GC throughput. Comparison graphs can be found below.

## Positives of Netty:
 -  Non blocking inbound and outbound – We were able to handle high number of concurrent connections with a significantly low number of threads. This has helped reduce the CPU utilization spent on the Selectors.
 -  Efficiently utilizes the system resources like CPU/network. Even higher throughput can be achieved with bonded or dedicated NICs. Depending upon the additional tasks on the proxy layer we might need additional CPU capacity.
 -  High number of connections doesn’t affect the stability of the ZUUL proxy instance, whereas with Tomcat the instance became unresponsive when the worker threads reach limits.
 -  Memory utilization is very efficient as the temporary stacks created by the number of threads are much less due to its low thread count. Higher application throughput is visible in the GC graphs shown below.

## Context Switching:
 -  In Netty based ZUUL we observed that the context switches started off at 100K per second and settled at 48K at peak load. This is because at the start of the run the efficiency of the ZUUL proxy is at peak [all threads were active from start of the run] since Netty NIO is event based, this trend matches the TPS and mirrors the response latency.
 -  However, in tomcat the worker threads increases with increase in concurrent connections and leveled off as soon as the throughput settled.

## Configuration
### TCP Parameters
- Increase the OS’s receive and write buffers, net.core.rmem_max and net.core.wmem_max.
- Maximum number of packets queued on the INPUT side, especially when the NIC receives packets faster than kernel can process them. - net.core.netdev_max_backlog.
- Increased the backlog per port, net.core.somaxconn and the global limit, tcp_max_syn_backlog.

### Netty Overrides
-   -Dxorg.jboss.netty.epollBugWorkaround=true – To enable the epoll CPU fix.
-   -Dxorg.jboss.netty.selectTimeout=10 – This is the default, has effect only when idling.
-   -Dcom.netflix.zuul.workers.inbound=4, -Dcom.netflix.zuul.workers.stage=8 and -Dcom.netflix.zuul.workers.outbound=4 –> IO thread count to limit the inbound NIO.

