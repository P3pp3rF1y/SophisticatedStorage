package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG;

public class ItemTagProvider extends ItemTagsProvider {
	public ItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
		super(packOutput, registries, SophisticatedStorage.MOD_ID);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		tag(BASE_TIER_WOODEN_STORAGE_TAG).add(ModBlocks.BARREL_ITEM.get(), ModBlocks.CHEST_ITEM.get());

		TagAppender<Item, Item> allStorageTag = tag(ModBlocks.ALL_STORAGE_TAG);
		BuiltInRegistries.ITEM.stream().filter(item -> item instanceof StorageBlockItem).forEach(allStorageTag::add);

		TagAppender<Item, Item> upgradeTag = tag(ModItems.STORAGE_UPGRADE_TAG);
		BuiltInRegistries.ITEM.entrySet().stream()
				.filter(entry -> entry.getKey().location().getNamespace().equals(SophisticatedStorage.MOD_ID) && entry.getValue() instanceof UpgradeItemBase)
				.map(Map.Entry::getValue).forEach(item -> {
					ResourceLocation location = BuiltInRegistries.ITEM.getKey(item);
					if (location.getPath().contains("/")) {
						upgradeTag.addOptional(item);
					} else {
						upgradeTag.add(item);
					}
				});

		tag(Tags.Items.CHESTS).add(ModBlocks.CHEST_ITEM.get(), ModBlocks.COPPER_CHEST_ITEM.get(), ModBlocks.IRON_CHEST_ITEM.get(),
				ModBlocks.GOLD_CHEST_ITEM.get(), ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.NETHERITE_CHEST_ITEM.get());
		tag(Tags.Items.BARRELS).add(ModBlocks.BARREL_ITEM.get(), ModBlocks.COPPER_BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(),
				ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get());
		tag(Tags.Items.BARRELS_WOODEN).add(ModBlocks.BARREL_ITEM.get());
	}
}
