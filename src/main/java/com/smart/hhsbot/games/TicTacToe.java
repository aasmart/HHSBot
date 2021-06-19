package com.smart.hhsbot.games;

import com.smart.hhsbot.commands.Commands;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TicTacToe {
    public static void play(SlashCommandEvent event) {
        OptionMapping userOption = event.getOption("opponent");
        if(userOption == null) {
            Commands.commandError(event, Commands.CapitalizationForm.FIRST, "Must have an opponent");
            return;
        }

        User user = userOption.getAsUser();
        event.reply(
                user.getAsMention() + ", " + event.getUser().getAsMention() + " has challenged you to a game of **Tic Tac Toe**. " +
                        "If you accept, select **Accept**. This message will delete after one minute."
        ).addActionRow(
                Button.primary("game:accept-tic:" + user.getId() + ":" + event.getUser().getId(), "Accept"),
                Button.secondary("game:decline-tic:" + user.getId(), "Decline")
        ).queue(message -> message.deleteOriginal().queueAfter(1, TimeUnit.MINUTES, success -> {}, failure -> {}));
    }

    public static void gameAccept(ButtonClickEvent event, String[] ids) {
        if(!event.getUser().getId().equals(ids[2]))
            return;

        event.getHook().deleteOriginal().queue();

        User u = event.getJDA().getUserById(ids[3]);
        if(u == null)
            return;

        event.getChannel().sendMessage( u.getAsMention() + ", " + event.getUser().getAsMention() + " has accepted your Tic Tac Toe game." +
                " Select **Begin Game** to begin the game. This message will delete after one minute.")
                .setActionRow(
                        Button.primary("game:start-tic:" + ids[3] + ":" + ids[2], "Begin Game"),
                        Button.secondary("game:cancel-tic:" + ids[3] + ":" + ids[2], "Cancel Game")
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

        List<ActionRow> rows = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            List<Button> row = new ArrayList<>();
            for(int j = 0; j < 3; j++)
                // 7 Spaces to make buttons the same size as emoji buttons
                row.add(Button.secondary("game:tictactoe:x:" + ids[2] + ":" + ids[3] + ":" + i + "" + j, "       "));
            rows.add(ActionRow.of(row));
        }

        event.getHook().deleteOriginal().queue();

        User playerOne = event.getUser();
        User playerTwo = event.getJDA().getUserById(ids[3]);
        if(playerTwo == null) {
            event.getChannel().sendMessage("Failed to retrieve player.").queue();
            return;
        }
        event.getChannel().sendMessage("Tic Tac Toe game between " + playerOne.getAsMention() + " and " + playerTwo.getAsMention() + ". It is " +
                event.getUser().getAsMention() + "'s turn.")
                .setActionRows(rows).queue(message -> message.delete().queueAfter(5, TimeUnit.MINUTES, success -> {}, failure -> {}));
    }

    public static void updateGameBoard(ButtonClickEvent event, String[] ids) {
        if(!event.getUser().getId().equals(ids[3]) || event.getButton() == null || event.getMessage() == null)
            return;

        String nextSymbol = ids[2].equals("x") ? "o" : "x";

        List<ActionRow> actionRows = new ArrayList<>();

        for (ActionRow actionRow : event.getMessage().getActionRows())
            actionRows.add(
                    ActionRow.of(
                            actionRow.getButtons().stream().map(button -> {
                                    String[] buttonIds = Objects.requireNonNull(button.getId()).split(":");
                                    if(button.getId() != null && buttonIds.length >= 6 && buttonIds[5].equals(ids[5])) {
                                        if (nextSymbol.equals("o"))
                                            return Button.secondary("x:disabled", Emoji.fromUnicode("U+274C"));
                                        else
                                            return Button.secondary("o:disabled", Emoji.fromUnicode("U+2B55"));
                                    } else if(!button.getId().contains("disabled"))
                                        return button.withId("game:tictactoe:" + nextSymbol + ":" + ids[4] + ":" + ids[3] + ":" + button.getId().split(":")[5]);
                                    else
                                        return button;
                                }
                            ).collect(Collectors.toList())));

        List<ActionRow> winCheck = checkWinCondition(actionRows, ids[5], ids[2]);
        if(winCheck != null) {
            event.getHook().editOriginal(event.getUser().getAsMention() + " has won!").setActionRows(winCheck).queue(TicTacToe::disableGameButtons);
            return;
        }

        User nextPlayer = event.getJDA().getUserById(ids[4]);
        if(nextPlayer == null)
            event.getHook().editOriginal("Failed to retrieve player").setActionRows().queue();
        else if(checkTie(actionRows))
            event.getHook().editOriginal("The game has ended in a draw").setActionRows(actionRows).queue(TicTacToe::disableGameButtons);
        else
            event.getHook().editOriginal("It is " + nextPlayer.getAsMention() + "'s turn").setActionRows(actionRows).queue();
    }

    public static List<ActionRow> checkWinCondition(List<ActionRow> actionRows, String pos, String symbol) {
        List<ActionRow> newActionRows = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            List<Button> buttonRow = new ArrayList<>();
            for(int j = 0; j< 3; j++)
                buttonRow.add(actionRows.get(i).getButtons().get(j));
            newActionRows.add(ActionRow.of(buttonRow));
        }

        // Row check
        int row = Integer.parseInt(pos.substring(0,1));
        int column = Integer.parseInt(pos.substring(1));

        // Row Check
        List<Button> rowButtons = actionRows.get(row).getButtons();
        List<Button> columnButtons = actionRows.stream().map(rowTemp -> rowTemp.getButtons().get(column)).collect(Collectors.toList());

        List<Button> rowCheck = checkStraight(rowButtons, symbol);
        if(rowCheck != null) {
            newActionRows.set(row, ActionRow.of(rowCheck));
            return newActionRows;
        }

        List<Button> columnCheck = checkStraight(columnButtons, symbol);
        if(columnCheck != null) {
            for(int i = 0; i < 3; i++) {
                List<Button> buttons = new ArrayList<>(newActionRows.get(i).getButtons());
                buttons.set(column, columnCheck.get(i));
                newActionRows.set(i, ActionRow.of(buttons));
            }
            return newActionRows;
        }

        // Diagonal Checks
        if(row == column) {
            ArrayList<ActionRow> newRowsTemp = new ArrayList<>(newActionRows);
            for (int i = 0; true; i++) {
                Button b = actionRows.get(i).getButtons().get(i);
                String id = b.getId();
                if (id == null)
                    break;

                List<Button> buttonsTemp = new ArrayList<>(newRowsTemp.get(i).getButtons());
                buttonsTemp.set(i, buttonsTemp.get(i).withStyle(ButtonStyle.PRIMARY));
                newRowsTemp.set(i, ActionRow.of(buttonsTemp));

                String[] ids = id.split(":");
                if (!ids[1].equals("disabled") || !ids[0].equals(symbol))
                    break;
                else if (i == 2)
                    return newRowsTemp;
            }
        }

        if(row+column == 2) {
            ArrayList<ActionRow> newRowsTemp = new ArrayList<>(newActionRows);
            for (int i = 2; true; i--) {
                Button b = actionRows.get(i).getButtons().get(2 - i);
                String id = b.getId();
                if (id == null)
                    break;

                List<Button> buttonsTemp = new ArrayList<>(newRowsTemp.get(i).getButtons());
                buttonsTemp.set(2 - i, buttonsTemp.get(2 - i).withStyle(ButtonStyle.PRIMARY));
                newRowsTemp.set(i, ActionRow.of(buttonsTemp));

                String[] ids = id.split(":");
                if (!ids[1].equals("disabled") || !ids[0].equals(symbol))
                    break;
                else if (i == 0)
                    return newRowsTemp;
            }
        }

        return null;
    }

    public static boolean checkTie(List<ActionRow> actionRows) {
        for(int i = 0; i < 3; i++)
            for(int j = 0; j < 3; j++) {
                if (!Objects.requireNonNull(actionRows.get(i).getButtons().get(j).getId()).contains("disabled"))
                    return false;
            }

        return true;
    }

    public static List<Button> checkStraight(List<Button> buttons, String symbol) {
        ArrayList<Button> modifiedButtons = new ArrayList<>();
        for(int i = 0; true; i++) {
            Button b = buttons.get(i);
            String id = b.getId();
            if(id == null)
                break;

            modifiedButtons.add(i, b.withStyle(ButtonStyle.PRIMARY));

            String[] ids = id.split(":");
            if(!ids[1].equals("disabled") || !ids[0].equals(symbol))
                return null;
            else if(i == 2)
                return modifiedButtons;
        }
        return null;
    }

    public static void disableGameButtons(Message message) {
        MessageBuilder builder = new MessageBuilder(message);

        List<ActionRow> rows = new ArrayList<>();

        for (ActionRow actionRow : message.getActionRows())
            rows.add(ActionRow.of(actionRow.getButtons().stream().map(button -> button.withDisabled(true)).collect(Collectors.toList())));

        builder.setActionRows(rows);
        message.editMessage(builder.build()).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS, success -> {}, failure -> {}));
    }

    public static void gameCancel(ButtonClickEvent event, String[] ids) {
        if(!event.getUser().getId().equals(ids[2]))
            return;

        event.getHook().editOriginal(event.getUser().getAsMention() + " has cancelled the game.").queue();
    }
}
