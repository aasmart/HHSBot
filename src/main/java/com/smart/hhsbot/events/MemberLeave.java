package com.smart.hhsbot.events;

import com.smart.hhsbot.Bot;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MemberLeave extends ListenerAdapter {
    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        TextChannel c = event.getGuild().getTextChannelById(Bot.guildData.get(event.getGuild().getIdLong()).channels.get("bot-logs"));
        if(c == null)
            return;
        c.sendMessage("**" + event.getUser().getAsMention() + " has left the server!**").queue();
    }
}
