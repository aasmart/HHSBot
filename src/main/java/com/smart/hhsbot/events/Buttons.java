package com.smart.hhsbot.events;

import com.smart.hhsbot.Bot;
import com.smart.hhsbot.games.TicTacToe;
import com.smart.hhsbot.userVerification.UserVerification;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Buttons extends ListenerAdapter {
    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        String[] id = event.getComponentId().split(":");
        String type = id[0];

        event.deferEdit().queue();

        switch (type) {
            case "verification" -> {
                switch (id[1]) {
                    case "email-submit" -> UserVerification.emailVerification(event, id);
                    case "email" -> UserVerification.handleEmailCheck(event, id);
                    case "code" -> UserVerification.verifyCode(event, id);
                    case "resend", "resend-2" -> UserVerification.resendEmail(event, id[2], id[3]);
                    case "new-email" -> UserVerification.changeEmail(event, id);
                    case "reload" -> UserVerification.reloadEmailButtons(event);
                    case "rules-decline" -> UserVerification.rulesDecline(event, id);
                    case "rules-agree" -> UserVerification.rulesAgree(event, id);
                    case "freshman", "sophomore", "junior", "senior" -> UserVerification.selectGradeLevel(event, id);
                    case "other" -> UserVerification.otherGrade(event, id);
                    case "invited-by" -> UserVerification.getInviter(event, id);
                    case "skip" -> UserVerification.admitToServer(event, id);
                    case "inviter" -> UserVerification.handleInviterCheck(event, id);
                    case "beta-test" -> {
                        if(event.getButton() != null) {
                            event.editButton(event.getButton().asDisabled()).queue();
                            UserVerification.join(Bot.BOT.jda.getGuildById(id[2]), event.getUser());
                        }
                    }
                    //case "confirm-grade" -> UserVerification.confirmGrade(event, id);
                }
            }
            case "game" -> {
                switch (id[1]) {
                    case "accept-tic" -> TicTacToe.gameAccept(event, id);
                    case "decline-tic" -> TicTacToe.gameDecline(event, id);
                    case "start-tic" -> TicTacToe.gameStart(event, id);
                    case "cancel-tic" -> TicTacToe.gameCancel(event, id);
                    case "tictactoe" -> TicTacToe.updateGameBoard(event, id);
                }
            }
        }
    }
}
