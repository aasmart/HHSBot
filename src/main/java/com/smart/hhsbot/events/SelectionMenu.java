package com.smart.hhsbot.events;

import com.smart.hhsbot.games.connectFour.ConnectFour;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SelectionMenu extends ListenerAdapter {
    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        String[] data = event.getComponentId().split(":");
        switch (data[1]) {
            case "connect-four" -> ConnectFour.handlePiecePlace(event, data);
        }
    }
}
