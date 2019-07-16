package com.games.sixstonekalah.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameStatusResponseDto {
    private String id;
    private String uri;
    private Map<String, Integer> status;
    private String nextHand;
    private String lastHand;
    private String playerWon;
}
