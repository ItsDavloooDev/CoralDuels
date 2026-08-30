package dev.itsdavlooo.coralduels.service.database.repository;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.statistic.LeaderboardEntry;
import dev.itsdavlooo.coralduels.domain.statistic.PlayerStatistic;
import dev.itsdavlooo.coralduels.service.database.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MySQLStatisticRepository implements StatisticRepository {

    private final DatabaseService databaseService;
    private final CoralDuelsPlugin plugin;

    public MySQLStatisticRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.plugin = CoralDuelsPlugin.getInstance();
        initializeTables();
    }

    private void initializeTables() {
        databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS coralduels_stats (" +
                            "uuid CHAR(36) PRIMARY KEY," +
                            "username VARCHAR(16) NOT NULL," +
                            "duels_played INT DEFAULT 0," +
                            "duels_won INT DEFAULT 0," +
                            "duels_lost INT DEFAULT 0," +
                            "duels_draw INT DEFAULT 0," +
                            "kills INT DEFAULT 0," +
                            "deaths INT DEFAULT 0," +
                            "current_streak INT DEFAULT 0," +
                            "best_streak INT DEFAULT 0," +
                            "elo_rating INT DEFAULT 1000," +
                            "total_damage_dealt BIGINT DEFAULT 0," +
                            "total_damage_taken BIGINT DEFAULT 0," +
                            "favorite_kit VARCHAR(32) DEFAULT ''," +
                            "last_duel_at BIGINT DEFAULT 0," +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                            ")")) {
                stmt.execute();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to create stats table: " + e.getMessage());
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS coralduels_history (" +
                            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "duel_id CHAR(36) NOT NULL," +
                            "challenger CHAR(36) NOT NULL," +
                            "target CHAR(36) NOT NULL," +
                            "winner CHAR(36)," +
                            "kit_name VARCHAR(32) NOT NULL," +
                            "arena_name VARCHAR(64) NOT NULL," +
                            "state VARCHAR(16) NOT NULL," +
                            "duration_ticks INT NOT NULL," +
                            "challenger_damage INT DEFAULT 0," +
                            "target_damage INT DEFAULT 0," +
                            "started_at BIGINT NOT NULL," +
                            "ended_at BIGINT NOT NULL," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "INDEX idx_challenger (challenger)," +
                            "INDEX idx_target (target)," +
                            "INDEX idx_winner (winner)," +
                            "INDEX idx_started_at (started_at)" +
                            ")")) {
                stmt.execute();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to create history table: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Optional<PlayerStatistic>> findByUuid(UUID uuid) {
        CompletableFuture<Optional<PlayerStatistic>> future = new CompletableFuture<>();
        databaseService.queryAsync("SELECT * FROM coralduels_stats WHERE uuid = ?", conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM coralduels_stats WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSet(rs);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error finding stat by UUID: " + e.getMessage());
            }
            return null;
        }, future::complete);
        return future;
    }

    @Override
    public CompletableFuture<PlayerStatistic> createOrUpdate(PlayerStatistic statistic) {
        CompletableFuture<PlayerStatistic> future = new CompletableFuture<>();
        databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO coralduels_stats (uuid, username, duels_played, duels_won, duels_lost, duels_draw, " +
                            "kills, deaths, current_streak, best_streak, elo_rating, total_damage_dealt, total_damage_taken, " +
                            "favorite_kit, last_duel_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "username = VALUES(username), duels_played = VALUES(duels_played), duels_won = VALUES(duels_won), " +
                            "duels_lost = VALUES(duels_lost), duels_draw = VALUES(duels_draw), kills = VALUES(kills), " +
                            "deaths = VALUES(deaths), current_streak = VALUES(current_streak), best_streak = VALUES(best_streak), " +
                            "elo_rating = VALUES(elo_rating), total_damage_dealt = VALUES(total_damage_dealt), " +
                            "total_damage_taken = VALUES(total_damage_taken), favorite_kit = VALUES(favorite_kit), " +
                            "last_duel_at = VALUES(last_duel_at)")) {
                setStatisticParams(stmt, statistic);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error creating/updating statistic: " + e.getMessage());
            }
        }, v -> future.complete(statistic));
        return future;
    }

    @Override
    public CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String category, int limit) {
        CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
        String column = switch (category.toLowerCase()) {
            case "wins" -> "duels_won";
            case "played" -> "duels_played";
            case "losses" -> "duels_lost";
            default -> "elo_rating";
        };
        databaseService.queryAsync(
                "SELECT uuid, username, " + column + " as value FROM coralduels_stats ORDER BY " + column + " DESC LIMIT ?",
                conn -> {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT uuid, username, " + column + " as value FROM coralduels_stats ORDER BY " + column + " DESC LIMIT ?")) {
                        stmt.setInt(1, limit);
                        List<LeaderboardEntry> entries = new ArrayList<>();
                        try (ResultSet rs = stmt.executeQuery()) {
                            int pos = 1;
                            while (rs.next()) {
                                entries.add(new LeaderboardEntry(
                                        pos++,
                                        UUID.fromString(rs.getString("uuid")),
                                        rs.getString("username"),
                                        rs.getInt("duels_won"),
                                        rs.getInt("duels_lost"),
                                        rs.getInt("elo_rating"),
                                        ""
                                ));
                            }
                        }
                        return entries;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Error getting leaderboard: " + e.getMessage());
                        return List.of();
                    }
                },
                future::complete
        );
        return future;
    }

    @Override
    public CompletableFuture<Void> incrementStat(UUID uuid, String column, int amount) {
        String allowedColumn = switch (column) {
            case "duels_won", "duels_lost", "duels_draw", "duels_played",
                 "kills", "deaths", "current_streak", "best_streak",
                 "total_damage_dealt", "total_damage_taken" -> column;
            default -> throw new IllegalArgumentException("Invalid column: " + column);
        };
        CompletableFuture<Void> future = new CompletableFuture<>();
        databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE coralduels_stats SET " + allowedColumn + " = " + allowedColumn + " + ? WHERE uuid = ?")) {
                stmt.setInt(1, amount);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error incrementing stat: " + e.getMessage());
            }
        }, v -> future.complete(null));
        return future;
    }

    @Override
    public CompletableFuture<Void> updateElo(UUID uuid, int newElo) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE coralduels_stats SET elo_rating = ?, best_streak = GREATEST(best_streak, current_streak) WHERE uuid = ?")) {
                stmt.setInt(1, newElo);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating ELO: " + e.getMessage());
            }
        }, v -> future.complete(null));
        return future;
    }

    @Override
    public CompletableFuture<Void> updateStreak(UUID uuid, int streak) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE coralduels_stats SET current_streak = ?, best_streak = GREATEST(best_streak, ?) WHERE uuid = ?")) {
                stmt.setInt(1, streak);
                stmt.setInt(2, streak);
                stmt.setString(3, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating streak: " + e.getMessage());
            }
        }, v -> future.complete(null));
        return future;
    }

    @Override
    public CompletableFuture<Void> recordDuelHistory(DuelHistoryRecord record) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO coralduels_history (duel_id, challenger, target, winner, kit_name, arena_name, " +
                            "state, duration_ticks, challenger_damage, target_damage, started_at, ended_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, record.duelId().toString());
                stmt.setString(2, record.challenger().toString());
                stmt.setString(3, record.target().toString());
                stmt.setString(4, record.winner() != null ? record.winner().toString() : null);
                stmt.setString(5, record.kitName());
                stmt.setString(6, record.arenaName());
                stmt.setString(7, record.state());
                stmt.setInt(8, record.durationTicks());
                stmt.setInt(9, record.challengerDamage());
                stmt.setInt(10, record.targetDamage());
                stmt.setLong(11, record.startedAt());
                stmt.setLong(12, record.endedAt());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error recording duel history: " + e.getMessage());
            }
        }, v -> future.complete(null));
        return future;
    }

    private PlayerStatistic mapResultSet(ResultSet rs) throws SQLException {
        return new PlayerStatistic(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("username"),
                rs.getInt("duels_played"),
                rs.getInt("duels_won"),
                rs.getInt("duels_lost"),
                rs.getInt("duels_draw"),
                rs.getInt("kills"),
                rs.getInt("deaths"),
                rs.getInt("current_streak"),
                rs.getInt("best_streak"),
                rs.getInt("elo_rating"),
                rs.getLong("total_damage_dealt"),
                rs.getLong("total_damage_taken"),
                rs.getString("favorite_kit"),
                rs.getLong("last_duel_at")
        );
    }

    private void setStatisticParams(PreparedStatement stmt, PlayerStatistic stat) throws SQLException {
        stmt.setString(1, stat.uuid().toString());
        stmt.setString(2, stat.username());
        stmt.setInt(3, stat.duelsPlayed());
        stmt.setInt(4, stat.duelsWon());
        stmt.setInt(5, stat.duelsLost());
        stmt.setInt(6, stat.duelsDraw());
        stmt.setInt(7, stat.kills());
        stmt.setInt(8, stat.deaths());
        stmt.setInt(9, stat.currentStreak());
        stmt.setInt(10, stat.bestStreak());
        stmt.setInt(11, stat.eloRating());
        stmt.setLong(12, stat.totalDamageDealt());
        stmt.setLong(13, stat.totalDamageTaken());
        stmt.setString(14, stat.favoriteKit());
        stmt.setLong(15, stat.lastDuelAt());
    }
}