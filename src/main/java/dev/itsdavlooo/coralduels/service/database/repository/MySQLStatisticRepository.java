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
        }).exceptionally(e -> {
            plugin.getLogger().severe("Table initialization failed: " + e.getMessage());
            return null;
        });
    }

    @Override
    public CompletableFuture<Optional<PlayerStatistic>> findByUuid(UUID uuid) {
        return databaseService.queryAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM coralduels_stats WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error finding stat by UUID: " + e.getMessage());
            }
            return Optional.<PlayerStatistic>empty();
        });
    }

    @Override
    public CompletableFuture<PlayerStatistic> createOrUpdate(PlayerStatistic statistic) {
        return databaseService.executeAsync(conn -> {
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
                throw new RuntimeException(e);
            }
        }).thenApply(v -> statistic);
    }

    @Override
    public CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String category, int limit) {
        String column = switch (category.toLowerCase()) {
            case "wins" -> "duels_won";
            case "played" -> "duels_played";
            case "losses" -> "duels_lost";
            case "streak" -> "current_streak";
            case "kills" -> "kills";
            case "deaths" -> "deaths";
            default -> "elo_rating";
        };
        return databaseService.queryAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT uuid, username, duels_played, duels_won, duels_lost, duels_draw, kills, deaths, elo_rating " +
                            "FROM coralduels_stats ORDER BY " + column + " DESC, username ASC LIMIT ?")) {
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
        });
    }

    @Override
    public CompletableFuture<Void> incrementStat(UUID uuid, String column, int amount) {
        String allowedColumn = switch (column) {
            case "duels_won", "duels_lost", "duels_draw", "duels_played",
                 "kills", "deaths", "current_streak", "best_streak",
                 "total_damage_dealt", "total_damage_taken" -> column;
            default -> throw new IllegalArgumentException("Invalid column: " + column);
        };
        return databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE coralduels_stats SET " + allowedColumn + " = " + allowedColumn + " + ? WHERE uuid = ?")) {
                stmt.setInt(1, amount);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error incrementing stat: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateElo(UUID uuid, int newElo) {
        return databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE coralduels_stats SET elo_rating = ?, best_streak = GREATEST(best_streak, current_streak) WHERE uuid = ?")) {
                stmt.setInt(1, newElo);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating ELO: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateStreak(UUID uuid, int streak) {
        return databaseService.executeAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE coralduels_stats SET current_streak = ?, best_streak = GREATEST(best_streak, ?) WHERE uuid = ?")) {
                stmt.setInt(1, streak);
                stmt.setInt(2, streak);
                stmt.setString(3, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating streak: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> recordDuelHistory(DuelHistoryRecord record) {
        return databaseService.executeAsync(conn -> {
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
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> recordMatchResult(UUID winner, UUID loser, String kitName, int winnerDamage, int loserDamage) {
        return databaseService.executeAsync(conn -> {
            try {
                conn.setAutoCommit(false);
                ensurePlayerRow(conn, winner);
                ensurePlayerRow(conn, loser);
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE coralduels_stats SET " +
                                "duels_played = duels_played + 1, " +
                                "duels_won = duels_won + 1, " +
                                "current_streak = current_streak + 1, " +
                                "best_streak = GREATEST(best_streak, current_streak + 1), " +
                                "kills = kills + 1, " +
                                "total_damage_dealt = total_damage_dealt + ?, " +
                                "total_damage_taken = total_damage_taken + ?, " +
                                "favorite_kit = ?, " +
                                "last_duel_at = ? " +
                                "WHERE uuid = ?")) {
                    stmt.setInt(1, winnerDamage);
                    stmt.setInt(2, loserDamage);
                    stmt.setString(3, kitName);
                    stmt.setLong(4, System.currentTimeMillis());
                    stmt.setString(5, winner.toString());
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE coralduels_stats SET " +
                                "duels_played = duels_played + 1, " +
                                "duels_lost = duels_lost + 1, " +
                                "current_streak = 0, " +
                                "deaths = deaths + 1, " +
                                "total_damage_dealt = total_damage_dealt + ?, " +
                                "total_damage_taken = total_damage_taken + ?, " +
                                "last_duel_at = ? " +
                                "WHERE uuid = ?")) {
                    stmt.setInt(1, loserDamage);
                    stmt.setInt(2, winnerDamage);
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.setString(4, loser.toString());
                    stmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    plugin.getLogger().severe("Rollback failed: " + ex.getMessage());
                }
                plugin.getLogger().severe("Error recording match result: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to restore autoCommit: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> recordDrawResult(UUID player1, UUID player2, String kitName, int damage1, int damage2) {
        return databaseService.executeAsync(conn -> {
            try {
                conn.setAutoCommit(false);
                ensurePlayerRow(conn, player1);
                ensurePlayerRow(conn, player2);
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE coralduels_stats SET " +
                                "duels_played = duels_played + 1, " +
                                "duels_draw = duels_draw + 1, " +
                                "current_streak = 0, " +
                                "total_damage_dealt = total_damage_dealt + ?, " +
                                "total_damage_taken = total_damage_taken + ?, " +
                                "last_duel_at = ? " +
                                "WHERE uuid = ?")) {
                    stmt.setInt(1, damage1);
                    stmt.setInt(2, damage2);
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.setString(4, player1.toString());
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE coralduels_stats SET " +
                                "duels_played = duels_played + 1, " +
                                "duels_draw = duels_draw + 1, " +
                                "current_streak = 0, " +
                                "total_damage_dealt = total_damage_dealt + ?, " +
                                "total_damage_taken = total_damage_taken + ?, " +
                                "last_duel_at = ? " +
                                "WHERE uuid = ?")) {
                    stmt.setInt(1, damage2);
                    stmt.setInt(2, damage1);
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.setString(4, player2.toString());
                    stmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    plugin.getLogger().severe("Rollback failed: " + ex.getMessage());
                }
                plugin.getLogger().severe("Error recording draw result: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to restore autoCommit: " + e.getMessage());
                }
            }
        });
    }

    private void ensurePlayerRow(Connection conn, UUID uuid) throws SQLException {
        String username = org.bukkit.Bukkit.getOfflinePlayer(uuid).getName();
        if (username == null || username.isEmpty()) {
            username = uuid.toString().substring(0, 16);
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT IGNORE INTO coralduels_stats (uuid, username) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
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