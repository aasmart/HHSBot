package com.smart.hhsbot.events;

import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class GuildJoin extends ListenerAdapter {

    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Startup.loadPrerequisites(Collections.singletonList(event.getGuild()));
    }
}
