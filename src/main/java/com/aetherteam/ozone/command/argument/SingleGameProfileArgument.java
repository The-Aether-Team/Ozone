package com.aetherteam.ozone.command.argument;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;

public class SingleGameProfileArgument implements ArgumentType<SingleGameProfileArgument.Result> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
    
    public static GameProfile getGameProfile(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, SingleGameProfileArgument.Result.class).getName(context.getSource());
    }

    public static SingleGameProfileArgument gameProfile() {
        return new SingleGameProfileArgument();
    }

    public <S> SingleGameProfileArgument.Result parse(StringReader reader, S p_353124_) throws CommandSyntaxException {
        return parse(reader, EntitySelectorParser.allowSelectors(p_353124_));
    }

    public SingleGameProfileArgument.Result parse(StringReader reader) throws CommandSyntaxException {
        return parse(reader, true);
    }

    private static SingleGameProfileArgument.Result parse(StringReader reader, boolean allowSelectors) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '@') {
            EntitySelectorParser entityselectorparser = new EntitySelectorParser(reader, allowSelectors);
            EntitySelector entityselector = entityselectorparser.parse();
            if (entityselector.includesEntities()) {
                throw EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(reader);
            } else if (entityselector.getMaxResults() > 1) {
                reader.setCursor(0);
                throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.createWithContext(reader);
            } else {
                return new SingleGameProfileArgument.SelectorResult(entityselector);
            }
        } else {
            int i = reader.getCursor();

            while (reader.canRead() && reader.peek() != ' ') {
                reader.skip();
            }

            String s = reader.getString().substring(i, reader.getCursor());
            return source -> {
                Optional<GameProfile> optional = source.getServer().getProfileCache().get(s);
                return optional.orElseThrow(GameProfileArgument.ERROR_UNKNOWN_PLAYER::create);
            };
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof SharedSuggestionProvider sharedsuggestionprovider) {
            StringReader stringreader = new StringReader(builder.getInput());
            stringreader.setCursor(builder.getStart());
            EntitySelectorParser entityselectorparser = new EntitySelectorParser(stringreader, EntitySelectorParser.allowSelectors(sharedsuggestionprovider));

            try {
                entityselectorparser.parse();
            } catch (CommandSyntaxException commandsyntaxexception) {
            }

            return entityselectorparser.fillSuggestions(
                builder, p_353116_ -> SharedSuggestionProvider.suggest(sharedsuggestionprovider.getOnlinePlayerNames(), p_353116_)
            );
        } else {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @FunctionalInterface
    public interface Result extends GameProfileArgument.Result {
        GameProfile getName(CommandSourceStack source) throws CommandSyntaxException;

        @Override
        default Collection<GameProfile> getNames(CommandSourceStack source) throws CommandSyntaxException {
            return Collections.singleton(this.getName(source));
        }
    }

    public static class SelectorResult implements SingleGameProfileArgument.Result {
        private final EntitySelector selector;

        public SelectorResult(EntitySelector selector) {
            this.selector = selector;
        }

        @Override
        public GameProfile getName(CommandSourceStack source) throws CommandSyntaxException {
            var profile = this.selector.findSinglePlayer(source);
            return profile.getGameProfile();
        }
    }
}
