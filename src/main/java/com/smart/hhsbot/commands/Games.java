package com.smart.hhsbot.commands;

import com.smart.hhsbot.games.connectFour.ConnectFour;
import com.smart.hhsbot.games.TicTacToe;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

public class Games extends Commands {
    public static void loadGameCommands(CommandListUpdateAction commands) {
        commands.addCommands(new CommandData("game", "Play a game!")
                .addOptions(new OptionData(OptionType.STRING, "game", "What game to play").setRequired(true)
                        .addChoice("Tic Tac Toe", "tic-tac-toe")
                        .addChoice("Connect 4", "connect-four"))
                .addOptions(new OptionData(OptionType.USER, "opponent", "The user you are playing against").setRequired(true))
        ).queue();
    }

    public static void determineGame(SlashCommandEvent event) {
        if(invalidSendState(event, CommandType.GUILD_COMMAND, CapitalizationForm.FIRST, new String[] {}, new String[]{"gaming"}, new String[] {}))
            return;

        OptionMapping gameOption = event.getOption("game");
        if(gameOption == null) {
            commandError(event, CapitalizationForm.FIRST, "Must choose a game to play!");
            return;
        }

        String game = gameOption.getAsString();

        switch (game) {
            case "tic-tac-toe" -> TicTacToe.play(event);
            case "connect-four" -> ConnectFour.play(event);
        }
    }
}
