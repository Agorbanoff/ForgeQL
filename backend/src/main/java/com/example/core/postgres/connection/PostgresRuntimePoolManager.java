package com.example.core.postgres.connection;

import com.example.common.exceptions.PostgresPoolLimitExceededException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PostgresRuntimePoolManager {

    private final PostgresConnectionFingerprintService connectionFingerprintService;
    private final int maxActivePools;
    private final int maxPoolSize;
    private final int minIdle;
    private final long idlePoolEvictionMs;
    private final long borrowTimeoutMs;
    private final Map<PostgresPoolKey, PoolEntry> pools = new LinkedHashMap<>();

    public PostgresRuntimePoolManager(
            PostgresConnectionFingerprintService connectionFingerprintService,
            @Value("${core.postgres.pool.max-active-pools:16}") int maxActivePools,
            @Value("${core.postgres.pool.max-size:3}") int maxPoolSize,
            @Value("${core.postgres.pool.min-idle:0}") int minIdle,
            @Value("${core.postgres.pool.idle-eviction-ms:300000}") long idlePoolEvictionMs,
            @Value("${core.postgres.pool.borrow-timeout-ms:10000}") long borrowTimeoutMs
    ) {
        this.connectionFingerprintService = connectionFingerprintService;
        this.maxActivePools = Math.max(1, maxActivePools);
        this.maxPoolSize = Math.max(1, maxPoolSize);
        this.minIdle = Math.max(0, Math.min(minIdle, this.maxPoolSize));
        this.idlePoolEvictionMs = Math.max(10_000L, idlePoolEvictionMs);
        this.borrowTimeoutMs = Math.max(250L, borrowTimeoutMs);
    }

    public Connection openConnection(
            PostgresRuntimeConnectionDefinition definition,
            PostgresJdbcConnectionConfig config
    ) throws SQLException {
        PoolEntry poolEntry = acquirePool(definition, config);
        try {
            Connection connection = poolEntry.dataSource().getConnection();
            return wrapConnection(connection, poolEntry);
        } catch (SQLException e) {
            poolEntry.releaseLease();
            throw e;
        }
    }

    @Scheduled(fixedDelayString = "${core.postgres.pool.cleanup-interval-ms:60000}")
    public synchronized void evictIdlePools() {
        evictExpiredIdlePools(System.currentTimeMillis());
    }

    @PreDestroy
    public synchronized void shutdown() {
        Iterator<Map.Entry<PostgresPoolKey, PoolEntry>> iterator = pools.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PostgresPoolKey, PoolEntry> entry = iterator.next();
            closeQuietly(entry.getValue().dataSource());
            iterator.remove();
        }
    }

    private synchronized PoolEntry acquirePool(
            PostgresRuntimeConnectionDefinition definition,
            PostgresJdbcConnectionConfig config
    ) {
        long now = System.currentTimeMillis();
        evictExpiredIdlePools(now);

        PostgresPoolKey poolKey = new PostgresPoolKey(
                definition.datasourceId(),
                connectionFingerprintService.fingerprint(config)
        );
        PoolEntry existingEntry = pools.get(poolKey);
        if (existingEntry != null) {
            existingEntry.acquireLease(now);
            return existingEntry;
        }

        ensureCapacity(now);

        HikariDataSource dataSource = createDataSource(definition, config, poolKey.fingerprint());
        PoolEntry poolEntry = new PoolEntry(dataSource, now);
        poolEntry.acquireLease(now);
        pools.put(poolKey, poolEntry);
        return poolEntry;
    }

    private void ensureCapacity(long now) {
        while (pools.size() >= maxActivePools) {
            if (!evictLeastRecentlyUsedIdlePool(now)) {
                throw new PostgresPoolLimitExceededException(
                        "Maximum number of active PostgreSQL runtime pools has been reached"
                );
            }
        }
    }

    private void evictExpiredIdlePools(long now) {
        Iterator<Map.Entry<PostgresPoolKey, PoolEntry>> iterator = pools.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PostgresPoolKey, PoolEntry> entry = iterator.next();
            if (entry.getValue().isIdleFor(now, idlePoolEvictionMs)) {
                closeQuietly(entry.getValue().dataSource());
                iterator.remove();
            }
        }
    }

    private boolean evictLeastRecentlyUsedIdlePool(long now) {
        PostgresPoolKey oldestIdlePoolKey = null;
        PoolEntry oldestIdlePoolEntry = null;

        for (Map.Entry<PostgresPoolKey, PoolEntry> entry : pools.entrySet()) {
            PoolEntry candidate = entry.getValue();
            if (!candidate.isIdle()) {
                continue;
            }
            if (oldestIdlePoolEntry == null || candidate.lastAccessAtMs() < oldestIdlePoolEntry.lastAccessAtMs()) {
                oldestIdlePoolKey = entry.getKey();
                oldestIdlePoolEntry = candidate;
            }
        }

        if (oldestIdlePoolKey == null || oldestIdlePoolEntry == null) {
            return false;
        }

        closeQuietly(oldestIdlePoolEntry.dataSource());
        pools.remove(oldestIdlePoolKey);
        return true;
    }

    private HikariDataSource createDataSource(
            PostgresRuntimeConnectionDefinition definition,
            PostgresJdbcConnectionConfig config,
            String fingerprint
    ) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setDataSourceProperties(config.properties());
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setConnectionTimeout(borrowTimeoutMs);
        hikariConfig.setIdleTimeout(idlePoolEvictionMs);
        hikariConfig.setInitializationFailTimeout(-1);
        hikariConfig.setAutoCommit(true);
        hikariConfig.setPoolName(buildPoolName(definition.datasourceId(), fingerprint));
        return new HikariDataSource(hikariConfig);
    }

    private String buildPoolName(Integer datasourceId, String fingerprint) {
        String shortFingerprint = fingerprint.length() > 12 ? fingerprint.substring(0, 12) : fingerprint;
        return "forgeql-pg-" + datasourceId + "-" + shortFingerprint;
    }

    private Connection wrapConnection(Connection connection, PoolEntry poolEntry) {
        AtomicBoolean closed = new AtomicBoolean(false);
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                boolean releaseLease = closed.compareAndSet(false, true);
                try {
                    if (releaseLease) {
                        return method.invoke(connection, args);
                    }
                    return null;
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                } finally {
                    if (releaseLease) {
                        poolEntry.releaseLease();
                    }
                }
            }

            try {
                return method.invoke(connection, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        };

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                invocationHandler
        );
    }

    private void closeQuietly(HikariDataSource dataSource) {
        try {
            dataSource.close();
        } catch (RuntimeException ignored) {
            // Closing an already-failed pool should not block shutdown or eviction.
        }
    }

    private record PostgresPoolKey(Integer datasourceId, String fingerprint) {
    }

    private static final class PoolEntry {
        private final HikariDataSource dataSource;
        private final AtomicInteger outstandingLeases = new AtomicInteger(0);
        private volatile long lastAccessAtMs;

        private PoolEntry(HikariDataSource dataSource, long createdAtMs) {
            this.dataSource = dataSource;
            this.lastAccessAtMs = createdAtMs;
        }

        private HikariDataSource dataSource() {
            return dataSource;
        }

        private void acquireLease(long now) {
            outstandingLeases.incrementAndGet();
            lastAccessAtMs = now;
        }

        private void releaseLease() {
            outstandingLeases.updateAndGet(current -> Math.max(0, current - 1));
            lastAccessAtMs = System.currentTimeMillis();
        }

        private boolean isIdleFor(long now, long idleWindowMs) {
            return isIdle() && now - lastAccessAtMs >= idleWindowMs;
        }

        private boolean isIdle() {
            return outstandingLeases.get() == 0;
        }

        private long lastAccessAtMs() {
            return lastAccessAtMs;
        }
    }
}
