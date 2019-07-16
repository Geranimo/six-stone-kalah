package com.games.sixstonekalah.controllers;

import com.games.sixstonekalah.exceptions.GameNotFoundException;
import com.games.sixstonekalah.model.Game;
import com.games.sixstonekalah.model.GameResponseDto;
import com.games.sixstonekalah.model.GameStatusResponseDto;
import com.games.sixstonekalah.service.KalahGameService;
import com.games.sixstonekalah.exceptions.InvalidMoveException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.constraints.NotBlank;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/games")
public class GamesController {

    private KalahGameService kalahGameService;

    public GamesController(KalahGameService kalahGameService) {
        this.kalahGameService = kalahGameService;
    }

    @GetMapping(path = "/{gameId}", produces = "application/json")
    public GameResponseDto getGame(@PathVariable("gameId") @NotBlank String gameId) throws GameNotFoundException {
        Game game = kalahGameService.getGame(gameId);
        return GameResponseDto.builder().id(game.getId()).uri(game.getUri()).build();
    }

    @PostMapping(produces = "application/json")
    public GameResponseDto createGame() {
        String gameId = UUID.randomUUID().toString();
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{gameId}")
                .buildAndExpand(gameId).toUri();
        Game newGame = kalahGameService.creteNewGame(gameId, location.toString());
        return GameResponseDto.builder().id(gameId).uri(location.toASCIIString()).build();
    }

    @PutMapping(path = "/{gameId}/pits/{pitId}")
    public GameStatusResponseDto makeAMove(@PathVariable("gameId") @NotBlank String gameId,
                                           @PathVariable("pitId") int pitId) throws InvalidMoveException, GameNotFoundException {
        Game game = kalahGameService.move(gameId, pitId);
        return GameStatusResponseDto.builder()
                .status(game.getPitsSeedsMap())
                .id(game.getId())
                .uri(game.getUri())
                .lastHand("Player " + game.getLastPlayedBy())
                .nextHand("Player " + game.getNextToPlay())
                .playerWon(game.getPlayerWon() != 0 ? "Player " + game.getPlayerWon() : "")
                .build();
    }

}
