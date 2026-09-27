package com.cardroom.server.controller;

import com.cardroom.server.dto.GameInfoResponse;
import com.cardroom.server.service.GameService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService games;

    public GameController(GameService games) {
        this.games = games;
    }

    @GetMapping
    public List<GameInfoResponse> list() {
        return games.list();
    }
}
