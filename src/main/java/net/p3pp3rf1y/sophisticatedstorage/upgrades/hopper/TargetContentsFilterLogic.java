package net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterAttributes;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TargetContentsFilterLogic extends ContentsFilterLogic {
	private Set<ItemStackKey> inventoryFilterStacks = new HashSet<>();
	private final LoadingCache<ResourceHandler<ItemResource>, Set<ItemStackKey>> inventoryCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build(new CacheLoader<>() {
		@Override
		public Set<ItemStackKey> load(ResourceHandler<ItemResource> inventory) {
			return InventoryHelper.getUniqueStacks(inventory);
		}
	});

	public TargetContentsFilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int filterSlotCount, Supplier<InventoryHandler> getInventoryHandler, MemorySettingsCategory memorySettings, DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> filterAttributesComponent) {
		super(upgrade, saveHandler, filterSlotCount, getInventoryHandler, memorySettings, filterAttributesComponent);
	}

	public void setInventory(ResourceHandler<ItemResource> inventory) {
		inventoryFilterStacks = inventoryCache.getUnchecked(inventory);
	}

	@Override
	protected boolean matchesFilter(Stream<TagKey<Item>> tags, Item item, int damageValue, boolean empty, DataComponentMap components) {
		if (!shouldFilterByStorage()) {
			return super.matchesFilter(tags, item, damageValue, empty, components);
		}

		for (ItemStackKey filterStack : inventoryFilterStacks) {
			if (stackMatchesFilter(filterStack.stack(), item, damageValue, empty, components)) {
				return true;
			}
		}
		return false;
	}
}
