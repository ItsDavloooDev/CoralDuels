package dev.itsdavlooo.coralduels.domain.reward;

public record Reward(
        RewardType type,
        int weight,
        String value,
        String message,
        boolean silent
) {
    public Reward(RewardType type, int weight, String value) {
        this(type, weight, value, "", false);
    }
}