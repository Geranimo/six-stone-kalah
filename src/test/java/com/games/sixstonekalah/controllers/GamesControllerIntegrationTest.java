package com.games.sixstonekalah.controllers;

import com.games.sixstonekalah.model.GameResponseDto;
import com.games.sixstonekalah.model.GameStatusResponseDto;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GamesControllerIntegrationTest {

    @Value("${local.server.port}")
    int port;

    final RestTemplate template = new RestTemplate();

    @Test
    public void testCreateNewGame() {
        ResponseEntity<GameResponseDto> newGame = template.postForEntity("http://localhost:" + port + "/games", null, GameResponseDto.class);
        assertThat(newGame.getStatusCode(),
                is(HttpStatus.OK));
        assertNotNull(newGame.getBody().getId());
        assertThat(newGame.getBody().getUri(), is("http://localhost:" + port + "/games/" + newGame.getBody().getId()));
    }

    @Test
    public void testGetCreatedGame() {
        ResponseEntity<GameResponseDto> newGame = template.postForEntity("http://localhost:" + port + "/games", null, GameResponseDto.class);
        ResponseEntity<GameResponseDto> getCreatedGame = template.getForEntity(newGame.getBody().getUri(), GameResponseDto.class);
        assertThat(getCreatedGame.getBody().getId(), is(newGame.getBody().getId()));
    }

    @Test
    public void testGetGameWithInvalidId() {
        ResponseEntity<GameResponseDto> newGame = template.postForEntity("http://localhost:" + port + "/games", null, GameResponseDto.class);
        String gameWithInvalidId = newGame.getBody().getUri() + 1234;
        try {
            template.getForEntity(gameWithInvalidId, GameResponseDto.class);
            assertTrue(false);
        } catch (HttpClientErrorException e) {
            assertThat(e.getRawStatusCode(), is(HttpStatus.NOT_FOUND.value()));
        }

    }

    @Test
    public void testEnterAnInvalidPitIdReturnsABadRequest() {
        String gamesEndpoint = "http://localhost:" + port + "/games";
        ResponseEntity<GameResponseDto> newGame = template.postForEntity(gamesEndpoint, null, GameResponseDto.class);
        String makeMoveOnPit = gamesEndpoint + "/" + newGame.getBody().getId() + "/pits/" + 15;
        try {
            template.exchange(makeMoveOnPit, HttpMethod.PUT, null, GameStatusResponseDto.class);
            assertTrue(false);
        } catch (HttpClientErrorException e) {
            assertThat(e.getRawStatusCode(), is(HttpStatus.BAD_REQUEST.value()));
        }

    }


    @Test
    public void testMakeAMoveInOneOfThePits() {
        String gamesEndpoint = "http://localhost:" + port + "/games";
        ResponseEntity<GameResponseDto> newGame = template.postForEntity(gamesEndpoint, null, GameResponseDto.class);
        String makeMoveOnPit = gamesEndpoint + "/" + newGame.getBody().getId() + "/pits/" + 1;
        ResponseEntity<GameStatusResponseDto> moveResponse = template.exchange(makeMoveOnPit, HttpMethod.PUT, null, GameStatusResponseDto.class);
        assertThat(moveResponse.getStatusCode(), is(HttpStatus.OK));
        Map<String, Integer> gameStatus = moveResponse.getBody().getStatus();
        assertThat(gameStatus.size(), is(14));
        int[] expectedResponse = {0, 7, 7, 7, 7, 7, 1, 6, 6, 6, 6, 6, 6, 0};

        for (int i = 0; i < expectedResponse.length; i++) {
            int resultedValue = gameStatus.get(String.valueOf(i + 1));
            assertThat(resultedValue, Is.is(expectedResponse[i]));
        }
    }
}