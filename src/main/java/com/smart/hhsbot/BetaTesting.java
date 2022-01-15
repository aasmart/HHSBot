package com.smart.hhsbot;

import com.smart.hhsbot.commands.Commands;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.Button;

public class BetaTesting {
    public static void betaTesting(SlashCommandEvent event) {
        if(Commands.invalidSendState(event, Commands.CommandType.GUILD_COMMAND, Commands.CapitalizationForm.FIRST, new String[]{"Moderator"}, new String[]{}, new String[]{}))
            return;

        OptionMapping userOption = event.getOption("user");
        if(userOption == null) {
            Commands.commandError(event, Commands.CapitalizationForm.FIRST,"Command must have a user");
            return;
        }

        OptionMapping featureOption = event.getOption("feature");
        if(featureOption == null) {
            Commands.commandError(event, Commands.CapitalizationForm.FIRST, "Command must have a feature to beta test");
            return;
        }

        User user = userOption.getAsUser();
        String feature = featureOption.getAsString();

        switch (feature.toLowerCase()) {
            case "verification", "member screening", "membership screening" -> membershipScreening(event, user);
        }
    }

    public static void membershipScreening(SlashCommandEvent event, User user) {
        Guild g = event.getGuild();
        if(g == null)
            return;

        try {
            user.openPrivateChannel().queue(channel ->
                channel.sendMessageEmbeds(Bot.buildEmbed(
                        "Verification Process Beta Testing",
                        "You have been selected for a beta testing process",
                        Bot.RED,
                        new MessageEmbed.Field[] {
                                new MessageEmbed.Field(
                                        "What is the Verification Process?",
                                        "The verification process is a new, automated process for giving users access to the server " +
                                        "with little to no moderator input. This is a several step process and may take several minutes.",
                                        false
                                ),
                                new MessageEmbed.Field(
                                        "What do I do?",
                                        "We, the HHS Discord administrators would appreciate it if you went through the process and gave us " +
                                                "feedback at the end using a Google Form. Completion is not required but appreciated and users will" +
                                                "remain anonymous. If you are willing to go through this process please select the attached button.",
                                        false
                                )
                        }
                ).setFooter(
                        "P.S there's a step where you have to select your grade level. Please select the same grade level as your " +
                        "current role. Also, declining the rules will kick you so be careful with that. Thank you"
                ).build()).setActionRow(
                        Button.success("verification:beta-test:" + g.getIdLong(), "Begin Verification Process")
                ).queue(success -> event.reply("Sent verification process").setEphemeral(true).queue(),
                        fail -> Commands.commandError(event, Commands.CapitalizationForm.FIRST, "Failed to open private channel."))
            , failure -> Commands.commandError(event, Commands.CapitalizationForm.FIRST, "Failed to open private channel."));
        } catch (UnsupportedOperationException e) {
            Commands.commandError(event, Commands.CapitalizationForm.FIRST, "I can't send a direct message to myself!");
        }
    }
}
