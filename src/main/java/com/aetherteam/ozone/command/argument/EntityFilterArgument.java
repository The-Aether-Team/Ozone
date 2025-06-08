package com.aetherteam.ozone.command.argument;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.aetherteam.ozone.claims.EntityFilter;
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

public class EntityFilterArgument implements ArgumentType<EntityFilter> {
    private static final Collection<String> EXAMPLES = Arrays.asList("owner_only", "anyone");
    public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
        arg -> Component.translatableEscape("argument.ozone_utilities.entity_filter.invalid", arg)
    );

    private final EntityFilter min;

    private EntityFilterArgument(EntityFilter min) {
        this.min = min;
    }

    public static EntityFilterArgument entityFilter() {
        return new EntityFilterArgument(EntityFilter.NO_ONE);
    }

    public static EntityFilterArgument entityFilterAtLeast(EntityFilter min) {
        return new EntityFilterArgument(min);
    }

    public static EntityFilter getEntityFilter(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, EntityFilter.class);
    }

    @Override
    public EntityFilter parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.readUnquotedString();
        EntityFilter entityFilter = EntityFilter.getByName(s);
        if (entityFilter != null && min.compareTo(entityFilter) <= 0) {
            return entityFilter;
        } else {
            throw ERROR_INVALID_VALUE.createWithContext(reader, s);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(EntityFilter.getNames(min), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
