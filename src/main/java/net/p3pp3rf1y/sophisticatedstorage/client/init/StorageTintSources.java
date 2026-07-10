package net.p3pp3rf1y.sophisticatedstorage.client.init;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.client.GenericWoodStorageTintCache;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;
import org.jspecify.annotations.Nullable;

public class StorageTintSources {
	public static void register(RegisterColorHandlersEvent.ItemTintSources event) {
		event.register(SophisticatedStorage.getIdentifier("main"), Main.MAP_CODEC);
		event.register(SophisticatedStorage.getIdentifier("accent"), Accent.MAP_CODEC);
	}

	public record Main(int defaultColor) implements ItemTintSource {
		public static final MapCodec<Main> MAP_CODEC = RecordCodecBuilder
				.mapCodec(instance -> instance.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(Main::defaultColor)).apply(instance, Main::new));

		public Main(int defaultColor) {
			this.defaultColor = ARGB.opaque(defaultColor);
		}

		@Override
		public int calculate(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity) {
			return StorageBlockItem.getMainColorFromComponentHolder(itemStack).orElseGet(() -> WoodStorageBlockItem.getWoodType(itemStack)
					.filter(GenericWoodStorageHelper::isGenericWood).map(GenericWoodStorageTintCache::getMainColor).orElse(-1));
		}

		@Override
		public MapCodec<? extends ItemTintSource> type() {
			return MAP_CODEC;
		}
	}

	public record Accent(int defaultColor) implements ItemTintSource {
		public static final MapCodec<Accent> MAP_CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(Accent::defaultColor)).apply(instance, Accent::new));

		public Accent(int defaultColor) {
			this.defaultColor = ARGB.opaque(defaultColor);
		}

		@Override
		public int calculate(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity) {
			return StorageBlockItem.getAccentColorFromComponentHolder(itemStack).orElseGet(() -> WoodStorageBlockItem.getWoodType(itemStack)
					.filter(GenericWoodStorageHelper::isGenericWood).map(GenericWoodStorageTintCache::getAccentColor).orElse(-1));
		}

		@Override
		public MapCodec<? extends ItemTintSource> type() {
			return MAP_CODEC;
		}
	}
}
