package com.cardroom.server.controller;

import com.cardroom.server.dto.CreateRoomRequest;
import com.cardroom.server.dto.CreateRoomResponse;
import com.cardroom.server.service.RoomService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Opening a room is HTTP; everything that happens inside one is the socket. */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService rooms;

    public RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping
    public CreateRoomResponse create(@RequestBody CreateRoomRequest request) {
        return new CreateRoomResponse(rooms.create(request.gameId()));
    }
}
