package com.aetherteam.ozone.command.admin;

import java.util.UUID;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.function.TriFunction;

import com.aetherteam.ozone.OzoneConfig;
import com.aetherteam.ozone.attachment.GlobalChunkPos;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.attachment.OzoneLevelAttachment;
import com.aetherteam.ozone.command.argument.EntityFilterArgument;
import com.aetherteam.ozone.command.argument.PlayerFilterArgument;
import com.aetherteam.ozone.command.argument.SingleGameProfileArgument;
import com.aetherteam.ozone.network.packet.clientbound.ChunkClaimPacket;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClaimCommand {
    private static final SimpleCommandExceptionType ERROR_NO_CLAIM = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.claim.error.noClaim"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_CLAIMED = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.claim.error.alreadyOwner"));
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("claim")
                .requires(sender -> sender.hasPermission(3))
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(
                                    context -> removeClaim(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"))
                                )
                        )
                        .executes(
                            context -> removeClaim(context.getSource(), getLoadedBlockPos(context.getSource()))
                        )
                )
                .then(
                    Commands.literal("info")
                        .then(
                            Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(
                                    context -> getClaimInfo(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"))
                                )
                        )
                        .executes(
                            context -> getClaimInfo(context.getSource(), getLoadedBlockPos(context.getSource()))
                        )
                )
                .then(
                    addFilters(Commands.literal("filter"))
                )
                .then(
                    Commands.literal("owner")
                        .then(
                            Commands.argument("target", SingleGameProfileArgument.gameProfile())
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(
                                            context -> setClaim(
                                                    context.getSource(),
                                                    BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                                    SingleGameProfileArgument.getGameProfile(context, "target")
                                                )
                                        )
                                )
                                .executes(
                                    context -> setClaim(
                                            context.getSource(),
                                            getLoadedBlockPos(context.getSource()),
                                            SingleGameProfileArgument.getGameProfile(context, "target")
                                        )
                                )
                        )
                        .executes(
                            context -> setClaim(
                                    context.getSource(),
                                    getLoadedBlockPos(context.getSource()),
                                    context.getSource().getPlayerOrException().getGameProfile()
                                )
                        )
                )
                .then(
                    Commands.literal("config")
                        .then(
                            Commands.literal("maxChunkClaims")
                                .then(
                                    Commands.argument("maxChunkClaims", IntegerArgumentType.integer(0))
                                        .executes(
                                            context -> {
                                                int maxChunkClaims = IntegerArgumentType.getInteger(context, "maxChunkClaims");
                                                OzoneConfig.setMaxChunkClaims(maxChunkClaims);
                                                context.getSource().sendSuccess(() -> Component.literal("maxChunkClaims has been set to: " + maxChunkClaims), true);
                                                return 1;
                                            }
                                        )
                                )
                                .executes(
                                    context -> {
                                        int maxChunkClaims = OzoneConfig.getMaxChunkClaims();
                                        context.getSource().sendSuccess(() -> Component.literal("maxChunkClaims is currently set to: " + maxChunkClaims), true);
                                        return 1;
                                    }
                                )
                        )
                )
                .then(
                    Commands.literal("list")
                        .executes(
                            context -> {
                                UUID owner = context.getSource().getPlayerOrException().getUUID();
                                OzoneLevelAttachment levelData = context.getSource().getServer().overworld().getData(OzoneDataAttachments.LEVEL);
                                for (var claim : levelData.getPlayerClaims(owner)) {
                                    context.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.ozone_utilities.claim.list", "%s in %s", Component.translatable(claim.dimension().location().toLanguageKey("dimension")), Component.translationArg(claim.pos())), true);
                                }
                                return 1;
                            }
                        )
                )
        );
    }

    private static <B extends ArgumentBuilder<CommandSourceStack, ?>> B addFilters(B builder) {
        for (final var field : OzoneChunkAttachment.PlayerFilterField.values()) {
            builder.then(
                Commands.literal(field.name())
                    .then(
                        Commands.argument("filter", PlayerFilterArgument.playerFilterAtLeast(field.getMinValue()))
                            .executes(
                                context -> setFilter(
                                        context.getSource(),
                                        getLoadedBlockPos(context.getSource()),
                                        field,
                                        PlayerFilterArgument.getPlayerFilter(context, "filter"),
                                        ChunkClaimPacket.ChangePlayerFilter::new
                                    )
                            )
                    )
            );
        }
        for (final var field : OzoneChunkAttachment.EntityFilterField.values()) {
            builder.then(
                Commands.literal(field.name())
                    .then(
                        Commands.argument("filter", EntityFilterArgument.entityFilterAtLeast(field.getMinValue()))
                            .executes(
                                context -> setFilter(
                                        context.getSource(),
                                        getLoadedBlockPos(context.getSource()),
                                        field,
                                        EntityFilterArgument.getEntityFilter(context, "filter"),
                                        ChunkClaimPacket.ChangeEntityFilter::new
                                    )
                            )
                    )
            );
        }
        return builder;
    }

    public static <F extends BiConsumer<? super OzoneChunkAttachment, ? super T>, T> int setFilter(CommandSourceStack source, BlockPos pos, F field, T filter, TriFunction<? super GlobalChunkPos, ? super F, ? super T, ? extends ChunkClaimPacket> packetCreator) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        if (!level.isAreaLoaded(pos, 0)) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        }

        ChunkAccess chunk = level.getChunk(pos);
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) {
            throw ERROR_NO_CLAIM.create();
        }

        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        if (!claim.hasOwner()) {
            chunk.removeData(OzoneDataAttachments.CHUNK);
            throw ERROR_NO_CLAIM.create();
        }

        field.accept(claim, filter);
        PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), packetCreator.apply(GlobalChunkPos.of(level.dimension(), chunk.getPos()), field, filter));
        chunk.setData(OzoneDataAttachments.CHUNK, claim);
        
        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.claim.filter.success"), true);
        return 1;
    }

    public static int getClaimInfo(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        if (!level.isAreaLoaded(pos, 0)) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        }
        
        ChunkAccess chunk = level.getChunk(pos);
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) {
            throw ERROR_NO_CLAIM.create();
        }

        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        if (!claim.hasOwner()) {
            chunk.removeData(OzoneDataAttachments.CHUNK);
            throw ERROR_NO_CLAIM.create();
        }

        Component ownerName = source.getServer().getProfileCache().get(claim.getOwner().get()).map(profile -> Component.literal(profile.getName())).orElseGet(() -> Component.literal("Unknown").withStyle(ChatFormatting.RED));

        source.sendSuccess(
            () -> Component.translatable(
                "commands.ozone_utilities.claim.info.location",
                Component.translationArg(chunk.getPos()),
                Component.translationArg(level.getDescription())
            ).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable(
                "commands.ozone_utilities.claim.info.owner",
                Component.translationArg(ownerName)
            ).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> claim.getSurveyorTableLocation()
                .map(surveyorTableLocation -> Component.translatable("commands.ozone_utilities.claim.info.surveyorTableLocation", surveyorTableLocation.getX(), surveyorTableLocation.getY(), surveyorTableLocation.getZ()))
                .orElseGet(() -> Component.translatable("commands.ozone_utilities.claim.info.surveyorTableLocation.unknown"))
            .withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowExplosions", Component.translationArg(claim.getAllowExplosions().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowPlace", Component.translationArg(claim.getAllowPlace().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowBreak", Component.translationArg(claim.getAllowBreak().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowInteractWithContainers", Component.translationArg(claim.getAllowInteractWithContainers().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowInteractWithDoors", Component.translationArg(claim.getAllowInteractWithDoors().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowInteractWithRedstone", Component.translationArg(claim.getAllowInteractWithRedstone().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowInteractWithRedstoneActivators", Component.translationArg(claim.getAllowInteractWithRedstoneActivators().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowInteractWithSigns", Component.translationArg(claim.getAllowInteractWithSigns().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowInteractWithOther", Component.translationArg(claim.getAllowInteractWithOther().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        source.sendSuccess(
            () -> Component.translatable("commands.ozone_utilities.claim.info.allowDrop", Component.translationArg(claim.getAllowDrop().getDescription())).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        );
        return 1;
    }

    public static int setClaim(CommandSourceStack source, BlockPos pos, GameProfile newOwner) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        if (!level.isAreaLoaded(pos, 0)) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        }

        ChunkAccess chunk = level.getChunk(pos);
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) {
            throw ERROR_NO_CLAIM.create();
        }

        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        if (!claim.hasOwner()) {
            throw ERROR_NO_CLAIM.create();
        }
        if (claim.isOwner(newOwner.getId())) {
            throw ERROR_ALREADY_CLAIMED.create();
        }
        
        claim.changeOwner(level, newOwner.getId());
        chunk.setData(OzoneDataAttachments.CHUNK, claim);
        
        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.claim.changeOwner.success", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    public static int removeClaim(CommandSourceStack source, Position pos) throws CommandSyntaxException {
        return removeClaim(source, BlockPos.containing(pos));
    }

    public static int removeClaim(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        if (!level.isAreaLoaded(pos, 0)) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        }
        
        ChunkAccess chunk = level.getChunk(pos);
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) {
            throw ERROR_NO_CLAIM.create();
        }

        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        if (!claim.hasOwner()) {
            throw ERROR_NO_CLAIM.create();
        }
        
        claim.removeOwner(level);
        chunk.removeData(OzoneDataAttachments.CHUNK);
        
        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.claim.remove.success", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    private static BlockPos getLoadedBlockPos(CommandSourceStack source) throws CommandSyntaxException {
        BlockPos blockpos = BlockPos.containing(source.getPosition());
        if (!source.getUnsidedLevel().isInWorldBounds(blockpos)) {
            throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
        } else if (!source.getUnsidedLevel().isLoaded(blockpos)) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        } else {
            return blockpos;
        }
    }
}
