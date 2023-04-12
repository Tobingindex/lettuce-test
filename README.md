# Some commands write queues later than refresh queues, resulting in command blocking

When I execute the following code, the program occasionally blocks and waits for some Future.

> Code

```java
private static void query(int times) throws InterruptedException {
    RedisClusterClient redisClusterClient = RedisUtils.initRedisClient();
    StatefulRedisClusterConnection<String, String> asyncConnect = redisClusterClient.connect();
    StatefulRedisClusterConnection<String, String> syncConnect = redisClusterClient.connect();
    asyncConnect.setAutoFlushCommands(false);

    String redisKey = "smt:eq:hot:hk:*";
    List<String> keys = RedisUtils.scanKeys(redisKey, syncConnect);
    System.out.println(keys.size());
    String[] quotationFiled = {"preClosePrice", "lastPrice", "upperLimitPrice", "lowerLimitPrice"};

    ArrayList<Future<?>> futures = new ArrayList<>();
    final AtomicInteger errorCounter = new AtomicInteger(0);
    final AtomicInteger successCounter = new AtomicInteger(0);
    final AtomicInteger remainingCounter = new AtomicInteger(0);
    RedisAdvancedClusterAsyncCommands<String, String> asyncCommands = asyncConnect.async();
    logger.info("[hmget] before");
    keys.forEach(k -> {
        RedisFuture<List<KeyValue<String, String>>> future = asyncCommands.hmget(k, quotationFiled);
        futures.add(future);
        remainingCounter.incrementAndGet();
        future.whenComplete((response, error) -> {
            if (error != null) {
                errorCounter.incrementAndGet();
            } else {
                successCounter.incrementAndGet();
            }
            remainingCounter.decrementAndGet();
        });
    });
    logger.info("[hmget] after");

    logger.error("[flushCommands] before");
    asyncConnect.flushCommands();
    logger.error("[flushCommands] after");

    Thread.sleep(100);
    while (remainingCounter.get() > 0) {
        logger.info("{} remaining, {} succeeded, {} failed, {} futures remaining. forTimes:{}",
                    remainingCounter.get(),
                    successCounter.get(),
                    errorCounter.get(),
                    futures.stream().filter(f -> !f.isDone()).count(),
                    times);
        Thread.sleep(100);
    }
}
```

> Output

