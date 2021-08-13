package com.smart.hhsbot.games.connectFour;

import com.smart.hhsbot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class ConnectFourBoard {
    private static final int BOARD_HEIGHT = 6;
    private static final int BOARD_WIDTH = 7;

    public ConnectFourPieceType[][] board;
    public ConnectFourBoard() {
        board = new ConnectFourPieceType[BOARD_HEIGHT][BOARD_WIDTH];

        for(int row = 0; row < board.length; row++) {
            for(int column = 0; column < board[0].length; column++) {
                board[row][column] = ConnectFourPieceType.BLANK;
            }
        }
    }

    /**
     * Places a {@link ConnectFourPieceType} in a selected column. If the placement failed, -1 will be returned. If successful, the
     * row the piece was placed will be returned
     *
     * @param placeColumn The column to place the chip (greater than 0)
     * @param piece The piece to place
     * @return The success/error value
     */
    public int placePiece(int placeColumn, ConnectFourPieceType piece) {
        placeColumn--;
        if(placeColumn < 0 || placeColumn >= board[0].length)
            return -1;

        for(int row = 0; row < board.length; row++) {
            if(row > 0 && board[row][placeColumn] != ConnectFourPieceType.BLANK) {
                board[row - 1][placeColumn] = piece;
                return row - 1;
            } else if(row == 5) {
                board[row][placeColumn] = piece;
                return row;
            }
        }
        return -1;
    }

    /**
     * Creates a {@link MessageEmbed} that houses the "Connect 4" board
     * @return A MessageEmbed
     */
    public MessageEmbed getAsDisplay() {
        StringBuilder content = new StringBuilder();
        for (ConnectFourPieceType[] connectFourPieceTypes : board) {
            for (ConnectFourPieceType columnPiece : connectFourPieceTypes)
                content.append(columnPiece.getUnicode());
            content.append('\n');
        }
        content.append(appendNumbers());

        EmbedBuilder gameDisplay = Bot.buildEmbed(
                "Connect 4",
                content.toString(),
                Bot.BLUE,
                new MessageEmbed.Field[]{
                        new MessageEmbed.Field("How to Play", "To play, simply use the attached \"Selection Menu\" to select which " +
                                "column you want to play your piece. **Player One** is " + ConnectFourPieceType.RED.getUnicode() + " and " +
                                "**Player Two** is " + ConnectFourPieceType.YELLOW.getUnicode() + ".", false),
                        new MessageEmbed.Field("Inactivity Notice", "This game will be deleted after **five minutes** of " +
                                "inactivity.", false)
                });

        return gameDisplay.build();
    }

    private String appendNumbers() {
        StringBuilder content = new StringBuilder();
        for(int i = 1; i <= board[0].length; i++) {
            content.append(":").append(
                switch (i) {
                    case 1 -> "one";
                    case 2 -> "two";
                    case 3 -> "three";
                    case 4 -> "four";
                    case 5 -> "five";
                    case 6 -> "six";
                    case 7 -> "seven";
                    case 8 -> "eight";
                    case 9 -> "nine";
                    default -> "zero";
                }
            ).append(":");
        }

        return content.toString();
    }
}
