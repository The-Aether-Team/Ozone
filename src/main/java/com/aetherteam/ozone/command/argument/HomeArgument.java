package com.aetherteam.ozone.command.argument;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.concurrent.CompletableFuture;

public class HomeArgument implements ArgumentType<String> {
    public HomeArgument() { }

    public static HomeArgument home() {
        return new HomeArgument();
    }

    public static String getHome(CommandContext<CommandSourceStack> source, String name) {
        return source.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) {
        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return getClientSuggestion(builder);
    }

    @OnlyIn(Dist.CLIENT)
    private static CompletableFuture<Suggestions> getClientSuggestion(SuggestionsBuilder builder) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            return SharedSuggestionProvider.suggest(player.getData(OzoneDataAttachments.PLAYER).getHomeCommandSuggestions(), builder);
        }
        return Suggestions.empty();
    }
}
