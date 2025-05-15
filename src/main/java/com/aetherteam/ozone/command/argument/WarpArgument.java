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
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.concurrent.CompletableFuture;

public class WarpArgument implements ArgumentType<String> {
    public WarpArgument() { }

    public static WarpArgument warp() {
        return new WarpArgument();
    }

    public static String getWarp(CommandContext<CommandSourceStack> source, String name) {
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
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            return SharedSuggestionProvider.suggest(level.getData(OzoneDataAttachments.LEVEL).getWarpCommandSuggestions(), builder);
        }
        return Suggestions.empty();
    }
}
