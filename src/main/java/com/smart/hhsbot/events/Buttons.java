package com.smart.hhsbot.events;

import com.smart.hhsbot.Bot;
import com.smart.hhsbot.games.connectFour.ConnectFour;
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


        switch (type) {
            case "verification" -> {
                event.deferEdit().queue();
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
                event.deferEdit().queue();
                switch (id[1]) {
                    // Tic Tac Toe
                    case "accept-tic" -> TicTacToe.gameAccept(event, id);
                    case "decline-tic" -> TicTacToe.gameDecline(event, id);
                    case "start-tic" -> TicTacToe.gameStart(event, id);
                    case "cancel-tic" -> TicTacToe.gameCancel(event, id);
                    case "tictactoe" -> TicTacToe.updateGameBoard(event, id);
                    // Connect 4
                    case "accept-four" -> ConnectFour.gameAccept(event, id);
                    case "decline-four" -> ConnectFour.gameDecline(event, id);
                    case "start-four" -> ConnectFour.gameStart(event, id);
                    case "cancel-four" -> ConnectFour.gameCancel(event, id);
                }
            }
            case "connect-four" -> {
                switch (id[1]) {
                    case "forfeit" -> ConnectFour.gameForfeit(event, id);
                    case "draw" -> ConnectFour.gameDraw(event, id);
                    case "draw-accept" -> ConnectFour.drawAccept(event, id);
                    case "draw-decline" -> ConnectFour.drawDecline(event, id);
                }
            }
        }
    }
}
