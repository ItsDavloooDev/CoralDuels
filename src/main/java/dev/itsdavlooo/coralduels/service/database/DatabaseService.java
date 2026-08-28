package dev.itsdavlooo.coralduels.service.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public void executeAsync(Consumer<Connection> consumer) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try (Connection conn = getConnection()) {
                consumer.accept(conn);
            } catch (SQLException e) {
                plugin.getLogger().severe("Database error: " + e.getMessage());
            }
        });
    }

    public <T> void queryAsync(String sql, Function<Connection, T> mapper, Consumer<T> callback) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try (Connection conn = getConnection()) {
                T result = mapper.apply(conn);
                plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> callback.accept(result));
            } catch (SQLException e) {
                plugin.getLogger().severe("Database query error: " + e.getMessage());
            }
        });
    }

    public void close() {
        dataSource.close();
    }

    public boolean isConnected() {
        return !dataSource.isClosed();
    }
}