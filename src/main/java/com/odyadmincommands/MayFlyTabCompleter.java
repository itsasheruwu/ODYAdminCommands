package com.odyadmincommands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class MayFlyTabCompleter implements TabCompleter {

    private static final List<String> MODES = Arrays.asList("on", "off", "toggle");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> names = Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partial))
                    .collect(Collectors.toList());
            completions.addAll(names);
        } else if (args.length == 2) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            for (String mode : MODES) {
                if (mode.startsWith(partial)) {
                    completions.add(mode);
                }
            }
        }

        return completions;
    }
}
