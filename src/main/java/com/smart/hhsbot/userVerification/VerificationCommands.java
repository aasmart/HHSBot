package com.smart.hhsbot.userVerification;

import com.smart.hhsbot.Bot;
import com.smart.hhsbot.commands.Commands;
import com.smart.hhsbot.events.Startup;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class VerificationCommands {
    public static void loadVerificationCommands(CommandListUpdateAction commands) {
        commands.addCommands(new CommandData("betatest", "Allows a user to beta test a feature")
                .addOptions(new OptionData(OptionType.USER, "user", "The user to allow beta testing for").setRequired(true))
                .addOptions(new OptionData(OptionType.STRING, "feature", "The feature to beta test").setRequired(true)
                        .addChoice("Verification Process", "verification")
                )
        ).queue();

        commands.addCommands(new CommandData("help", "Requests help for various topics")
                .addSubcommands(new SubcommandData(
                        "request", "Request help from admins. Used in DMs with the bot"
                ).addOptions(new OptionData(OptionType.STRING, "message", "The message to send").setRequired(true)))
        ).queue();

        commands.addCommands(new CommandData("dm", "Send a direct message to a user")
                .addOptions(new OptionData(OptionType.USER, "user", "The user to DM").setRequired(true))
                .addOptions(new OptionData(OptionType.STRING, "message", "The message to send").setRequired(true))
        ).queue();

        commands.addCommands(new CommandData("reload", "Reloads the bot for a server")).queue();
    }

    // TODO Make all responses embeds
    public static void helpRequest(SlashCommandEvent event) {
        if(Commands.invalidSendState(event, Commands.CommandType.DIRECT_MESSAGE_COMMAND, Commands.CapitalizationForm.FIRST, new String[]{}, new String[]{}, new String[]{}))
            return;

        // TODO Make the guild an option & a proper error
        Guild g = Bot.BOT.jda.getGuildById(526516818375082032L);
        if(g == null)
            return;
        LinkedHashMap<String, Long> channels = Bot.guildData.get(g.getIdLong()).channels;
        LinkedHashMap<String, Long> roles = Bot.guildData.get(g.getIdLong()).roles;

        TextChannel channel = g.getTextChannelById(channels.get("bot-logs"));
        Role moderator = g.getRoleById(roles.get("Moderator"));

        if(channel == null || moderator == null) {
            Commands.commandError(event, Commands.CapitalizationForm.FIRST,"Failed to find needed items. Please contact a moderator");
            return;
        }

        OptionMapping messageOption = event.getOption("message");
        if(messageOption == null) {
            Commands.commandError(event, Commands.CapitalizationForm.FIRST,"Must have message");
            return;
        }
        String message = messageOption.getAsString();

        if(message.length() > 1000) {
            Commands.commandError(event, Commands.CapitalizationForm.FIRST,"Message must be no longer than 1000 characters");
            return;
        }

        event.getChannel().getHistory().retrievePast(100).queue(messages -> {
            List<Message> filteredMessages = messages.stream().filter(msg -> msg.getAuthor().getIdLong() == event.getUser().getIdLong()).collect(Collectors.toList());
            StringBuilder builder = new StringBuilder();

            for(int i = 0; i < (Math.min(filteredMessages.size(), 5)); i++) {
                if(filteredMessages.get(i).getContentRaw().length() == 0)
                    break;
                else if(filteredMessages.get(i).getContentRaw().length() > 100)
                    builder.append("**").append(i+1).append(". **").append(filteredMessages.get(i).getContentRaw(), 0, 101).append("...\n");
                else
                    builder.append("**").append(i+1).append(". **").append(filteredMessages.get(i).getContentRaw()).append("\n");
            }

            channel.sendMessage(moderator.getAsMention()).setEmbeds(Bot.buildEmbed(
                    "A User Has Request Help!",
                    event.getUser().getAsMention() + " has requested help.",
                    Bot.BLUE,
                    new MessageEmbed.Field[]{
                            new MessageEmbed.Field(
                                    "Help Message Contents",
                                    message,
                                    false
                            ),
                            new MessageEmbed.Field(
                                    "Previous " + Math.min(filteredMessages.size(), 5) + " Messages",
                                    builder.toString(),
                                    false
                            )
                    },
                    "Use /dm to respond"
            ).setThumbnail(event.getUser().getEffectiveAvatarUrl()).build()).queue(
                    success -> event.replyEmbeds(Bot.buildEmbed(
                            "Help Request Sent",
                            "Your help request was sent. Please be patient as you await a response.",
                            Bot.BLUE,
                            new MessageEmbed.Field[]{}
                    ).build()).queue(),
                    fail -> event.replyEmbeds(Bot.buildEmbed(
                            "Fail to Send Help Request",
                            "We encountered an error trying to send your help request. Please contact " +
                                    "Moderators if this issue persists.",
                            Bot.BLUE,
                            new MessageEmbed.Field[]{}
                    ).build()).queue());
        });


    }

    public static void directMessage(SlashCommandEvent event) {
        if(Commands.invalidSendState(event, Commands.CommandType.GUILD_COMMAND, Commands.CapitalizationForm.ALL , new String[]{"Moderator"}, new String[]{"bot-logs"}, new String[]{}))
            return;

        OptionMapping userOption = event.getOption("user");
        OptionMapping messageOption = event.getOption("message");

        if(userOption == null)
            Commands.commandError(event, Commands.CapitalizationForm.ALL,"User was not given.");
        else if(messageOption == null)
            Commands.commandError(event, Commands.CapitalizationForm.ALL,"Message was not given.");
        else {
            User user = userOption.getAsUser();
            String message = messageOption.getAsString();
            if (message.length() > 1000) {
                Commands.commandError(event, Commands.CapitalizationForm.ALL,"Message may be no longer than 1000 characters.");
                return;
            }

            EmbedBuilder builder = Bot.buildEmbed(
                    "Direct Message Received",
                    "You have received a direct message.",
                    Bot.BLUE,
                    new MessageEmbed.Field[]{new MessageEmbed.Field("Message", message, false)},
                    "Sent by " + event.getUser().getName() + "#" + event.getUser().getDiscriminator()
            ).setThumbnail(event.getUser().getEffectiveAvatarUrl());

            try {
                user.openPrivateChannel().queue(privateChannel ->
                                privateChannel.sendMessageEmbeds(builder.build()).queue(success ->
                                                event.replyEmbeds(Bot.buildEmbed(
                                                        "Direct Message Sent",
                                                        "The direct message was sent to " + user.getAsMention() + ".",
                                                        Bot.BLUE,
                                                        new MessageEmbed.Field[]{
                                                                new MessageEmbed.Field(
                                                                        "Message Contents",
                                                                        message,
                                                                        false
                                                                )
                                                        }
                                                ).build()).queue(),
                                        failure ->
                                                event.replyEmbeds(Bot.buildEmbed(
                                                        "Direct Message Not Sent",
                                                        "Could not send the direct message.",
                                                        Bot.BLUE,
                                                        new MessageEmbed.Field[]{}
                                                ).build()).setEphemeral(true).queue()

                                ), fail -> event.replyEmbeds(Bot.buildEmbed(
                        "Direct Message Not Sent",
                        "Could not send the direct message.",
                        Bot.BLUE,
                        new MessageEmbed.Field[]{}
                        ).build()).setEphemeral(true).queue()
                );
            } catch (UnsupportedOperationException exception) {
                Commands.commandError(event, Commands.CapitalizationForm.ALL,"Can't send a direct message to myself!");
            }
        }
    }

    // TODO Make embed response
    public static void reload(SlashCommandEvent event) {
        if(Commands.invalidSendState(event, Commands.CommandType.GUILD_COMMAND, Commands.CapitalizationForm.FIRST, new String[]{"Moderator"}, new String[]{}, new String[]{}))
            return;

        Startup.loadPrerequisites(Collections.singletonList(event.getGuild()));
        Commands.commandSuccess(event, Commands.CapitalizationForm.FIRST, "Bot reloaded for guild.");
    }

}
