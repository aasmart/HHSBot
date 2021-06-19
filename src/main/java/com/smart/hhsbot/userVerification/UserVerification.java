package com.smart.hhsbot.userVerification;

import com.smart.hhsbot.Bot;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A class used for verifying a user who joined the Guild
 */
public class UserVerification {
    // TODO Verification process for teachers
    // TODO Config file
    private final long guildID;
    private final boolean proceeded;

    public UserVerification(long guildID, boolean proceeded) {
        this.guildID = guildID;
        this.proceeded = proceeded;
    }

    // TODO Make email/Guild more flexible
    // Regex
    private static final String DOMAIN = "@haslett.k12.mi.us";
    private static final String EMAIL_REGEX = "(\\d{2})([a-z]{1,6})([a-z]{2})(@haslett.k12.mi.us)?";
    private static final String EXAMPLE_NAME = "Name Example";
    private static final String EXAMPLE_EMAIL_NAME = "00examplex";

    // Email Stuff
    public static HashMap<Long, UserVerification> emailStatus = new HashMap<>();
    public static ArrayList<Long> emailCooldown = new ArrayList<>();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Generates a random 6-digit code
     *
     * @return A String containing a 6-digit code
     */
    public static String generateVerificationCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            code.append(new Random().nextInt(10));
        }
        return code.toString();
    }

    /**
     * Modifies the status of all the buttons in a message
     *
     * @param event The event
     * @param disabled True if all buttons are to be disabled
     */
    private static void modifyAllButtons(ButtonClickEvent event, boolean disabled) {
        assert event.getMessage() != null;
        MessageBuilder builder = new MessageBuilder(event.getMessage());

        List<ActionRow> actionRows = new ArrayList<>();

        for (ActionRow actionRow : event.getMessage().getActionRows())
            actionRows.add(ActionRow.of(actionRow.getButtons().stream().map(button -> button.withDisabled(disabled)).collect(Collectors.toList())));

        builder.setActionRows(actionRows);
        event.getMessage().editMessage(builder.build()).queue();
    }

    private static void modifyAllButtons(Message message, boolean disabled) {
        MessageBuilder builder = new MessageBuilder(message);

        List<ActionRow> actionRows = new ArrayList<>();

        for (ActionRow actionRow : message.getActionRows())
            actionRows.add(ActionRow.of(actionRow.getButtons().stream().map(button -> button.withDisabled(disabled)).collect(Collectors.toList())));

        builder.setActionRows(actionRows);
        message.editMessage(builder.build()).queue();
    }

    /**
     * The first message the user sees upon joining the Guild. Requires the submission of an email to continue to the
     * next step
     *
     * @param guild The guild the user joined
     * @param user The user who joined the server
     */
    public static void join(Guild guild, User user) {
        user.openPrivateChannel().queue(privateChannel ->
            privateChannel.sendMessageEmbeds(Bot.buildEmbed(
                guild.getName() + " Server Verification",
                "You must verify before you gain access to this server!",
                Bot.RED,
                new MessageEmbed.Field[]{
                    new MessageEmbed.Field(
                            "Beginning Verification",
                            "Hello and welcome to the **" + guild.getName() + "** server! As we value the " +
                                    "safety and security of our members, we kindly request you provide us with your " +
                                    "**school issued email**. Once you have typed it out and sent it, please select **Submit Email**.",
                            false),
                    new MessageEmbed.Field(
                            "What is My School Issued Email Address?",
                            "Given the name `" + EXAMPLE_NAME + "` & a graduation year of 2000, this *student* " +
                                    "will have the email address `" + EXAMPLE_EMAIL_NAME + DOMAIN + "`. " +
                                    "Note the domain (" + DOMAIN + ") is optional.",
                            false
                    ),
                    new MessageEmbed.Field(
                            "I Don't Have This Email! What do I do?",
                            "If you don't have the school-issued email address it likely means you do not" +
                                    " attend our schools. If this is the case, please leave the server. If you are a teacher " +
                                    "please see the next field for further information. If you do attend" +
                                    " the school, please contact us using `/help request`. If you are a teacher please see " +
                                    "the next field for further information.",
                            false
                    ),
                    new MessageEmbed.Field(
                            "I'm a Teacher",
                            "If you are a teacher you will go through a manual verification process by server staff. To begin, " +
                                    "use `/help request` to inform staff you are a teacher.",
                            false
                    )
                }
                ).build()
            ).setActionRow(
                    Button.primary("verification:email-submit:" + guild.getIdLong(), "Submit Email!")
            ).queue()
        );
    }

    /**
     * Ran after the user clicks a button with the id "verification:email-submit". This is the second step in the process
     * and will check for a valid email within the first 100 messages. If found, it will continue on to checkEmail(). If not,
     * a warning message will be sent
     *
     * @param event The button click event
     * @param ids The button ids
     */
    public static void emailVerification(ButtonClickEvent event, String[] ids) {
        Pattern p = Pattern.compile(EMAIL_REGEX);

        event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                List<Message> messageList = messages.stream().filter(message -> {
                    Matcher m = p.matcher(message.getContentRaw());
                    if (message.getAuthor().getIdLong() == event.getUser().getIdLong())
                        return m.find();
                    else
                        return false;
                }).collect(Collectors.toList());

                if(messageList.size() > 0)
                    checkEmail(event, messageList.get(0), ids);
                else {
                    Guild g = Bot.BOT.jda.getGuildById(ids[2]);
                    event.getChannel().retrieveMessageById(event.getMessageId()).queue(message ->
                        event.getChannel().sendMessageEmbeds(
                            Bot.buildEmbed(
                                (g != null ? g.getName() : "Unknown") + " Server Verification",
                                "You must verify before you gain access to this server!",
                                Bot.RED,
                                new MessageEmbed.Field[]{
                                    new MessageEmbed.Field(
                                            "Couldn't Find Valid Email!",
                                            "Could not find a valid email within the most recent 100 messages. Please submit a valid " +
                                            "email and press **Submit Email**.",
                                            false
                                    )
                                },
                                "This message will delete in under 30 seconds."
                            ).build()
                        ).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS))
                    );
                }
            }
        );
    }

    /**
     * Asks the user if the email selected is indeed their email. This message has two buttons id-ed "verification:email:yes:(user-email"
     * as well as "verification:email:no:(message-id):(guild-id)".
     *
     * @param event A button click event
     * @param message The message containing the user's email
     * @param ids The button ids
     */
    public static void checkEmail(ButtonClickEvent event, Message message, String[] ids) {
        Pattern p = Pattern.compile(EMAIL_REGEX);
        Matcher m = p.matcher(message.getContentRaw());

        if(event.getButton() != null)
            event.editButton(event.getButton().asDisabled()).queue();

        if(m.find()) {
            String email = m.group(1) + m.group(2) + m.group(3) + (m.groupCount() != 5 ? DOMAIN : "");
            Guild g = Bot.BOT.jda.getGuildById(ids[2]);

            event.getChannel().sendMessageEmbeds(Bot.buildEmbed(
                    (g != null ? g.getName() : "Unknown") + " Server Verification",
                    "You must verify before you gain access to this server!",
                    Bot.RED,
                    new MessageEmbed.Field[]{
                            new MessageEmbed.Field(
                                    "Is This Your Correct Email?",
                                    "Is `" + email + "` your correct email? If yes, select \"Yes\". If not, select \"No\".",
                                    false),
                    }
            ).build()).setActionRow(
                    Button.primary("verification:email:yes:" + email + ":" + ids[2], "Yes"),
                    Button.secondary("verification:email:no:" + event.getMessageId(), "No")
            ).queue();
        }
    }

    /**
     * Handles the response from clicking the buttons attached to the "checkEmail" message. If "Yes" was selected, the message's buttons
     * will be disabled and an email will be sent to their account. If "No" was selected the message will be deleted and the buttons in the
     * original message will be re-enabled.
     *
     * @param event A button click event
     * @param ids The button ids
     */
    public static void handleEmailCheck(ButtonClickEvent event, String[] ids) {
        switch (ids[2]) {
            case "yes" -> {
                modifyAllButtons(event, true);

                String email = ids[3];

                String verificationCode = generateVerificationCode();
                sendEmail(event, verificationCode, email, false, ids[4]);
            }

            case "no" -> event.getChannel().retrieveMessageById(ids[3]).queue(message -> {
                MessageBuilder builder = new MessageBuilder(message);
                builder.setActionRows(ActionRow.of(message.getButtons().get(0).asEnabled()));

                message.editMessage(builder.build()).queue();

                if(event.getMessage() != null)
                    event.getMessage().delete().queue();
                else
                    event.deferEdit().setActionRow().queue();
            });
        }
    }

    /**
     * Sends an email to a given email address. If successful, the user will be prompted with a code submission message. If the
     * email failed, they will be prompted with a warning suggesting help or to resend the email
     *
     * @param event A button click event
     * @param verificationCode The user's verification code
     * @param email The email address to send the email to
     * @param resendDisabled If the buttons for resending/changing emails wil be disabled.
     * @param guildID The ID of the guild
     */
    public static void sendEmail(ButtonClickEvent event, String verificationCode, String email, boolean resendDisabled, String guildID) {
        new Thread(() -> {
            try {
                codeMessage(event, email, verificationCode, resendDisabled || emailCooldown.contains(event.getUser().getIdLong()), guildID);

                Guild g = Bot.BOT.jda.getGuildById(guildID);
                GmailSender.sendMessage(GmailSender.createEmail(
                        email,
                        "hhsdiscordbot@gmail.com",
                        (g != null ? g.getName() : "Unknown") + " Discord Server Verification",
                        "If you are receiving this, your email was used by Discord user " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() +
                                ". If this is not you, delete this email. If this account belongs to you, your verification code is " +
                                verificationCode + ". If you have received this email numerous times please use the attached link to join " +
                                "our Discord server and use \"/help request\". From there we will contact you and sort out the issue: " +
                                "https://discord.gg/jhWyUGK"));
            } catch (Exception e) {
                if (Bot.guildData.containsKey(Long.parseLong(guildID))) {
                    Guild g = Bot.BOT.jda.getGuildById(guildID);
                    if (g == null)
                        return;

                    TextChannel channel = g.getTextChannelById(Bot.guildData.get(Long.parseLong(guildID)).channels.get("bot-logs"));
                    if (channel == null)
                        return;

                    channel.sendMessageEmbeds(Bot.buildEmbed(
                            "Failed to Send Email",
                            "Couldn't send email to " + event.getUser().getAsMention(),
                            Bot.RED,
                            new MessageEmbed.Field[]{}
                    ).build()).queue();
                }
                emailError(event, email, resendDisabled, guildID);
            }
        }).start();
    }

    /**
     * Called if an email couldn't be sent. This has a button with the id "verification:resend:(user's email)".
     *
     * @param event A button click event
     * @param email The user's email
     * @param resendDisabled If the buttons for resending the email will be disabled
     * @param guildID The ID of the guild
     */
    private static void emailError(ButtonClickEvent event, String email, boolean resendDisabled, String guildID) {
        Guild g = Bot.BOT.jda.getGuildById(guildID);

        event.getChannel().sendMessageEmbeds(
                Bot.buildEmbed(
                        (g != null ? g.getName() : "Unknown") + " Server Verification",
                        "You must verify before you gain access to this server!",
                        Bot.RED,
                        new MessageEmbed.Field[]{
                                new MessageEmbed.Field(
                                        "Failed to Send Email",
                                        "An issue was encountered trying to send an email to your account. If this issue persists, " +
                                                "please use `/help request`. To try again, select \"Resend Email\".",
                                        false
                                ),
                                new MessageEmbed.Field(
                                        "Reload? What's That",
                                        "If you resend the email, the button will be grayed out meaning it can't " +
                                                "be used. If it's been over 5 minutes and the buttons aren't activated, clicking \"Reload\" will " +
                                                "attempt to fix this.",
                                        false
                                )
                        }
                ).build()
        ).setActionRow(
                Button.primary("verification:resend:" + email + ":" + guildID, "Resend Email").withDisabled(resendDisabled),
                Button.primary("verification:reload", "Reload")
        ).queue();
    }

    /**
     * Sends an embed asking the user to submit the 6-digit verification code they received in their emails. This message contains
     * a button for submitting the code (verification:code:[verification-code]). Two buttons for re-sending the email or changing your
     * email (verification:resend-2:[email] and verification:new-email] respectively) that add the user to a cooldown (emailCooldown) for
     * five minutes.
     *
     * @param event A button click event
     * @param email The user's email
     * @param verificationCode The user's 6-digit verification code
     * @param disableEmailOptions True if the resend/email change options are to be disabled
     * @param guildID The ID of the guild
     */
    private static void codeMessage(ButtonClickEvent event, String email, String verificationCode, boolean disableEmailOptions, String guildID) {
        Guild g = Bot.BOT.jda.getGuildById(guildID);

        event.getChannel().sendMessageEmbeds(
                Bot.buildEmbed(
                        (g != null ? g.getName() : "Unknown") + " Server Verification",
                        "You must verify before you gain access to this server!",
                        Bot.RED,
                        new MessageEmbed.Field[]{
                                new MessageEmbed.Field(
                                        "Email Sent",
                                        "An email was sent to your account (" + email + ") containing a 6-digit verification code. Enter " +
                                                " the code and then select \"Submit Code\".",
                                        false
                                ),
                                new MessageEmbed.Field(
                                        "Help! I Didn't Receive the Email!",
                                        "If you didn't get a email, don't worry! Select \"Resend Email\" and the email will be resent. " +
                                                "If this issue persists, please use `/help request`. " +
                                                "Note: Resending the email has a *5 minute cooldown*.",
                                        false
                                ),
                                new MessageEmbed.Field(
                                        "I Want to Change My Email",
                                        "If you want to change your email, select \"Change Email\". Note: Changing your email has a 5" +
                                                " minute cooldown.",
                                        false
                                ),
                                new MessageEmbed.Field(
                                        "Reload? What's That",
                                        "If you change your email or resend the email, the buttons will be grayed out meaning they can't " +
                                                "be used. If it's been over 5 minutes and the buttons aren't activated, clicking \"Reload\" will " +
                                                "attempt to fix this.",
                                        false
                                )
                        }
                ).build()
        ).setActionRow(
                Button.primary("verification:code:" + verificationCode + ":" + guildID, "Submit Code"),
                Button.danger("verification:resend-2:" + email + ":" + guildID, "Resend Email").withDisabled(disableEmailOptions),
                Button.danger("verification:new-email:" + guildID, "Change Email").withDisabled(disableEmailOptions),
                Button.primary("verification:reload", "Reload")
        ).queue(message -> {
            if(disableEmailOptions) {
                MessageBuilder builder = new MessageBuilder(message);
                builder.setActionRows(ActionRow.of(
                        Button.primary("verification:code:" + verificationCode + ":" + guildID, "Submit Code"),
                        Button.danger("verification:resend-2:" + email + ":" + guildID, "Resend Email").asEnabled(),
                        Button.danger("verification:new-email:" + guildID, "Change Email").asEnabled(),
                        Button.primary("verification:reload", "Reload")
                ));

                message.editMessage(builder.build())
                        .setCheck(() -> {
                            long userID = event.getUser().getIdLong();
                            if(emailStatus.containsKey(userID) && emailStatus.get(userID).guildID == Long.parseLong(guildID)) {
                                return !emailStatus.get(event.getUser().getIdLong()).proceeded;
                            } else
                                return true;
                        })
                        .queueAfter(5, TimeUnit.MINUTES, success -> emailCooldown.remove(event.getUser().getIdLong()));
            }
        });
    }

    /**
     * Resends the verification email to the user. Also adds the user to a five minute email cooldown. Called by
     * verification:resend:(user's email)
     *
     * @param event A button click event
     * @param guildID The ID of the guild
     */
    public static void resendEmail(ButtonClickEvent event, String email, String guildID) {
        String verificationCode = generateVerificationCode();

        assert event.getMessage() != null;
        event.getMessage().delete().queue();
        emailCooldown.add(event.getUser().getIdLong());
        sendEmail(event, verificationCode, email, true , guildID);

        Guild g = Bot.BOT.jda.getGuildById(guildID);

        event.getChannel().sendMessageEmbeds(
                Bot.buildEmbed(
                        (g != null ? g.getName() : "Unknown") + " Server Verification",
                        "You must verify before you gain access to this server!",
                        Bot.RED,
                        new MessageEmbed.Field[]{
                                new MessageEmbed.Field(
                                        "Email Resent",
                                        "The email was resent containing a new verification code. You may not send" +
                                                " another email or change your email for **5 minutes**.",
                                        false
                                )
                        },
                        "This message will delete in under 30 seconds."
                ).build()
        ).queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));
    }

    /**
     * A button used for attempting to re-enable the email-related buttons if they were disabled and didn't come back on
     *
     * @param event A button click event
     */
    public static void reloadEmailButtons(ButtonClickEvent event) {
        if(!emailCooldown.contains(event.getUser().getIdLong()))
            modifyAllButtons(event, false);
    }

    /**
     * Initiates the email change. Called by verification:new-email
     *
     * @param event A button click event
     * @param ids The button's ids
     */
    public static void changeEmail(ButtonClickEvent event, String[] ids) {
        modifyAllButtons(event, true);

        emailCooldown.add(event.getUser().getIdLong());

        join(Bot.BOT.jda.getGuildById(ids[2]), event.getUser());
    }

    /**
     * Verifies the code in the most recent 100 messages. If found, {@link #sendRules(ButtonClickEvent, String[])} will
     * be called. If not, an error message will be displayed to the user
     *
     * @param event A button click event
     * @param ids The button's ids
     */
    public static void verifyCode(ButtonClickEvent event, String[] ids) {
        event.getChannel().getHistory().retrievePast(100).queue(messages -> {
            boolean validCode = messages.stream().anyMatch(message -> {
                if (message.getAuthor().getIdLong() == event.getUser().getIdLong())
                    return message.getContentRaw().contains(ids[2]);
                else
                    return false;
            });

            if(validCode) {
                emailStatus.put(event.getUser().getIdLong(), new UserVerification(Long.parseLong(ids[3]), true));
                executor.schedule(() -> {
                    emailStatus.remove(event.getUser().getIdLong());
                }, 301, TimeUnit.SECONDS);

                sendRules(event, ids);
            } else {
                Guild g = Bot.BOT.jda.getGuildById(ids[3]);
                event.getChannel().retrieveMessageById(event.getMessageId()).queue(message ->
                    event.getChannel().sendMessageEmbeds(
                        Bot.buildEmbed(
                            (g != null ? g.getName() : "Unknown") + " Server Verification",
                            "You must verify before you gain access to this server!",
                            Bot.RED,
                            new MessageEmbed.Field[]{
                                    new MessageEmbed.Field(
                                            "Couldn't Find Valid Code!",
                                            "Could not find the valid code within the most recent 100 messages. Please submit a valid " +
                                                    "code and press **Submit Code**.",
                                            false
                                    )
                            },
                            "This message will delete in under 30 seconds."
                        ).build()
                    ).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS))
                );
            }
        }
        );
    }

    /**
     * If a correct code was submitted, this method will display the rules with two buttons for agreeing and disagreeing
     * to the rules (verification:rules-agree:[guild-id] and verification:rules-disagree:[guild-id] respectively). If
     * the user agrees to the rules {@link #rulesAgree(ButtonClickEvent, String[])} will be called. If the user disagrees
     * {@link #rulesDecline(ButtonClickEvent, String[])} will be called.
     *
     * @param event A button click event
     * @param ids The button's ids
     */
    private static void sendRules(ButtonClickEvent event, String[] ids) {
        modifyAllButtons(event, true);

        emailCooldown.remove(event.getUser().getIdLong());

        Guild g = Bot.BOT.jda.getGuildById(ids[3]);
        String guildName = g == null ? "**FAILED TO FIND SERVER**" : g.getName();

        event.getChannel().sendMessageEmbeds(
            Bot.buildEmbed(
                (g != null ? g.getName() : "Unknown") + " Server Verification",
                "You must verify before you gain access to this server!",
                Bot.RED,
                new MessageEmbed.Field[]{
                    new MessageEmbed.Field(
                        "Valid Code Submitted!",
                        "You have submitted the correct verification code. Now, please review the rules and once done, select " +
                                "\"Agree to Rules\". *By agreeing to the rules you must follow them and are willing to accept the " +
                                " punishment for breaking them.*",
                        false
                    ),
                    new MessageEmbed.Field(
                            "I Don't Agree with the Rules! What do I Do?",
                            "If you do not agree to the rules, select \"Decline Rule Agreement\". By selecting this, you will promptly " +
                                    "be removed from the sever.",
                            false
                    )
                }
            ).build(),
                Bot.buildEmbed(
                        "Server Rules",
                        "These are the rules for the " + guildName + " server",
                        Bot.RED,
                        new MessageEmbed.Field[]{
                                new MessageEmbed.Field(
                                        "Rules!",
                                        "1. Please keep the server PG rated.\n" +
                                                "2. Do your best to keep everything in the correct channel.\n" +
                                                "3. Do not spam the chat, excessively ping people, or act in a rude manner.\n" +
                                                "4. Do not use the chat to cheat on homework or tests. Helping classmates with homework is encouraged.\n" +
                                                "Breaking these rules may result in a mute or a ban.",
                                        false
                                ),
                        }
                ).build()
        ).setActionRow(
                Button.success("verification:rules-agree:" + ids[3], "Agree to Rules"),
                Button.danger("verification:rules-decline:" + ids[3], "Decline Rule Agreement")
        ).queue();
    }

    /**
     * Called when the user agrees to the rules. This will prompt the user with a message to select their school grade level.
     * There are several buttons in this message: *verification:confirm-grade:(grade):(guild-id), verification:(grade-level):(guild-id).
     * Only one of the 4 grade level buttons can be "active" at a time and the "confirm-grade" button calls {@link #invitedBy(ButtonClickEvent, String[])}
     *
     * *grade and guild-id are set when the user clicks one of the 4 grade level buttons calling {@link #selectGradeLevel(ButtonClickEvent, String[])}
     *
     * @param event The button click event
     * @param ids The button's ids
     */
    public static void rulesAgree(ButtonClickEvent event, String[] ids) {
        modifyAllButtons(event, true);

        Guild g = Bot.BOT.jda.getGuildById(ids[2]);
        event.getChannel().sendMessageEmbeds(
            Bot.buildEmbed(
                (g != null ? g.getName() : "Unknown") + " Server Verification",
                "You must verify before you gain access to this server!",
                Bot.RED,
                new MessageEmbed.Field[]{
                    new MessageEmbed.Field(
                        "Rules Agreed To!",
                        "Thank you for agreeing to the server's roles. Now, please select your **CORRECT** grade level." /*then " +
                                "select the " + Main.CHECK_EMOJI + " button."*/,
                        false
                    ),
                    new MessageEmbed.Field(
                        "I Don't See my Grade Level!",
                        "If you do not see your grade level please select \"Other\" and you will be handled from there.",
                        false
                    )
                }
            ).build()
        ).setActionRow(
            //Button.success("verification:confirm-grade:", Emoji.fromMarkdown(Main.CHECK_EMOJI)),
            Button.secondary("verification:freshman:" + ids[2], "Freshman"),
            Button.secondary("verification:sophomore:" + ids[2], "Sophomore"),
            Button.secondary("verification:junior:"+ ids[2], "Junior"),
            Button.secondary("verification:senior:" + ids[2], "Senior"),
            Button.secondary("verification:other:" + ids[2], "Other")
        ).queue();
    }

    /**
     * Determines the user's grade level. If the button is active, or "PRIMARY" the button will be disabled (switched to "SECONDARY") and vice
     * versa. When enabled, the confirm-grade button will posses the grade level and the guild ID of the selected button. When disabled
     * they will be removed. Only one can be active at a time and changing the active one will disable the rest
     *
     * @param event The button click event
     * @param ids The button's ids
     */
    public static void selectGradeLevel(ButtonClickEvent event, String[] ids) {
        assert event.getButton() != null;
        assert event.getMessage() != null;
        MessageBuilder builder = new MessageBuilder(event.getMessage());

        builder.setActionRows(
            ActionRow.of(
                event.getMessage().getActionRows().get(0).getButtons().stream().map(button ->
                    {
                        if(Objects.equals(button.getId(), event.getComponentId()))
                            return button.withStyle(ButtonStyle.PRIMARY).asDisabled();
                        else
                            return button.withStyle(ButtonStyle.SECONDARY).asDisabled();
                    }
                ).collect(Collectors.toList())
            )
        );

        event.getMessage().editMessage(builder.build()).queue();
        if(!ids[1].equals("other"))
            invitedBy(event, ids);
    }

    /**
     * Adds the selected grade level to the user and tells them they have been admitted to the guild
     *
     * @param event The button click event
     * @param ids The button's ids
     */
    public static void invitedBy(ButtonClickEvent event, String[] ids) {
        Guild g = Bot.BOT.jda.getGuildById(ids[2]);

        event.getChannel().sendMessageEmbeds(
                Bot.buildEmbed(
                        (g != null ? g.getName() : "Unknown") + " Server Verification",
                        "You must verify before you gain access to this server!",
                        Bot.RED,
                        new MessageEmbed.Field[]{
                                new MessageEmbed.Field(
                                        "Grade Selected",
                                        "You have selected `" + ids[1] + "`.",
                                        false
                                ),
                                new MessageEmbed.Field(
                                        "Who Invited You?",
                                        "Please indicate who invited you. After typing out their username, please select \"Submit User\"." +
                                                "This must be no more than 25 characters. If no one invited you please select \"Skip\"",
                                        false
                                ),
                        }
                ).build()
        ).setActionRow(
                Button.primary("verification:invited-by:" + ids[1] + ":" + ids[2], "Submit User"),
                Button.primary("verification:skip:none:" + ids[1] + ":" + ids[2], "Skip")
        ).queue();
    }

    public static void getInviter(ButtonClickEvent event, String[] ids) {
        event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                    List<Message> messageList = messages.stream().filter(message ->
                            message.getContentRaw().length() <= 25 && message.getAuthor().getIdLong() == event.getUser().getIdLong()
                    ).collect(Collectors.toList());

                    if(messageList.size() > 0)
                        checkInviter(event, messageList.get(0), ids);
                    else {
                        Guild g = Bot.BOT.jda.getGuildById(ids[2]);
                        event.getChannel().retrieveMessageById(event.getMessageId()).queue(message ->
                                event.getChannel().sendMessageEmbeds(
                                        Bot.buildEmbed(
                                                (g != null ? g.getName() : "Unknown") + " Server Verification",
                                                "You must verify before you gain access to this server!",
                                                Bot.RED,
                                                new MessageEmbed.Field[]{
                                                        new MessageEmbed.Field(
                                                                "Couldn't Find an Inviter!",
                                                                "Could not find the name of the person who invited you in the most recent 100 messages. " +
                                                                        "Please submit their name (must be no more than 25 characters) and press **Submit Email**.",
                                                                false
                                                        )
                                                },
                                                "This message will delete in under 30 seconds."
                                        ).build()
                                ).queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS))
                        );
                    }
                }
        );
    }

    public static void checkInviter(ButtonClickEvent event, Message message, String[] ids) {
        if(event.getButton() != null)
            modifyAllButtons(event, true);

        String user = message.getContentDisplay();
        Guild g = Bot.BOT.jda.getGuildById(ids[3]);

        event.getChannel().sendMessageEmbeds(Bot.buildEmbed(
                (g != null ? g.getName() : "Unknown") + " Server Verification",
                "You must verify before you gain access to this server!",
                Bot.RED,
                new MessageEmbed.Field[]{
                        new MessageEmbed.Field(
                                "Is This Who you Were Invited By?",
                                "Is `" + user + "` the correct user? If yes, select \"Yes\". If not, select \"No\".",
                                false),
                }
        ).build()).setActionRow(
                Button.primary("verification:inviter:" + user + ":" + ids[2] + ":" + ids[3], "Yes"),
                Button.secondary("verification:inviter:no:" + event.getMessageId(), "No")
        ).queue();
    }

    public static void handleInviterCheck(ButtonClickEvent event, String[] ids) {
        if ("no".equals(ids[2]))
            event.getChannel().retrieveMessageById(ids[3]).queue(message -> {
                modifyAllButtons(message, false);

                if (event.getMessage() != null)
                    event.getMessage().delete().queue();
                else
                    event.deferEdit().setActionRow().queue();
            });
        else {
            modifyAllButtons(event, true);
            admitToServer(event, ids);
        }
    }

    public static void admitToServer(ButtonClickEvent event, String[] ids) {
        assert event.getMessage() != null;
        Guild g = Bot.BOT.jda.getGuildById(ids[4]);
        if(g != null) {
            String grade = ids[3].substring(0,1).toUpperCase() + ids[3].substring(1);
            LinkedHashMap<String, Long> roles = Bot.guildData.get(g.getIdLong()).roles;
            LinkedHashMap<String, Long> channels = Bot.guildData.get(g.getIdLong()).channels;

            Role r = g.getRoleById(roles.get(grade));

            if(r == null)
                event.getChannel().sendMessageEmbeds(Bot.buildEmbed(
                        "Failed to Assign Role",
                        "Could not assign role: " + grade + ". Please use `/help request`",
                        Bot.RED,
                        new MessageEmbed.Field[]{}
                ).build()).queue();
            else
                g.addRoleToMember(event.getUser().getIdLong(), r).queue();

            event.getChannel().sendMessageEmbeds(
                    Bot.buildEmbed(
                            "Admitted to Server!",
                            "Congrats, you have joined the **" + g.getName() + "** server!",
                            Bot.DARK_GREEN,
                            new MessageEmbed.Field[]{
                                    new MessageEmbed.Field(
                                            "Complete the Verification Process Survey!",
                                            "Thank you for completing the verification process in its early form. As we are still " +
                                                    "fine tuning this process we kindly ask you to complete a survey on the quality of" +
                                                    " the verification process. This is an optional survey but your response and honesty " +
                                                    "would be appreciated: https://forms.gle/e7bin2x1XaqRrYL86.",
                                            false
                                    )
                            }
                    ).build()
            ).queue();

            TextChannel c = g.getTextChannelById(channels.get("bot-logs"));
            if(c == null)
                return;

            c.sendMessageEmbeds(Bot.buildEmbed(
                    "User Verified",
                    "Verified a user",
                    Bot.RED,
                    new MessageEmbed.Field[]{
                            new MessageEmbed.Field(
                                    "User",
                                    event.getUser().getAsMention(),
                                    true
                            ),
                            new MessageEmbed.Field(
                                    "Grade Level",
                                    grade,
                                    true
                            ),
                            new MessageEmbed.Field(
                                    "Invited By",
                                    ids[2],
                                    true
                            )
                    }
            ).setThumbnail(event.getUser().getAvatarUrl()).build()).queue();
        } else
            event.getChannel().sendMessageEmbeds(Bot.buildEmbed(
                    "Couldn't Fetch Server",
                    "Sorry I couldn't fetch the guild. Please use `/help request`... if the guild still exists.",
                    Bot.RED,
                    new MessageEmbed.Field[]{}
            ).build()).queue();
    }

    /**
     * Called if the user does not agree with the rules. If called, it will kick the user from the server
     *
     * @param event The button click event
     * @param ids The button's ids
     */
    public static void rulesDecline(ButtonClickEvent event, String[] ids) {
        modifyAllButtons(event, true);

        Guild g = Bot.BOT.jda.getGuildById(ids[2]);
        // TODO Proper error
        if(g != null)
            g.kick(event.getUser().getId(), "You did not agree to the server's rules.").queue();
        else
            event.getChannel().sendMessageEmbeds(Bot.buildEmbed(
                    "Failed to Kick",
                    "I could not kick you as I couldn't retrieve the server. Please use `/help request`.",
                    Bot.RED,
                    new MessageEmbed.Field[]{}
            ).build()).queue();
    }

    /**
     * Ran if the user selects the verification:other button.
     *
     * @param event A button click event
     * @param ids The button's ids
     */
    public static void otherGrade(ButtonClickEvent event, String[] ids) {
        selectGradeLevel(event, ids);

        event.getChannel().sendMessageEmbeds(Bot.buildEmbed(
                "Incorrect Grade Selected",
                "You selected the \"Other\" option when attempting to verify. This means you're currently" +
                        " not in High School and will not be granted permission to join the server. If you think this was a mistake or have " +
                        "any other questions, please contact us using `/help request`.",
                Bot.RED,
                new MessageEmbed.Field[]{}
        ).build()).queue();
    }
}
