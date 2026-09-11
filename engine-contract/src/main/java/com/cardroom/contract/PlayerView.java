package com.cardroom.contract;

/**
 * Marker: "this must serialize to JSON".
 *
 * <p>{@code project} is the only redaction point in the system. If a hidden card reaches a
 * client by any other path, the server-authoritative claim is decorative.
 */
public interface PlayerView {}
