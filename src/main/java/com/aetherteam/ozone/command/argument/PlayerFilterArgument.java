package com.aetherteam.ozone.command.argument;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.aetherteam.ozone.claims.PlayerFilter;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class PlayerFilterArgument implements ArgumentType<PlayerFilter> {
    private static final Collection<String> EXAMPLES = Arrays.asList("owner_only", "anyone");
    public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
        arg -> Component.translatableEscape("argument.ozone_utilities.player_filter.invalid", arg)
    );

    private final PlayerFilter min;

    private PlayerFilterArgument(PlayerFilter min) {
        this.min = min;
    }

    public static PlayerFilterArgument playerFilter() {
        return new PlayerFilterArgument(PlayerFilter.NO_ONE);
    }

    public static PlayerFilterArgument playerFilterAtLeast(PlayerFilter min) {
        return new PlayerFilterArgument(min);
    }

    public static PlayerFilter getPlayerFilter(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, PlayerFilter.class);
    }

    @Override
    public PlayerFilter parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.readUnquotedString();
        PlayerFilter playerFilter = PlayerFilter.getByName(s);
        if (playerFilter != null && min.compareTo(playerFilter) <= 0) {
            return playerFilter;
        } else {
            throw ERROR_INVALID_VALUE.createWithContext(reader, s);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(PlayerFilter.getNames(min), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
