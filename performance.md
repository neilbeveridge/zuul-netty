# zuul-netty Performance Testing - DRAFT

### m1.medium Benchmark 
This benchmark exists as it's easy to saturate the hardware, surfacing the traits of the implementation.

Benchmark parameters:
- 10k stub payload, GZIP, 50ms dither.

#### Vanilla Zuul Result
CPU Saturated at ~1300TPS. Further detail to follow.

#### Zuul-Netty Result
```
Running 3m test @ http://---.us-west-1.compute.internal/?length=10000&dither=50
  100 threads and 200 connections
  Thread Stats  Avg      Stdev    Max  +/- Stdev
    Latency    85.09ms  21.84ms 430.58ms  89.66%
    Req/Sec    23.36      3.87    41.00    74.48%
  136926 requests in 0.97m, 1.30GB read
Requests/sec:  2360.63
Transfer/sec:    22.96MB
```

### High Performance Benchmark
The proxy instance type was improved in order to find a point when it would become network-bound, making best use of the resources. ELBs were found to be unreliable in terms of performance and so the proxy implemented an internal randomised load balancer.

Benchmark parameters:
- 10k stub payload, GZIP, 50ms dither.
- Client: 3 x m1.xl
- Stub: 5 x m3.2xl
- Proxy: 1 x m3.2xl

I used 3 clients in order not to saturate the network. I wasn't able to saturate the CPU on the proxy – it always had 30% idling. I imagine that the NIC was saturated on the proxy as it was handling inbound and outbound traffic. The three clients gave the following results – you can see the raised latency caused by network saturation:

```
Running 3m test @ http://ip-172-31-24-110.us-west-1.compute.internal/?length=10000&dither=50
  400 threads and 700 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   139.88ms  113.00ms   1.11s    87.88%
    Req/Sec     9.12      3.89    20.00     83.37%
  688494 requests in 3.00m, 6.54GB read
Requests/sec:   3825.85
Transfer/sec:     37.21MB

Running 3m test @ http://ip-172-31-24-110.us-west-1.compute.internal/?length=10000&dither=50
  400 threads and 700 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   166.01ms  144.06ms 992.36ms   89.92%
    Req/Sec     7.78      3.53    24.00     83.23%
  593650 requests in 3.00m, 5.64GB read
Requests/sec:   3299.71
Transfer/sec:     32.10MB

Running 3m test @ http://ip-172-31-24-110.us-west-1.compute.internal/?length=10000&dither=50
  400 threads and 700 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   237.02ms  244.94ms   1.22s    81.00%
    Req/Sec     7.73      4.73    27.00     67.53%
  586688 requests in 3.00m, 5.57GB read
Requests/sec:   3259.44
Transfer/sec:     31.71MB
```

If I run the same on only two clients then the proxy idles at 46% and I see the following results at the clients:

```
Running 3m test @ http://ip-172-31-24-110.us-west-1.compute.internal/?length=10000&dither=50
  400 threads and 700 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    83.11ms   18.77ms 567.60ms   98.00%
    Req/Sec    11.75      2.23    24.00     82.09%
  879649 requests in 3.00m, 8.36GB read
Requests/sec:   4889.49
Transfer/sec:     47.56MB

Running 3m test @ http://ip-172-31-24-110.us-west-1.compute.internal/?length=10000&dither=50
  400 threads and 700 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    83.64ms   21.74ms 455.94ms   98.15%
    Req/Sec    11.78      4.04    26.00     56.82%
  879316 requests in 3.00m, 8.35GB read
Requests/sec:   4885.35
Transfer/sec:     47.52MB
```

Further direct vanilla-netty bakeoff data to follow.
