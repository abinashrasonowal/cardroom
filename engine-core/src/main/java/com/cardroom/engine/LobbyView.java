package com.cardroom.engine;

import com.cardroom.contract.PlayerId;
import com.cardroom.contract.PlayerView;
import com.cardroom.contract.RoomCode;
import java.util.List;

/**
 * What players see before a game starts. The engine owns this one — a game module has no
 * opinion about who is in the lobby, and no game has started yet to ask.
 */
public record LobbyView(RoomCode room, String gameId, Phase phase, PlayerId host, List<Member> members)
        implements PlayerView {

    public LobbyView {
        members = List.copyOf(members);
    }

    public record Member(int seat, PlayerId id, String nick, boolean connected) {}
}
