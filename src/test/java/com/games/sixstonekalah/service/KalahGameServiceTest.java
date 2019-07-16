package com.games.sixstonekalah.service;

import com.games.sixstonekalah.exceptions.GameNotFoundException;
import com.games.sixstonekalah.model.Game;
import com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers;
import com.games.sixstonekalah.exceptions.InvalidMoveException;
import com.games.sixstonekalah.repository.GameRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;

public class KalahGameServiceTest {

    private GameRepository gameRepository;
    private KalahGameService kalahGameService;

    @Before
    public void setUp() {
        gameRepository = Mockito.mock(GameRepository.class);
        kalahGameService = new KalahGameService(gameRepository);
    }

    @Test
    public void testWhenTyringToStartMovingFromOpponentPitThrowsException() throws InvalidMoveException, GameNotFoundException {
        int[] initialState = {0, 7, 7, 7, 7, 7, 1, 6, 6, 6, 6, 6, 6, 0};
        Map<String, Integer> pitsSeedsMap = new HashMap<>();
        for (int i = 0; i < initialState.length; i++) {
            pitsSeedsMap.put(String.valueOf(i + 1), initialState[i]);
        }
        Game game = Game.builder()
                .id(UUID.randomUUID().toString())
                .uri("URI")
                .pitsSeedsMap(pitsSeedsMap)
                .bonusMove(true)
                .lastPlayedBy(1)
                .nextToPlay(1)
                .build();

        Mockito.when(gameRepository.findById(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(game));

        int moveFromOpponentPits = 8;
        catchException(kalahGameService).move(game.getId(), moveFromOpponentPits);
        assertThat(caughtException(), allOf(
                instanceOf(InvalidMoveException.class),
                CatchExceptionHamcrestMatchers.hasMessage("Move from other players pit is not possible")
        ));
    }

    @Test
    public void testWhenTyringToTakeFromEmptyPitThrowsException() throws InvalidMoveException, GameNotFoundException {
        int[] initialState = {0, 7, 7, 7, 7, 7, 1, 6, 6, 6, 6, 6, 6, 0};
        Map<String, Integer> pitsSeedsMap = new HashMap<>();
        for (int i = 0; i < initialState.length; i++) {
            pitsSeedsMap.put(String.valueOf(i + 1), initialState[i]);
        }
        Game game = Game.builder()
                .id(UUID.randomUUID().toString())
                .uri("URI")
                .pitsSeedsMap(pitsSeedsMap)
                .bonusMove(true)
                .lastPlayedBy(1)
                .nextToPlay(1)
                .build();

        Mockito.when(gameRepository.findById(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(game));

        int moveFromAnEmptyPit = 1;
        catchException(kalahGameService).move(game.getId(), moveFromAnEmptyPit);
        assertThat(caughtException(), allOf(
                instanceOf(InvalidMoveException.class),
                CatchExceptionHamcrestMatchers.hasMessage("Move cannot be started from an empty pit")
        ));
    }

    @Test
    public void testWhenTyringToStartMovingFromKalahThrowsException() throws InvalidMoveException, GameNotFoundException {
        int[] initialState = {0, 7, 7, 7, 7, 7, 1, 6, 6, 6, 6, 6, 6, 0};
        Map<String, Integer> pitsSeedsMap = new HashMap<>();
        for (int i = 0; i < initialState.length; i++) {
            pitsSeedsMap.put(String.valueOf(i + 1), initialState[i]);
        }
        Game game = Game.builder()
                .id(UUID.randomUUID().toString())
                .uri("URI")
                .pitsSeedsMap(pitsSeedsMap)
                .bonusMove(true)
                .lastPlayedBy(2)
                .nextToPlay(1)
                .build();

        Mockito.when(gameRepository.findById(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(game));

        int startMovingFromKalah = 7;
        catchException(kalahGameService).move(game.getId(), startMovingFromKalah);
        assertThat(caughtException(), allOf(
                instanceOf(InvalidMoveException.class),
                CatchExceptionHamcrestMatchers.hasMessage("Move cannot be done from the house")
        ));

        startMovingFromKalah = 14;
        catchException(kalahGameService).move(game.getId(), startMovingFromKalah);
        assertThat(caughtException(), allOf(
                instanceOf(InvalidMoveException.class),
                CatchExceptionHamcrestMatchers.hasMessage("Move cannot be done from the house")
        ));
    }

    @Test
    public void testWhenAttemptToMoveOnInvalidPitThrowsException() throws InvalidMoveException, GameNotFoundException {
        int[] initialState = {0, 7, 7, 7, 7, 7, 1, 6, 6, 6, 6, 6, 6, 0};
        Map<String, Integer> pitsSeedsMap = new HashMap<>();
        for (int i = 0; i < initialState.length; i++) {
            pitsSeedsMap.put(String.valueOf(i + 1), initialState[i]);
        }
        Game game = Game.builder()
                .id(UUID.randomUUID().toString())
                .uri("URI")
                .pitsSeedsMap(pitsSeedsMap)
                .bonusMove(true)
                .lastPlayedBy(2)
                .nextToPlay(1)
                .build();

        Mockito.when(gameRepository.findById(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(game));

        int invalidPitId = 15;
        catchException(kalahGameService).move(game.getId(), invalidPitId);
        assertThat(caughtException(), allOf(
                instanceOf(InvalidMoveException.class),
                CatchExceptionHamcrestMatchers.hasMessage("Enter a valid pit id")
        ));
    }

}