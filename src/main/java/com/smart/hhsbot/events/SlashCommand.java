package com.smart.hhsbot.events;

import com.smart.hhsbot.BetaTesting;
import com.smart.hhsbot.commands.Games;
import com.smart.hhsbot.games.TicTacToe;
import com.smart.hhsbot.userVerification.VerificationCommands;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SlashCommand extends ListenerAdapter {
    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        try {
            switch (event.getName()) {
                case "betatest" -> BetaTesting.betaTesting(event);
                case "help" -> VerificationCommands.helpRequest(event);
                case "dm" -> VerificationCommands.directMessage(event);
                case "reload" -> VerificationCommands.reload(event);
                case "game" -> Games.determineGame(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
