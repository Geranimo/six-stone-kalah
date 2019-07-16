package com.games.sixstonekalah.service;

import com.games.sixstonekalah.model.Game;
import com.google.common.collect.Range;
import com.games.sixstonekalah.controllers.GamesController;
import com.games.sixstonekalah.exceptions.GameNotFoundException;
import com.games.sixstonekalah.exceptions.InvalidMoveException;
import com.games.sixstonekalah.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

/**
 * Kalah Game service for Two Players and 6 Stone per pit
 */
@Service
public class KalahGameService {

    private static final Logger LOG = LoggerFactory.getLogger(GamesController.class);

    private static final int EMPTY = 0;
    private static final int LAST_SEED = 1;

    private static final int PLAYER_1 = 1;
    private static final int PLAYER_2 = 2;

    private static final int NUMBER_OF_PLAYERS = 2;
    private static final int NUMBER_OF_KALAHS = NUMBER_OF_PLAYERS;

    private static final int NUMBER_OF_PITS = 6;
    private static final int NUMBER_OF_SEEDS_PER_PIT = 6;

    private static final int FIRST_PIT_INDEX = 1;
    private static final int LAST_PIT_INDEX = (NUMBER_OF_PITS * NUMBER_OF_PLAYERS) + NUMBER_OF_KALAHS;

    private static final int PLAYER_1_KALAH_INDEX = NUMBER_OF_PITS + 1;
    private static final int PLAYER_2_KALAH_INDEX = LAST_PIT_INDEX;

    private static final Range<Integer> PLAYER_1_PIT_RANGE = Range.closed(FIRST_PIT_INDEX, PLAYER_1_KALAH_INDEX - 1);
    private static final Range<Integer> PLAYER_2_PIT_RANGE = Range.closed(PLAYER_1_KALAH_INDEX + 1, PLAYER_2_KALAH_INDEX - 1);

    private GameRepository gameRepository;

    public KalahGameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game creteNewGame(String id, String location) {
        HashMap<String, Integer> pitsSeedsMap = initializePitsWithSeeds();
        Game newGame = Game.builder()
                .id(id)
                .uri(location)
                .pitsSeedsMap(new TreeMap<>(pitsSeedsMap))
                .nextToPlay(1)
                .build();
        return gameRepository.save(newGame);
    }


