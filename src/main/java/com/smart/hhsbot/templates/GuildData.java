package com.smart.hhsbot.templates;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.LinkedHashMap;

public class GuildData {
    private String emailAddress;
    public LinkedHashMap<String, Long> roles;
    public LinkedHashMap<String, Long> channels;
    public LinkedHashMap<String, Long> categories;

    public GuildData() {
        roles = new LinkedHashMap<>();
        channels = new LinkedHashMap<>();
        categories = new LinkedHashMap<>();
    }

     public GuildData addRole(String name, Role r) {
         roles.put(name, r.getIdLong());
         return this;
     }

     public GuildData addChannel(String name, TextChannel t) {
         channels.put(name, t.getIdLong());
         return this;
     }

      public GuildData addCategory(String name, Category c) {
         categories.put(name, c.getIdLong());
         return this;
      }
}
