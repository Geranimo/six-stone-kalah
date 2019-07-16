package com.games.sixstonekalah.service;

import com.games.sixstonekalah.exceptions.GameNotFoundException;
import com.games.sixstonekalah.model.Game;
import com.games.sixstonekalah.exceptions.InvalidMoveException;
import com.games.sixstonekalah.repository.GameRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class KalahGameServiceMovesTest {

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    private static class TestData {
        int[] currentState;
        int moveFromPit;
        Integer lastPlayedBy;
        Integer nexToPlay;
        boolean withBonusMove;
        boolean gotBonusMove;
        boolean gameOver;
        int playerWon;
        int[] expectedState;
    }

    @Parameters
    public static Collection<TestData> data() {
        return Arrays.asList(
                //random move
                new TestData(new int[]{6, 6, 6, 6, 6, 6, 0, 6, 6, 6, 6, 6, 6, 0}, 5, null,1, false, false, false, 0, new int[]{6, 6, 6, 6, 0, 7, 1, 7, 7, 7, 7, 6, 6, 0}),
                new TestData(new int[]{6, 6, 6, 6, 0, 7, 1, 7, 7, 7, 7, 6, 6, 0}, 8, 1,2, false, false, false, 0, new int[]{7, 6, 6, 6, 0, 7, 1, 0, 8, 8, 8, 7, 7, 1}),
                new TestData(new int[]{7, 6, 6, 6, 0, 7, 1, 0, 8, 8, 8, 7, 7, 1}, 6, 2, 1,false, false, false, 0, new int[]{7, 6, 6, 6, 0, 0, 2, 1, 9, 9, 9, 8, 8, 1}),
                new TestData(new int[]{7, 6, 6, 6, 0, 0, 2, 1, 9, 9, 9, 8, 8, 1}, 8, 1, 2,false, false, false, 0, new int[]{7, 6, 6, 6, 0, 0, 2, 0, 10, 9, 9, 8, 8, 1}),

                // lastPlayedBy one move that results in a bonus round
                new TestData(new int[]{6, 6, 6, 6, 6, 6, 0, 6, 6, 6, 6, 6, 6, 0}, 1, null,1, false, true, false, 0, new int[]{0, 7, 7, 7, 7, 7, 1, 6, 6, 6, 6, 6, 6, 0}),
                // lastPlayedBy make a move that results in a bonus round
                new TestData(new int[]{7, 6, 9, 0, 0, 2, 6, 0, 15, 10, 9, 9, 1, 1}, 13, 1, 2,false, true, false, 0, new int[]{7, 6, 9, 0, 0, 2, 6, 0, 15, 10, 9, 9, 0, 2}),

                //lastPlayedBy 1 places last seeds in his empty pit and captures opponents pits seeds
                new TestData(new int[]{9, 0, 10, 0, 1, 9, 3, 0, 6, 10, 9, 9, 9, 1}, 6, null, 1,false, false, false, 0, new int[]{10, 0, 10, 0, 1, 0, 15, 1, 7, 11, 10, 0, 10, 1}),
                //lastPlayedBy two places last seeds in his empty pit and captures opponents pits seeds
                new TestData(new int[]{8, 7, 7, 1, 1, 2, 4, 0, 14, 10, 9, 10, 8, 2}, 13, 1, 2,false, false, false, 0, new int[]{9, 8, 8, 2, 2, 0, 4, 0, 14, 10, 9, 10, 0, 7}),

                // lastPlayedBy 1 place stone in his kalah and skips opponent kalah
                new TestData(new int[]{7, 6, 6, 0, 1, 9, 3, 0, 6, 10, 9, 9, 9, 1}, 6, null, 1,false, false, false, 0, new int[]{8, 7, 6, 0, 1, 0, 4, 1, 7, 11, 10, 10, 10, 1}),
                //lastPlayedBy 2 place stone in his kalah and skips opponent kalah
                new TestData(new int[]{7, 6, 6, 0, 1, 1, 3, 1, 10, 11, 9, 9, 9, 1}, 10, 1, 2,false, false, false, 0, new int[]{8, 7, 7, 1, 2, 2, 3, 2, 10, 0, 10, 10, 10, 2}),

                //lastPlayedBy two won
                new TestData(new int[]{0, 0, 0, 0, 0, 0, 10, 6, 6, 6, 6, 6, 6, 26}, 5, null, 1,false, false, true, 2, new int[]{0, 0, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 62}),
                //lastPlayedBy one won
                new TestData(new int[]{6, 6, 6, 6, 6, 6, 26, 0, 0, 0, 0, 0, 0, 10}, 9, 1, 2,false, false, true, 1, new int[]{0, 0, 0, 0, 0, 0, 62, 0, 0, 0, 0, 0, 0, 10})
        );
    }

    @Parameterized.Parameter
    public TestData testData;

    private GameRepository gameRepository;

    private KalahGameService kalahGameService;

    @Test
    public void testMoveWithExpectations() throws InvalidMoveException, GameNotFoundException {
        gameRepository = Mockito.mock(GameRepository.class);
        kalahGameService = new KalahGameService(gameRepository);
        Game game = Game.builder()
                .id(UUID.randomUUID().toString())
                .uri("URI")
                .pitsSeedsMap(getPitsSeedMap(testData.getCurrentState()))
                .bonusMove(testData.isWithBonusMove())
                .lastPlayedBy(testData.getLastPlayedBy())
                .nextToPlay(testData.getNexToPlay())
                .build();
        Mockito.when(gameRepository.findById(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(game));
        Game result = kalahGameService.move(game.getId(), testData.getMoveFromPit());
        int[] expectedValuesAtIndex = testData.getExpectedState();

        for (int i = 0; i < testData.getExpectedState().length; i++) {
            int resultedValue = result.getPitsSeedsMap().get(String.valueOf(i + 1));
            assertThat(resultedValue, is(expectedValuesAtIndex[i]));
        }
        assertThat(result.isBonusMove(), is(testData.isGotBonusMove()));
        assertThat(result.isGameOver(), is(testData.isGameOver()));
        assertThat(result.getPlayerWon(), is(testData.getPlayerWon()));
    }

    private HashMap<String, Integer> getPitsSeedMap(int[] currentStateOfTheGame) {
        HashMap<String, Integer> currentPitsSeedsMap = new HashMap<>();
        for (int k = 0; k < currentStateOfTheGame.length; k++) {
            currentPitsSeedsMap.put(String.valueOf(k + 1), currentStateOfTheGame[k]);
        }
        return currentPitsSeedsMap;
    }
}