package com.smart.hhsbot.games.connectFour;

public enum ConnectFourPieceType {
    BLANK("\u26AA"),
    RED("\uD83D\uDD34"),
    YELLOW("\uD83D\uDFE1");

    private final String unicode;
    ConnectFourPieceType(String unicode) {
        this.unicode = unicode;
    }

    public String getUnicode() {
        return unicode;
    }
}
