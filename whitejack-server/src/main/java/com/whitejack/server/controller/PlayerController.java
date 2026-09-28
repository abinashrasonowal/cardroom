package com.whitejack.server.controller;

import com.whitejack.server.dto.MeResponse;
import com.whitejack.server.filter.IdentityFilter;
import com.whitejack.server.service.PlayerIdentityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private final PlayerIdentityService identity;

    public PlayerController(PlayerIdentityService identity) {
        this.identity = identity;
    }

    /** {@link IdentityFilter} has already issued or verified the cookie by the time this runs. */
    @GetMapping("/me")
    public MeResponse me(@RequestAttribute(IdentityFilter.TOKEN) String token) {
        return new MeResponse(identity.verify(token).orElseThrow().value(), token);
    }
}
