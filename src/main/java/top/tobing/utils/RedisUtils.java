package top.tobing.utils;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ConnectionDeactivatedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * RedisUtils
 *
 * @author liangshangfu
 * @date 2023-04-12 12:27
 */
public class RedisUtils {

    private final static Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    public static String redisServiceUrl = "10.10.87.185:3001,10.10.87.185:3002,10.10.87.185:3003,10.10.87.185:3004,10.10.87.185:3005,10.10.87.185:3006";

    public static RedisClusterClient initRedisClient() {
        List<RedisURI> list = new LinkedList<>();
        for (String server : redisServiceUrl.split(",")) {
            String host = server.split(":")[0];
            String port = server.split(":")[1];
            RedisURI.Builder builder = RedisURI.builder().
                    withHost(host).
                    withPort(Integer.parseInt(port))
                    .withPassword("********");
            list.add(builder.build());
        }
        RedisClusterClient redisClient = RedisClusterClient.create(list);
        redisClient.setOptions(clusterClientOptions());

        EventBus eventBus = redisClient.getResources().eventBus();
        eventBus.get().subscribe(e -> {
            if ((e instanceof ConnectionDeactivatedEvent)
                    || (e instanceof DisconnectedEvent)) {
                logger.error("lettuce event error:{}", e.toString());
            } else {
                logger.info("lettuce event:{}", e.toString());
            }
        });
        return redisClient;
    }

    public static List<String> scanKeys(String keyPattern, StatefulRedisClusterConnection<String, String> connection) {
        Set<String> keyList = new HashSet<>();
        try {
            ScanCursor curs = ScanCursor.INITIAL;
            while (!curs.isFinished()) {
                ScanArgs args = new ScanArgs();
                args.match(keyPattern);
                args.limit(1000);
                KeyScanCursor<String> keyCursor = connection.sync().scan(curs, args);
                keyList.addAll(keyCursor.getKeys());
                curs = keyCursor;
            }
        } catch (Exception ex) {
            keyList = new HashSet<>();
        }
        return new LinkedList<>(keyList);
    }


    private static ClusterClientOptions clusterClientOptions() {

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(100))
                .build();

        return ClusterClientOptions.builder().
                autoReconnect(true).
                maxRedirects(3)
                // .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(50)))
                // .topologyRefreshOptions(clusterTopologyRefreshOptions)
                .build();
    }

}
