package com.whitejack.bots.view;

import java.util.List;

/** engine-core LobbyView as it arrives on the wire. */
public record LobbyView(String room, String gameId, String phase, String host, List<Member> members) {

    public record Member(int seat, String id, String nick, boolean connected) {}
}