```bash
[INFO ] 2023-04-12 13:00:10.223[INFO][main]top.tobing.LettuceTest.main:30 -----------------------------before----------------------------
[INFO ] 2023-04-12 13:00:10.241[INFO][lettuce-eventExecutorLoop-8-2]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52558 -> /10.10.87.185:3001]
[INFO ] 2023-04-12 13:00:10.244[INFO][lettuce-eventExecutorLoop-8-3]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52559 -> /10.10.87.185:3002]
[INFO ] 2023-04-12 13:00:10.245[INFO][lettuce-eventExecutorLoop-8-3]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52560 -> /10.10.87.185:3003]
[INFO ] 2023-04-12 13:00:10.245[INFO][lettuce-eventExecutorLoop-8-3]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52561 -> /10.10.87.185:3004]
[INFO ] 2023-04-12 13:00:10.245[INFO][lettuce-eventExecutorLoop-8-4]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52562 -> /10.10.87.185:3005]
[INFO ] 2023-04-12 13:00:10.246[INFO][lettuce-eventExecutorLoop-8-5]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52563 -> /10.10.87.185:3006]
[INFO ] 2023-04-12 13:00:10.247[INFO][lettuce-eventExecutorLoop-8-6]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52558 -> /10.10.87.185:3001]
[INFO ] 2023-04-12 13:00:10.250[INFO][lettuce-eventExecutorLoop-8-7]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52559 -> /10.10.87.185:3002]
[INFO ] 2023-04-12 13:00:10.250[INFO][lettuce-eventExecutorLoop-8-7]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52561 -> /10.10.87.185:3004]
[INFO ] 2023-04-12 13:00:10.250[INFO][lettuce-eventExecutorLoop-8-8]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52562 -> /10.10.87.185:3005]
[INFO ] 2023-04-12 13:00:10.250[INFO][lettuce-eventExecutorLoop-8-8]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52560 -> /10.10.87.185:3003]
[INFO ] 2023-04-12 13:00:10.251[INFO][lettuce-eventExecutorLoop-8-9]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52563 -> /10.10.87.185:3006]
[ERROR] 2023-04-12 13:00:10.259[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:DisconnectedEvent [/172.30.67.33:52559 -> /10.10.87.185:3002]
[ERROR] 2023-04-12 13:00:10.260[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:DisconnectedEvent [/172.30.67.33:52560 -> /10.10.87.185:3003]
[ERROR] 2023-04-12 13:00:10.260[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:ConnectionDeactivatedEvent [/172.30.67.33:52560 -> /10.10.87.185:3003]
[ERROR] 2023-04-12 13:00:10.260[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:DisconnectedEvent [/172.30.67.33:52561 -> /10.10.87.185:3004]
[ERROR] 2023-04-12 13:00:10.260[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:ConnectionDeactivatedEvent [/172.30.67.33:52559 -> /10.10.87.185:3002]
[ERROR] 2023-04-12 13:00:10.261[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:ConnectionDeactivatedEvent [/172.30.67.33:52561 -> /10.10.87.185:3004]
[ERROR] 2023-04-12 13:00:10.261[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:DisconnectedEvent [/172.30.67.33:52562 -> /10.10.87.185:3005]
[ERROR] 2023-04-12 13:00:10.261[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:DisconnectedEvent [/172.30.67.33:52558 -> /10.10.87.185:3001]
[ERROR] 2023-04-12 13:00:10.261[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:ConnectionDeactivatedEvent [/172.30.67.33:52562 -> /10.10.87.185:3005]
[ERROR] 2023-04-12 13:00:10.262[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:ConnectionDeactivatedEvent [/172.30.67.33:52558 -> /10.10.87.185:3001]
[ERROR] 2023-04-12 13:00:10.262[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:DisconnectedEvent [/172.30.67.33:52563 -> /10.10.87.185:3006]
[ERROR] 2023-04-12 13:00:10.262[ERROR][lettuce-eventExecutorLoop-8-10]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:53 -lettuce event error:ConnectionDeactivatedEvent [/172.30.67.33:52563 -> /10.10.87.185:3006]
[INFO ] 2023-04-12 13:00:10.264[INFO][lettuce-eventExecutorLoop-8-11]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52564 -> /10.10.87.185:3003]
[INFO ] 2023-04-12 13:00:10.269[INFO][lettuce-eventExecutorLoop-8-12]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52564 -> /10.10.87.185:3003]
[INFO ] 2023-04-12 13:00:10.280[INFO][lettuce-eventExecutorLoop-8-13]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52565 -> /10.10.87.185:3003]
[INFO ] 2023-04-12 13:00:10.284[INFO][lettuce-eventExecutorLoop-8-14]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52565 -> /10.10.87.185:3003]
[INFO ] 2023-04-12 13:00:10.295[INFO][lettuce-eventExecutorLoop-8-15]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52566 -> /10.10.87.185:3004]
[INFO ] 2023-04-12 13:00:10.300[INFO][lettuce-eventExecutorLoop-8-16]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52566 -> /10.10.87.185:3004]
[INFO ] 2023-04-12 13:00:10.696[INFO][lettuce-eventExecutorLoop-8-1]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52567 -> /10.10.87.185:3006]
[INFO ] 2023-04-12 13:00:10.701[INFO][lettuce-eventExecutorLoop-8-2]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52567 -> /10.10.87.185:3006]
[INFO ] 2023-04-12 13:00:11.039[INFO][lettuce-eventExecutorLoop-8-3]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52568 -> /10.10.87.185:3002]
[INFO ] 2023-04-12 13:00:11.044[INFO][lettuce-eventExecutorLoop-8-4]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52568 -> /10.10.87.185:3002]
15553
[INFO ] 2023-04-12 13:00:11.489[INFO][main]top.tobing.LettuceTest.query:53 -[hmget] before
[INFO ] 2023-04-12 13:00:11.494[INFO][lettuce-eventExecutorLoop-8-5]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52569 -> /10.10.87.185:3002]
[INFO ] 2023-04-12 13:00:11.496[INFO][lettuce-eventExecutorLoop-8-6]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52570 -> /10.10.87.185:3004]
[INFO ] 2023-04-12 13:00:11.497[INFO][lettuce-eventExecutorLoop-8-6]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectedEvent [/172.30.67.33:52571 -> /10.10.87.185:3006]
[INFO ] 2023-04-12 13:00:11.499[INFO][lettuce-eventExecutorLoop-8-7]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52569 -> /10.10.87.185:3002]
[INFO ] 2023-04-12 13:00:11.501[INFO][lettuce-eventExecutorLoop-8-8]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52571 -> /10.10.87.185:3006]
[INFO ] 2023-04-12 13:00:11.501[INFO][lettuce-eventExecutorLoop-8-8]top.tobing.utils.RedisUtils.lambda$initRedisClient$0:55 -lettuce event:ConnectionActivatedEvent [/172.30.67.33:52570 -> /10.10.87.185:3004]
[INFO ] 2023-04-12 13:00:11.503[INFO][main]top.tobing.LettuceTest.query:67 -[hmget] after
[ERROR] 2023-04-12 13:00:11.504[ERROR][main]top.tobing.LettuceTest.query:69 -[flushCommands] before
[ERROR] 2023-04-12 13:00:11.512[ERROR][main]top.tobing.LettuceTest.query:71 -[flushCommands] after
[INFO ] 2023-04-12 13:00:11.627[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:11.732[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:11.838[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:11.941[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.046[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.151[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.255[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.360[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.462[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.571[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.676[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.781[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.887[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:12.991[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.096[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.201[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.307[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.411[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.517[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.619[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.727[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.832[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:13.937[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.042[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.148[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.253[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.357[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.459[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.567[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.672[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.777[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.882[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2
[INFO ] 2023-04-12 13:00:14.988[INFO][main]top.tobing.LettuceTest.query:75 -209 remaining, 15344 succeeded, 0 failed, 209 futures remaining. forTimes:2

Process finished with exit code -1
```

