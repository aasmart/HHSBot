package com.smart.hhsbot.events;

import com.smart.hhsbot.Bot;
import com.smart.hhsbot.templates.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Startup extends ListenerAdapter {
    public void onReady(@NotNull ReadyEvent event) {
        // Universal
        loadPrerequisites(event.getJDA().getGuilds());
    }

    public static void loadPrerequisites(List<Guild> guilds) {
        for(Guild g : guilds) {
            long guildID = g.getIdLong();
            Bot.guildData.put(guildID, new GuildData());

            EmbedBuilder embedBuilder = Bot.buildEmbed(
                    "HHS Bot Startup",
                    "Displays whether or not a required item was found at startup. Modification of the required items " +
                            "will not be reflected in this panel after startup. A " + Bot.WARNING_EMOJI + " means there are duplicates" +
                            " of a type and one should be removed otherwise issues may occur.",
                    Bot.RED,
                    new MessageEmbed.Field[]{}
            );

            // Roles
            StringBuilder roles = new StringBuilder();
            for(String s : Bot.roles) {
                List<Role> roleList = g.getRolesByName(s, true);
                exists(roleList, roles, s);

                if(roleList.size() != 0)
                    Bot.guildData.replace(guildID, Bot.guildData.get(guildID).addRole(s, roleList.get(0)));
            }
            embedBuilder.addField("Roles", roles.toString(), true);

            // Channels
            StringBuilder channels = new StringBuilder();
            for(String s : Bot.channels) {
                List<TextChannel> channelList = g.getTextChannelsByName(s.replaceAll(" ", "-"), true);
                exists(channelList, channels, s);

                if(channelList.size() != 0)
                    Bot.guildData.replace(guildID, Bot.guildData.get(guildID).addChannel(s.toLowerCase().replaceAll("\\s+", "-"), channelList.get(0)));
            }
            embedBuilder.addField("Channels", channels.toString(), true);

            // Categories
            StringBuilder categories = new StringBuilder();
            for(String s : Bot.categories) {
                List<Category> categoryList = g.getCategoriesByName(s.replaceAll(" ", "-"), true);
                exists(categoryList, categories, s);

                if(categoryList.size() != 0)
                    Bot.guildData.replace(guildID, Bot.guildData.get(guildID).addCategory(s.toLowerCase().replaceAll("\\s+", "-"), categoryList.get(0)));
            }
            embedBuilder.addField("Categories", categories.toString(), true);

            // TODO Character limit error
            /*TextChannel systemChannel = g.getTextChannelsByName("bot-logs", true).get(0);
            if(systemChannel == null)
                g.getTextChannels().get(0).sendMessage("Failed to find system messages channel").setEmbeds(embedBuilder.build()).queue();
            else
                systemChannel.sendMessageEmbeds(embedBuilder.build()).queue();*/
        }
    }

    private static <T> void exists(List<T> list, StringBuilder builder, String name) {
        if(list.size() > 1)
            builder.append(Bot.WARNING_EMOJI + " ").append(name).append("\n");
        else if(list.size() == 1)
            builder.append(Bot.CHECK_EMOJI + " ").append(name).append("\n");
        else
            builder.append(Bot.RED_CROSS_EMOJI + " ").append(name).append("\n");
    }
}
