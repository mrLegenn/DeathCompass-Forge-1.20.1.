package com.example.deathcompass;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

@Mod(DeathCompassMod.MOD_ID)
public class DeathCompassMod {
    public static final String MOD_ID = "deathcompass";
    private static final String DEATH_DATA = "DeathCompassLastDeath";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String DIMENSION = "Dimension";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> DEATH_COMPASS = ITEMS.register(
            "death_compass",
            () -> new DeathCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
    );

    public DeathCompassMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DEATH_COMPASS);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;

            BlockPos pos = player.blockPosition();
            CompoundTag data = new CompoundTag();
            data.putInt(X, pos.getX());
            data.putInt(Y, pos.getY());
            data.putInt(Z, pos.getZ());
            data.putString(DIMENSION, player.level().dimension().location().toString());
            player.getPersistentData().put(DEATH_DATA, data);
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            if (!event.isWasDeath()) return;

            CompoundTag oldData = event.getOriginal().getPersistentData();
            if (oldData.contains(DEATH_DATA, CompoundTag.TAG_COMPOUND)) {
                event.getEntity().getPersistentData().put(
                        DEATH_DATA, oldData.getCompound(DEATH_DATA).copy()
                );
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerRespawnEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;

            CompoundTag data = player.getPersistentData().getCompound(DEATH_DATA);
            if (data.isEmpty() || !data.contains(DIMENSION)) return;

            BlockPos pos = new BlockPos(data.getInt(X), data.getInt(Y), data.getInt(Z));
            ItemStack compass = new ItemStack(DEATH_COMPASS.get());
            CompoundTag tag = compass.getOrCreateTag();
            tag.put("LodestonePos", NbtUtils.writeBlockPos(pos));
            tag.putString("LodestoneDimension", data.getString(DIMENSION));
            tag.putBoolean("LodestoneTracked", false);

            if (!player.getInventory().add(compass)) {
                player.drop(compass, false);
            }
        }
    }

    public static class DeathCompassItem extends CompassItem {
        public DeathCompassItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("LodestonePos", CompoundTag.TAG_COMPOUND)) return;

            BlockPos pos = NbtUtils.readBlockPos(tag.getCompound("LodestonePos"));
            tooltip.add(Component.translatable("tooltip.deathcompass.last_death")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("X: " + pos.getX() + "  Y: " + pos.getY() + "  Z: " + pos.getZ())
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(tag.getString("LodestoneDimension"))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
