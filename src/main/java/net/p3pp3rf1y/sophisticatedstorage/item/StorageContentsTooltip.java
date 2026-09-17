package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public record StorageContentsTooltip(ItemStack storage, @Nullable LinkedStorageTooltip linkedStorageTooltip) implements TooltipComponent {
	public StorageContentsTooltip(ItemStack storage) {
		this(storage, null);
	}
	public ItemStack getStorageItem() {
		return storage;
	}
}
