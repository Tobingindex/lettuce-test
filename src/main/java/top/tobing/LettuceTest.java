package top.tobing;

import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.tobing.utils.RedisUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LettuceTest
 *
 * @author liangshangfu
 * @date 2023-04-12 12:26
 */
public class LettuceTest {

    private static final Logger logger = LoggerFactory.getLogger(LettuceTest.class);


    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            logger.info("----------------------------before----------------------------");
            query(i);
            logger.info("----------------------------After----------------------------");
        }

    }

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
}
