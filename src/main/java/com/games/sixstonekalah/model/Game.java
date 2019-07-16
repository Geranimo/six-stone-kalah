package com.games.sixstonekalah.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.lang.Nullable;

import java.util.Map;

@RedisHash("Game")
@Getter
@Setter
@Builder
public class Game {
    private String id;
    private String uri;
    @Nullable
    private Map<String, Integer> pitsSeedsMap;
    @Nullable
    private Integer lastPlayedBy;
    private Integer nextToPlay;
    private boolean bonusMove;
    private int playerWon;
    private boolean gameOver;
}
