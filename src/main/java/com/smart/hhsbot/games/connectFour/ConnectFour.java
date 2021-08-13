package com.smart.hhsbot.games.connectFour;

import com.smart.hhsbot.commands.Commands;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ConnectFour {
    public static HashMap<Long, ConnectFourBoard> activeGames = new HashMap<>();
    public static HashMap<Long, ScheduledExecutorService> activeGameCleanup = new HashMap<>();

    public static void play(SlashCommandEvent event) {
        OptionMapping userOption = event.getOption("opponent");
        if(userOption == null) {
            Commands.commandError(event, Commands.CapitalizationForm.FIRST, "Must have an opponent");
            return;
        }

        User user = userOption.getAsUser();
        event.reply(
                user.getAsMention() + ", " + event.getUser().getAsMention() + " has challenged you to a game of **Connect 4**. " +
                        "If you accept, select **Accept**. This message will delete after one minute."
        ).addActionRow(
                Button.primary("game:accept-four:" + user.getId() + ":" + event.getUser().getId(), "Accept"),
                Button.secondary("game:decline-four:" + user.getId(), "Decline")
        ).queue(message -> message.deleteOriginal().queueAfter(1, TimeUnit.MINUTES, success -> {}, failure -> {}));
    }

    public static void gameAccept(ButtonClickEvent event, String[] ids) {
        if(!event.getUser().getId().equals(ids[2]))
            return;

        event.getHook().deleteOriginal().queue();

        User u = event.getJDA().getUserById(ids[3]);
        if(u == null)
            return;

        event.getChannel().sendMessage( u.getAsMention() + ", " + event.getUser().getAsMention() + " has accepted your Connect 4 game." +
                        " Select **Begin Game** to begin the game. This message will delete after one minute.")
                .setActionRow(
                        Button.primary("game:start-four:" + ids[3] + ":" + ids[2], "Begin Game"),
                        Button.secondary("game:cancel-four:" + ids[3] + ":" + ids[2], "Cancel Game")
                ).queue(message -> message.delete().queueAfter(1, TimeUnit.MINUTES, success -> {}, failure -> {}));
    }

    public static void gameDecline(ButtonClickEvent event, String[] ids) {
        if(!event.getUser().getId().equals(ids[2]))
            return;

        event.getHook().editOriginal(event.getUser().getAsMention() + " has declined the game.").setActionRows().queue();
    }

    public static void gameStart(ButtonClickEvent event, String[] ids) {
        if(!event.getUser().getId().equals(ids[2]))
            return;

        ConnectFourBoard board = new ConnectFourBoard();

        event.getHook().deleteOriginal().queue();

        User playerOne = event.getUser();
        User playerTwo = event.getJDA().getUserById(ids[3]);
        if(playerTwo == null) {
            event.getChannel().sendMessage("Failed to retrieve player.").queue();
            return;
        }

        SelectionMenu.Builder menu = SelectionMenu.create("menu:connect-four:1:" + playerOne.getId() + ":" + playerTwo.getId())
                .setPlaceholder("Choose the column to place your chip!")
                .setRequiredRange(1, 1);

        for(int i = 1; i <= board.board[0].length; i++)
            menu.addOption("Column " + i, "connect-four:" + i);

        event.getChannel()
                .sendMessage("**Connect 4** game between " + playerOne.getAsMention() + " and " + playerTwo.getAsMention() +
                        ". It is " + event.getUser().getAsMention() + "'s turn")
                .setEmbeds(board.getAsDisplay())
                .setActionRows(
                        ActionRow.of(
                                menu.build()
                        ),
                        ActionRow.of(
                                Button.primary("connect-four:draw:" + playerOne.getId() + ":" + playerTwo.getId(), "Offer Draw"),
                                Button.danger("connect-four:forfeit:" + playerOne.getId() + ":" + playerTwo.getId(), "Forfeit")
                        )
                )
                .queue(message -> {
                    activeGames.put(message.getIdLong(), board);
                    cleanupBoard(message);
                });
    }

    /**
     * Used to handle a SelectionMenuEvent that contains the location for the piece to be played, along with player data
     *
     * @param event The event
     * @param data The split component ID
     */
    public static void handlePiecePlace(SelectionMenuEvent event, String[] data) {
        if(event.getMessage() == null)
            return;

        switch (data[2]) {
            case "1" -> {
                if(!event.getUser().getId().equals(data[3])) {
                    event.reply("It is not your turn to play. Alternatively, you may not be a part of the game.").setEphemeral(true).queue();
                    return;
                }
            }
            case "2" -> {
                if(!event.getUser().getId().equals(data[4])) {
                    event.reply("It is not your turn to play. Alternatively, you may not be a part of the game.").setEphemeral(true).queue();
                    return;
                }
            }
        }

        ConnectFourBoard board = activeGames.get(event.getMessageIdLong());
        if(board == null) {
            event.reply("I could not find the cached game board, therefore I could not place the piece.").setEphemeral(true).queue();
            return;
        }

        if(event.getSelectedOptions() == null) {
            event.reply("You must select a column!").setEphemeral(true).queue();
            return;
        }

        int placeColumn = Integer.parseInt(event.getSelectedOptions().get(0).getValue().split(":")[1]);
        boolean removeColumn = false;

        int placedRow = board.placePiece(placeColumn, data[2].equals("1") ? ConnectFourPieceType.RED : ConnectFourPieceType.YELLOW);
        if(placedRow != -1) {
            if(placedRow == 0)
                removeColumn = true;
        } else {
            event.reply("Invalid column!").setEphemeral(true).queue();
            return;
        }

        boolean won = hasWon(
                board,
                placedRow + "" + (placeColumn - 1),
                data[2].equals("1") ? ConnectFourPieceType.RED : ConnectFourPieceType.YELLOW);

        User playerOne = event.getJDA().getUserById(data[3]);
        User playerTwo = event.getJDA().getUserById(data[4]);
        if(playerOne == null || playerTwo == null) {
            event.reply("Could not retrieve one or more users").setEphemeral(true).queue();
            return;
        }

        // Update ID
        String[] tempData = Arrays.copyOf(data, data.length);
        tempData[2] = data[2].equals("1") ? "2" : "1";
        StringBuilder builder = new StringBuilder();
        for (String value : tempData) {
            builder.append(value);
            builder.append(':');
        }
        
        if(event.getComponent() == null)
            return;

        SelectionMenu.Builder menu = event.getComponent().createCopy();
        menu.setDisabled(won);
        menu.setId(builder.toString());
        List<SelectOption> options = menu.getOptions();
        for(int i = options.size() - 1; i >= 0; i--) {
            if(removeColumn && options.get(i).getValue().contains(Integer.toString(placeColumn)))
                options.remove(i);
        }

        ActionRow buttons = ActionRow.of(
                Button.primary("connect-four:draw:" + playerOne.getId() + ":" + playerTwo.getId(), "Offer Draw").withDisabled(won),
                Button.danger("connect-four:forfeit:" + playerOne.getId() + ":" + playerTwo.getId(), "Forfeit").withDisabled(won)
        );

        MessageBuilder messageBuilder = new MessageBuilder()
                .setEmbeds(board.getAsDisplay())
                .setActionRows(ActionRow.of(menu.build()), buttons);

        // Add extra options because the size can't be 0
        if(won) {
            if(options.size() == 0)
                options.add(SelectOption.of("lol", "what"));

            activeGames.remove(event.getMessageIdLong());
            messageBuilder.setContent(event.getUser().getAsMention() + " has won!");
        } else if(options.size() == 0) {
            options.add(SelectOption.of("lol", "what"));

            activeGames.remove(event.getMessageIdLong());
            messageBuilder.setContent("The game has ended in a draw")
                    .setActionRows(
                            ActionRow.of(menu.setDisabled(true).build()),
                            ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().stream().map(Button::asDisabled).collect(Collectors.toList()))
                    );
        } else
            messageBuilder.setContent("**Connect 4** game between " + playerOne.getAsMention() + " and " + playerTwo.getAsMention() +
                    ". It is " + (data[2].equals("1") ? playerTwo.getAsMention() : playerOne.getAsMention()) + "'s turn");

        event.editMessage(messageBuilder.build()).queue(success -> {
            ScheduledExecutorService executor = activeGameCleanup.get(event.getMessageIdLong());
            executor.shutdownNow();
            cleanupBoard(event.getMessage());
        });
    }

    /**
     * Checks to see if the current player has one after placing their piece
     *
     * @param board The board being used
     * @param pos The position the piece was placed (xy)
     * @param piece The piece the player played
     * @return True if the player has won
     */
    public static boolean hasWon(ConnectFourBoard board, String pos, ConnectFourPieceType piece) {
        // Row check
        int rowVal = Integer.parseInt(pos.substring(0,1));
        int columnVal = Integer.parseInt(pos.substring(1));

        // Row Check
        if(checkWinCondition(board.board[rowVal], piece))
            return true;

        // Column check
        ConnectFourPieceType[] columnArray = new ConnectFourPieceType[board.board.length];
        for(int i = 0; i < columnArray.length; i++)
            columnArray[i] = board.board[i][columnVal];
        if(checkWinCondition(columnArray, piece))
            return true;

        List<ConnectFourPieceType> upDiagonal = new ArrayList<>();
        int upDiagonalColumn = Math.max(0,columnVal - (ConnectFourBoard.BOARD_HEIGHT-1 - rowVal));
        for(int i = Math.min(ConnectFourBoard.BOARD_HEIGHT-1, Math.min(ConnectFourBoard.BOARD_WIDTH-1, columnVal) + rowVal); i >= 0; i--) {
            if(upDiagonalColumn + ((board.board.length - 1) - i) > ConnectFourBoard.BOARD_WIDTH-1)
                break;
            upDiagonal.add(board.board[i][upDiagonalColumn + ((board.board.length - 1) - i)]);
        }
        if(upDiagonal.size() >= 4 && checkWinCondition(upDiagonal.toArray(ConnectFourPieceType[]::new), piece))
            return true;

        List<ConnectFourPieceType> downDiagonal = new ArrayList<>();
        int tempVal = ((ConnectFourBoard.BOARD_HEIGHT-1) - Math.min(ConnectFourBoard.BOARD_WIDTH-1, columnVal)) + (rowVal - (ConnectFourBoard.BOARD_HEIGHT-1));
        int downDiagonalColumn = tempVal < 0 ? Math.abs(tempVal) : 0;
        for(int i = Math.max(0, tempVal); i < board.board.length; i++) {
            try {
                downDiagonal.add(board.board[i][downDiagonalColumn + i]);
            } catch (Exception e) {
                break;
            }
        }
        return downDiagonal.size() >= 4 && checkWinCondition(downDiagonal.toArray(ConnectFourPieceType[]::new), piece);
    }

    /**
     * Checks to see if an array matches a win condition. The "win condition" is when there are 4 of the same piece
     * in a row
     *
     * @param array The array of values to check
     * @param piece The piece to search for
     * @return True if the win condition is met
     */
    public static boolean checkWinCondition(ConnectFourPieceType[] array, ConnectFourPieceType piece) {
        int pieceCount = 0;
        for (ConnectFourPieceType connectFourPieceType : array) {
            if (pieceCount > 0 && connectFourPieceType != piece)
                pieceCount = 0;
            else if (connectFourPieceType == piece)
                pieceCount++;
            if (pieceCount == 4)
                return true;
        }
        return false;
    }

    /**
     * Called when a user clicks the "Forfeit" button
     *
     * @param event The event
     * @param data The split component ID
     */
    @SuppressWarnings("DuplicatedCode")
    public static void gameForfeit(ButtonClickEvent event, String[] data) {
        Optional<String> idOptional = Arrays.stream(data).filter(id -> id.equals(event.getUser().getId())).findFirst();
        String id = idOptional.orElse(null);

        if(id == null) {
            event.reply("You are not participating in this game.").setEphemeral(true).queue();
            return;
        } else if(!activeGames.containsKey(event.getMessageIdLong())) {
            event.reply("This game is no longer cached.").setEphemeral(true).queue();
            return;
        }

        String otherId = id.equals(data[2]) ? data[3] : data[2];

        if(event.getMessage() == null)
            return;
        List<ActionRow> actionRows = event.getMessage().getActionRows();

        MessageBuilder messageBuilder = new MessageBuilder(event.getMessage());
        messageBuilder.setActionRows(
                ActionRow.of(((SelectionMenu)actionRows.get(0).getComponents().get(0)).asDisabled()),
                ActionRow.of(actionRows.get(1).getButtons().stream().map(Button::asDisabled).collect(Collectors.toList()))
        );
        messageBuilder.setContent("<@" + id + "> has forfeited. Therefore, <@" + otherId + "> wins by default");

        activeGames.remove(event.getMessageIdLong());
        event.editMessage(messageBuilder.build()).queue();
    }

    /**
     * Called when a user selects the "Draw" button. Offers the other player a draw
     *
     * @param event The event
     * @param data The split component ID
     */
    @SuppressWarnings("DuplicatedCode")
    public static void gameDraw(ButtonClickEvent event, String[] data) {
        Optional<String> idOptional = Arrays.stream(data).filter(id -> id.equals(event.getUser().getId())).findFirst();
        String id = idOptional.orElse(null);

        if(id == null) {
            event.reply("You are not participating in this game.").setEphemeral(true).queue();
            return;
        } else if(!activeGames.containsKey(event.getMessageIdLong())) {
            event.reply("This game is no longer cached.").setEphemeral(true).queue();
            return;
        }

        String otherId = id.equals(data[2]) ? data[3] : data[2];

        event.reply(event.getUser().getAsMention() + " has offered a draw. You have 10 seconds to accept or decline.")
                .addActionRow(
                    Button.primary("connect-four:draw-accept:" + otherId, "Accept"),
                    Button.secondary("connect-four:draw-decline:" + otherId, "Decline")
                )
                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS, success -> {}, fail -> {}));
    }

    /**
     * Called when the user accepts the draw offer
     *
     * @param event The event
     * @param data The split component ID
     */
    public static void drawAccept(ButtonClickEvent event, String[] data) {
        if(event.getMessage() == null)
            return;

        if(!event.getUser().getId().equals(data[2])) {
            event.reply("You are not participating in this game.").setEphemeral(true).queue();
            return;
        } else if(event.getMessage().getMessageReference() == null) {
            event.reply("This game no longer exists.").setEphemeral(true).queue();
            return;
        }

        event.getChannel().retrieveMessageById(event.getMessage().getMessageReference().getMessageId()).queue(message -> {
            if(!activeGames.containsKey(message.getIdLong())) {
                event.reply("This game is no longer cached.").setEphemeral(true).queue();
                return;
            }

            List<ActionRow> actionRows = message.getActionRows();

            MessageBuilder messageBuilder = new MessageBuilder(message);
            messageBuilder.setActionRows(
                    ActionRow.of(((SelectionMenu)actionRows.get(0).getComponents().get(0)).asDisabled()),
                    ActionRow.of(actionRows.get(1).getButtons().stream().map(Button::asDisabled).collect(Collectors.toList()))
            );
            messageBuilder.setContent("This game has ended in a draw");

            activeGames.remove(message.getIdLong());
            message.editMessage(messageBuilder.build()).queue(success ->
                event.getMessage().delete().queue()
            );
        }, fail ->
            event.reply("Encountered an error attempting to process the draw. This is likely because the referenced message no longer " +
                    "exists.").queue()
        );
    }

    /**
     * Called when a user declines a draw offer
     *
     * @param event The event
     * @param data The split component ID
     */
    public static void drawDecline(ButtonClickEvent event, String[] data) {
        if(event.getMessage() == null)
            return;

        if(!event.getUser().getId().equals(data[2])) {
            event.reply("You are not participating in this game.").setEphemeral(true).queue();
            return;
        }

        event.getMessage().delete().queue();
    }

    /**
     * Called when a use cancels the game request
     *
     * @param event The event
     * @param data The split component ID
     */
    public static void gameCancel(ButtonClickEvent event, String[] data) {
        if(!event.getUser().getId().equals(data[2]))
            return;

        event.getHook().editOriginal(event.getUser().getAsMention() + " has cancelled the game.").queue();
    }

    /**
     * Cleanups the game board
     *
     * @param message The message containing the game board
     */
    public static void cleanupBoard(Message message) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(() -> {
            activeGames.remove(message.getIdLong());
            message.delete().queue(success -> {}, fail -> {});
        }, 5, TimeUnit.MINUTES);
        activeGameCleanup.put(message.getIdLong(), executorService);
    }
}
