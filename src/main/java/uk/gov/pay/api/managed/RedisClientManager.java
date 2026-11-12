package uk.gov.pay.api.managed;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class RedisClientManager {
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> statefulRedisConnection;

    @Inject
    public RedisClientManager(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public StatefulRedisConnection getRedisConnection() {
        if (statefulRedisConnection == null) {
            statefulRedisConnection = redisClient.connect();
        }
        return statefulRedisConnection;
    }

    /**
     * Closes the connection; the client and its resources are shut down by their own beans.
     */
    @PreDestroy
    public void stop() {
        if (statefulRedisConnection != null) {
            statefulRedisConnection.close();
        }
    }
}
