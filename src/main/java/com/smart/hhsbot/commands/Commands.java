package com.smart.hhsbot.commands;

import com.smart.hhsbot.Bot;
import com.smart.hhsbot.templates.GuildData;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commands {
    /**
     * The type of SlashCommand, which consists of {@link CommandType#GUILD_COMMAND} that can only be used in guilds. {@link CommandType#DIRECT_MESSAGE_COMMAND}
     * which can only be used in direct messages with the bot. And {@link CommandType#ALL} which can be used in both.
     */
    public enum CommandType {
        GUILD_COMMAND,
        DIRECT_MESSAGE_COMMAND,
        ALL
    }

    public enum CapitalizationForm {
        FIRST,
        ALL
    }

    /**
     * Returns if a command usage is invalid. If it is, true will be returned. This will check the member's roles, the channel, and the category
     * the command is executed in. If used in a category that is valid in an invalid channel false will be returned. This is favored to Discord's
     * built-in Slash Command permissions system as theirs is limited to a small amount of roles/users.
     *
     * @param event The Slash Command's event
     * @param commandType Where this command can be ran. See {@link CommandType}
     * @param roles The roles the user must have to execute this command
     * @param channels The channel the command must be used in
     * @param categories The category the command must be used in
     * @return True if the command cannot be used
     */
    public static boolean invalidSendState(SlashCommandEvent event, CommandType commandType, CapitalizationForm form,String[] roles, String[] channels, String[] categories) {
        if(commandType != CommandType.ALL && (!event.isFromGuild() && commandType == CommandType.GUILD_COMMAND)) {
            commandError(event, form, "This command must be used in a Guild.");
            return true;
        } else if(commandType != CommandType.ALL && (event.isFromGuild() && commandType == CommandType.DIRECT_MESSAGE_COMMAND)) {
            commandError(event, form, "This command must be used in direct messages with me.");
            return true;
        }

        if(commandType == CommandType.DIRECT_MESSAGE_COMMAND)
            return false;

        if(event.getMember() == null || event.getGuild() == null)
            return true;

        GuildData data = Bot.guildData.get(event.getGuild().getIdLong());

        List<Long> roleIds = new ArrayList<>();
        Arrays.stream(roles).forEach(role -> {
            if(data.roles.containsKey(role))
                roleIds.add(data.roles.get(role));
        });

        if(roles.length > 0 && !Bot.hasRoles(event.getMember(), roleIds)) {
            commandError(event, form, "Insufficient Permissions");
            return true;
        } else {
            List<Long> channelIDs = new ArrayList<>();
            Arrays.stream(channels).forEach(channel -> {
                if(data.channels.containsKey(channel))
                    channelIDs.add(data.channels.get(channel));
            });
            List<Long> categoryIDs = new ArrayList<>();
            Arrays.stream(categories).forEach(category -> {
                if(data.categories.containsKey(category))
                    categoryIDs.add(data.categories.get(category));
            });

            if(categoryIDs.size() == 0 && channelIDs.size() == 0)
                return false;
            else if(event.getGuildChannel().getParent() != null && categoryIDs.stream().anyMatch(categoryID -> categoryID == event.getGuildChannel().getParent().getIdLong()) ||
                    (channelIDs.stream().anyMatch(channelID -> channelID == event.getChannel().getIdLong())))
                return false;
            else {
                event.replyEmbeds(Bot.buildEmbed(
                        Bot.RED_CROSS_EMOJI + " " + event.getName().toUpperCase(),
                        "You can only use that command in the following " + (channels.length > 0 ? (channels.length == 1 ? "channel: " : "channels: ") + Bot.oxfordComma(channelIDs.stream().map(channelID -> {
                            TextChannel t = Bot.BOT.jda.getTextChannelById(channelID);
                            return t == null ? "" : t.getAsMention();
                        }).collect(Collectors.toList()), "and") : "") +
                                (categories.length > 0 ? (channels.length > 0 ? "; and in " : "") + (categories.length == 1 ? "category: " : "categories: ") + Bot.oxfordComma(Arrays.stream(categories).collect(Collectors.toList()), "and") : ""),
                        Bot.RED,
                        new MessageEmbed.Field[]{}
                ).build()).setEphemeral(true).queue();
                return true;
            }
        }
    }

    public static void commandError(SlashCommandEvent event, CapitalizationForm form, String errorMessage) {
        String title = form == CapitalizationForm.FIRST ? event.getName().substring(0,1).toUpperCase() + event.getName().substring(1) : event.getName().toUpperCase();
        event.replyEmbeds(Bot.buildEmbed(
                Bot.RED_CROSS_EMOJI + " " + title,
                errorMessage,
                Bot.RED,
                new MessageEmbed.Field[]{}
        ).build()).setEphemeral(true).queue();
    }

    public static void commandSuccess(SlashCommandEvent event, CapitalizationForm form, String message) {
        String title = form == CapitalizationForm.FIRST ? event.getName().substring(0,1).toUpperCase() + event.getName().substring(1) : event.getName().toUpperCase();
        event.replyEmbeds(Bot.buildEmbed(
                Bot.CHECK_EMOJI + " " + title,
                message,
                Bot.DARK_GREEN,
                new MessageEmbed.Field[]{}
        ).build()).setEphemeral(true).queue();
    }
}
