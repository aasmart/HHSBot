package com.smart.hhsbot.events;

import com.smart.hhsbot.userVerification.UserVerification;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MemberJoin extends ListenerAdapter {
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if(event.getUser().isBot())
            return;

        UserVerification.join(event.getGuild(), event.getUser());
    }
}
