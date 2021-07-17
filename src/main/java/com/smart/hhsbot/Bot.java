package com.smart.hhsbot;

import com.smart.hhsbot.commands.Games;
import com.smart.hhsbot.events.*;
import com.smart.hhsbot.templates.GuildData;
import com.smart.hhsbot.userVerification.VerificationCommands;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Bot extends ListenerAdapter implements EventListener {
    public static final Bot BOT = new Bot();
    public final JDA jda;

    // Required Items
    public static String[] roles = {
            "Freshman", "Sophomore", "Junior", "Senior", "Moderator"
    };
    public static String[] channels = {
            "Bot Logs", "Gaming"
    };
    public static String[] categories = {
    };

    // Guilds
    public static LinkedHashMap<Long, GuildData> guildData = new LinkedHashMap<>();

    // Emoji Data
    public static final String RED_CROSS_EMOJI = "<:x_emoji:852532275358859264>";
    public static final String CHECK_EMOJI = "<:check_mark:852532194702393385>";
    public static final String WARNING_EMOJI = "<:warning:852532221578313769>";

    // Color Data
    public static final Color GREEN = new Color(0x57F287);
    public static final Color RED = new Color(0xE20001);
    public static final Color BLUE = new Color(0x6A79C4);
    public static final Color DARK_GREEN = new Color(0x5DA859);
    public static final Color DARK_RED = new Color(0xBD3761);
    public static final Color PINK = new Color(0xE67290);
    public static final Color GOLD = new Color(0xEBD922);

    public Bot() {
        this.jda = loadBot();
    }

    public static void main(String[] args) {
        // Check for file directory
    }

    public JDA loadBot() {
        try {
            String FILE_DIRECTORY = "C:\\Users\\alexa\\Documents\\HHSBot\\";
            String TOKEN_PATH = "bot-token.secret";
            String token = new String(Files.readAllBytes(new File(FILE_DIRECTORY + TOKEN_PATH).toPath()));

            // Load JDA
            JDA jda = JDABuilder.createDefault(
                    token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .addEventListeners(
                            new Startup(),
                            new MemberJoin(),
                            new SlashCommand(),
                            new Buttons(),
                            new GuildJoin(),
                            new GuildLeave(),
                            new MemberLeave()
                    )
                    .setActivity(Activity.watching("HHS Students"))
                    .build();

            /*CommandListUpdateAction commands = jda.updateCommands();

            loadAllCommands(commands);
            commands.queue();*/

            System.out.println("Bot Loaded!");
            return jda;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void loadAllCommands(CommandListUpdateAction commands) {
        VerificationCommands.loadVerificationCommands(commands);
        Games.loadGameCommands(commands);
    }

    public static EmbedBuilder buildEmbed(String title, String description, Color color, MessageEmbed.Field[] fields) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle(title);
        b.setDescription(description);
        b.setColor(color);

        for (MessageEmbed.Field f : fields)
            b.addField(f.getName(), f.getValue(), f.isInline());

        return b;
    }

    public static EmbedBuilder buildEmbed(String title, String description, Color color, MessageEmbed.Field[] fields, String footer) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle(title);
        b.setDescription(description);
        b.setColor(color);
        b.setFooter(footer);

        for (MessageEmbed.Field f : fields)
            b.addField(f.getName(), f.getValue(), f.isInline());

        return b;
    }

    public static boolean hasRoles(Member m, List<Long> roleIds) {
        List<Long> memberRoleIds = m.getRoles().stream().map(Role::getIdLong).collect(Collectors.toList());

        for(Long memberID : memberRoleIds) {
            for(Long roleID : roleIds) {
                if(memberID.longValue() == roleID.longValue())
                    return true;
            }
        }
        return false;
    }

    public static String oxfordComma(List<String> stringArr, String fanboys) {
        String string = stringArr.toString().replaceAll("[\\[\\]]", "").trim();

        // OXFORD COMMA: Only run if the command contains a comma
        if (string.contains(",")) {
            String tempRoleString;  // Create a temp string for setting values without modifying the original string

            if (stringArr.size() == 2)
                // If the length is 2, don't add a comma
                tempRoleString = string.substring(0, string.lastIndexOf(","));
            else
                // If the length is not 2, add a comma
                tempRoleString = string.substring(0, string.lastIndexOf(",") + 1);

            // Set back to original string
            string = tempRoleString + " " + fanboys + " " + string.substring(string.lastIndexOf(",") + 2);
        }
        return string;
    }
}
