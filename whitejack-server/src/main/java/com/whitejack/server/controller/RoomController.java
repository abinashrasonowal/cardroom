package com.whitejack.server.controller;

import com.whitejack.contract.RoomCode;
import com.whitejack.server.dto.AddBotResponse;
import com.whitejack.server.dto.CreateRoomRequest;
import com.whitejack.server.dto.CreateRoomResponse;
import com.whitejack.server.service.BotService;
import com.whitejack.server.service.RoomService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Opening a room is HTTP; everything that happens inside one is the socket. */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService rooms;
    private final BotService bots;

    public RoomController(RoomService rooms, BotService bots) {
        this.rooms = rooms;
        this.bots = bots;
    }

    @PostMapping
    public CreateRoomResponse create(@RequestBody CreateRoomRequest request) {
        return new CreateRoomResponse(rooms.create(request.gameId()));
    }

    /** Seats a Jev bot; it then plays over the socket like anyone else. */
    @PostMapping("/{room}/bots")
    public AddBotResponse addBot(@PathVariable String room) {
        return bots.add(new RoomCode(room));
    }
}