    public Game getGame(String gameId) throws GameNotFoundException {
        return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException("Game with id doesnt exist"));
    }

    public Game move(String gameId, int pitId) throws InvalidMoveException, GameNotFoundException {
        Game game = this.getGame(gameId);
        final int activePlayer = game.getNextToPlay();
        Map<String, Integer> pitSeedMap = game.getPitsSeedsMap();

        if (game.isGameOver()) {
            return game;
        }
        if (checkIfTheGameEnded(game.getPitsSeedsMap())) {
            //calculate the kalah points
            HashMap<String, Integer> player1Moved = movePlayerSeedsToHisHouse(game.getPitsSeedsMap(), PLAYER_1);
            HashMap<String, Integer> player2Moved = movePlayerSeedsToHisHouse(player1Moved, PLAYER_2);
            game.setPitsSeedsMap(player2Moved);
            game.setPlayerWon(getPlayerStoreValue(PLAYER_1, player2Moved) > getPlayerStoreValue(PLAYER_2, player2Moved) ? PLAYER_1 : PLAYER_2);
            game.setLastPlayedBy(activePlayer);
            game.setBonusMove(false);
            game.setGameOver(true);
            gameRepository.save(game);
            return game;
        }

        checkIfMoveIsValidForThePitId(pitId, pitSeedMap);
        checkIfMoveIsValidForActivePlayer(pitId, activePlayer);

        int numberOfSeedsToSow = pitSeedMap.getOrDefault(String.valueOf(pitId), 0);
        int moveIndex = pitId;
        pitSeedMap.put(String.valueOf(moveIndex), EMPTY);
        boolean bonusMove = false;
        while (numberOfSeedsToSow != EMPTY) {
            moveIndex++;
            if (moveIndex == LAST_PIT_INDEX + 1) {
                moveIndex = FIRST_PIT_INDEX;
            }
            if (moveIndex != getOpponentKalaIndex(activePlayer)) {
                Integer currentPitValue = pitSeedMap.get(String.valueOf(moveIndex));
                if (numberOfSeedsToSow == LAST_SEED) {
                    //GET A BONUS MOVE
                    if (moveIndex == getActivePlayersStoreIndex(activePlayer)) {
                        bonusMove = true;
                        pitSeedMap.put(String.valueOf(moveIndex), ++currentPitValue);
                    } else if (currentPitValue == 0 && isCurrentIndexOnActivePlayersSide(moveIndex, activePlayer)) {
                        // CAPTURE OPPONENTS SEEDS
                        pitSeedMap = capturesOppositePitsSeedIntoOwnKalah(activePlayer, pitSeedMap, numberOfSeedsToSow, moveIndex);
                    } else {
                        pitSeedMap.put(String.valueOf(moveIndex), ++currentPitValue);
                    }
                } else {
                    pitSeedMap.put(String.valueOf(moveIndex), ++currentPitValue);
                }
                numberOfSeedsToSow--;
            }
        }
        game.setPitsSeedsMap(new TreeMap<>(pitSeedMap));
        game.setLastPlayedBy(activePlayer);
        game.setBonusMove(bonusMove);
        game.setNextToPlay(getNextToPlay(bonusMove, activePlayer));
        gameRepository.save(game);
        return game;
    }

    private Integer getNextToPlay(boolean bonusMove, int activePlayer) {
        Integer nextPlayer = null;
        if (activePlayer == PLAYER_1) {
            nextPlayer = PLAYER_2;
        }
        if (activePlayer == PLAYER_2) {
            nextPlayer = PLAYER_1;
        }
        return bonusMove ? activePlayer : nextPlayer;
    }

    private Map<String, Integer> capturesOppositePitsSeedIntoOwnKalah(int activePlayer,
                                                                      Map<String, Integer> pitsSeedsMap,
                                                                      int numberOfSeeds,
                                                                      int moveIndex) {
        Map<String, Integer> result = new HashMap<>(pitsSeedsMap);
        final int oppositePitId = LAST_PIT_INDEX - moveIndex;
        final int opponentsHouseSeedsCount = result.get(String.valueOf(oppositePitId));
        //remove the seeds from the opponents pit
        result.put(String.valueOf(oppositePitId), EMPTY);
        final int activePlayersStoreIndex = getActivePlayersStoreIndex(activePlayer);
        final int activePlayersKalahSeedsCount = result.get(String.valueOf(activePlayersStoreIndex));
        // put the seeds in to the players Kalah
        result.put(String.valueOf(activePlayersStoreIndex), opponentsHouseSeedsCount + numberOfSeeds + activePlayersKalahSeedsCount);
        return result;
    }

    private HashMap<String, Integer> movePlayerSeedsToHisHouse(Map<String, Integer> pitsSeedMap, int playerId) {
        HashMap<String, Integer> result = new HashMap<>(pitsSeedMap);
        Integer totalNumberOfSeedsInPits = getSumOfSeedsInPlayersPit(result, playerId);
        final int activePlayersStoreIndex = getActivePlayersStoreIndex(playerId);
        final int numberOfSeedsInKalah = result.get(String.valueOf(activePlayersStoreIndex));
        final int numberOfSeedsTobePlacedInKalah = totalNumberOfSeedsInPits + numberOfSeedsInKalah;
        result.put(String.valueOf(activePlayersStoreIndex), numberOfSeedsTobePlacedInKalah);
        return cleanSeedsInThePit(playerId, result);
    }

    private int getPlayerStoreValue(int activePlayer, Map<String, Integer> pitsSeedMap) {
        return pitsSeedMap.get(String.valueOf(getActivePlayersStoreIndex(activePlayer)));
    }

    private int getSumOfSeedsInPlayersPit(Map<String, Integer> pitsSeedMap, int getSumOfSeedsInPlayersPit) {
        Range<Integer> playerPitRange = getPlayerPitsRange(getSumOfSeedsInPlayersPit);
        return pitsSeedMap.keySet()
                .stream()
                .filter(s -> playerPitRange.contains(Integer.valueOf(s)))
                .map(pitsSeedMap::get)
                .reduce(0, Integer::sum);
    }

    private HashMap<String, Integer> cleanSeedsInThePit(int activePlayer, Map<String, Integer> pitsSeedMap) {
        HashMap<String, Integer> result = new HashMap<>(pitsSeedMap);
        result.keySet()
                .stream()
                .filter(s -> getPlayerPitsRange(activePlayer).contains(Integer.valueOf(s)))
                .forEach(s -> result.put(s, EMPTY));
        return result;
    }

    private boolean checkIfTheGameEnded(Map<String, Integer> pitsSeedsMap) {
        return getSumOfSeedsInPlayerPit(PLAYER_1, pitsSeedsMap) == 0
                || getSumOfSeedsInPlayerPit(PLAYER_2, pitsSeedsMap) == 0;
    }

    private int getSumOfSeedsInPlayerPit(int playerId, Map<String, Integer> pitsSeedsMap) {
        return pitsSeedsMap.keySet()
                .stream()
                .filter(s -> getPlayerPitsRange(playerId).contains(Integer.valueOf(s)))
                .map(pitsSeedsMap::get)
                .reduce(0, Integer::sum);
    }

    private Range<Integer> getPlayerPitsRange(int playerId) {
        return playerId == PLAYER_1 ? PLAYER_1_PIT_RANGE : PLAYER_2_PIT_RANGE;
    }

    private int getActivePlayersStoreIndex(int activePlayer) {
        return activePlayer == PLAYER_1 ? PLAYER_1_KALAH_INDEX : PLAYER_2_KALAH_INDEX;
    }

    private boolean isCurrentIndexOnActivePlayersSide(int index, int activePlayer) {
        return getPlayerPitsRange(activePlayer).contains(index);
    }

    private int getOpponentKalaIndex(int activePlayer) {
        return activePlayer == PLAYER_1 ? PLAYER_2_KALAH_INDEX : PLAYER_1_KALAH_INDEX;
    }

    private void checkIfMoveIsValidForThePitId(int pitIndex, Map<String, Integer> pitsSeedsMap) throws InvalidMoveException {
        if (pitIndex == PLAYER_1_KALAH_INDEX || pitIndex == PLAYER_2_KALAH_INDEX) {
            throw new InvalidMoveException("Move cannot be done from the house");
        }
        if (!PLAYER_1_PIT_RANGE.contains(pitIndex) && !PLAYER_2_PIT_RANGE.contains(pitIndex)) {
            throw new InvalidMoveException("Enter a valid pit id");
        }
        final int pitValue = pitsSeedsMap.get(String.valueOf(pitIndex));
        if (pitValue == EMPTY) {
            throw new InvalidMoveException("Move cannot be started from an empty pit");
        }
    }

    private void checkIfMoveIsValidForActivePlayer(int pitId, int activePlayer) throws InvalidMoveException {
        if (!getPlayerPitsRange(activePlayer).contains(pitId)) {
            throw new InvalidMoveException("Move from other players pit is not possible");
        }
    }

    private static HashMap<String, Integer> initializePitsWithSeeds() {
        HashMap<String, Integer> pitsSeedsMap = new HashMap<>();
        IntStream.rangeClosed(FIRST_PIT_INDEX, LAST_PIT_INDEX).forEach(value -> {
                    if (value == PLAYER_1_KALAH_INDEX || value == PLAYER_2_KALAH_INDEX) {
                        pitsSeedsMap.put(String.valueOf(value), EMPTY);
                    } else {
                        pitsSeedsMap.put(String.valueOf(value), NUMBER_OF_SEEDS_PER_PIT);
                    }
                }
        );
        return pitsSeedsMap;
    }
}