**I found that some commands writing queues later than refreshing queues caused command blocking.**

The reason is that when 「[asyncCommands.hmget(k, quotationFiled)](https://github.com/Tobingindex/lettuce-test/blob/master/src/main/java/top/tobing/LettuceTest.java#L55)」 is executed, it will ultimately be written to 「[DefaultEndpoint#commandBuffer](https://github.com/lettuce-io/lettuce-core/blob/6.0.x/src/main/java/io/lettuce/core/protocol/DefaultEndpoint.java#L355)」 through 「[ClusterDistributionChannelWriter#doWrite()](https://github.com/lettuce-io/lettuce-core/blob/6.0.x/src/main/java/io/lettuce/core/cluster/ClusterDistributionChannelWriter.java#L112)」.

However, due to the fact that the connectFuture was not yet prepared when「[ClusterDistributionChannelWriter#doWrite()](https://github.com/lettuce-io/lettuce-core/blob/6.0.x/src/main/java/io/lettuce/core/cluster/ClusterDistributionChannelWriter.java#L112)」was used, the actual time to write 「[DefaultEndpoint#commandBuffer](https://github.com/lettuce-io/lettuce-core/blob/6.0.x/src/main/java/io/lettuce/core/protocol/DefaultEndpoint.java#L355)」 was later than 「[asyncConnect.flushCommands()](https://github.com/Tobingindex/lettuce-test/blob/master/src/main/java/top/tobing/LettuceTest.java#L70)」



