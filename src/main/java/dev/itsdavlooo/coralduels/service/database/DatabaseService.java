package dev.itsdavlooo.coralduels.service.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public final class DatabaseService {

    private final HikariDataSource dataSource;
    private final CoralDuelsPlugin plugin;

    public DatabaseService(CoralDuelsPlugin plugin, ConfigService config) {
        this.plugin = plugin;
        this.dataSource = createDataSource(config.getConfig());
    }

    private HikariDataSource createDataSource(FileConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                config.getString("database.host", "localhost"),
                config.getInt("database.port", 3306),
                config.getString("database.database", "coralduels"),
                config.getBoolean("database.ssl", false)));
        hikariConfig.setUsername(config.getString("database.username", "root"));
        hikariConfig.setPassword(config.getString("database.password", ""));
        hikariConfig.setMaximumPoolSize(config.getInt("database.pool-size", 10));
        hikariConfig.setPoolName("CoralDuels-Pool");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(hikariConfig);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> executeAsync(java.util.function.Consumer<Connection> consumer) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                consumer.accept(conn);
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, runnable -> plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run()));
    }

    public <T> CompletableFuture<T> queryAsync(java.util.function.Function<Connection, T> mapper) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return mapper.apply(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, runnable -> plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run()));
    }

    public void close() {
        dataSource.close();
    }

    public boolean isConnected() {
        return !dataSource.isClosed();
    }
}