package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class BarrelBakedModelCacheKey {
	private final BarrelBakedModelBase model;
	@Nullable
	private final BlockState state;
	@Nullable
	private final Direction side;
	@Nullable
	private final RenderType renderType;
	@Nullable
	private final Item barrelItem;
	@Nullable
	private final String woodName;
	private final boolean hasMainColor;
	private final boolean hasAccentColor;
	private final boolean packed;
	private final boolean showsLock;
	private final boolean showsTier;
	private final boolean flatTopItem;
	private final Map<BarrelMaterial, ResourceLocation> materials;
	private final Map<ResourceLocation, Integer> materialTintColors;
	private final List<RenderInfo.DisplayItem> displayItems;
	private final List<Integer> inaccessibleSlots;
	private final int hashCode;

	BarrelBakedModelCacheKey(BarrelBakedModelBase model, @Nullable BlockState state, @Nullable Direction side, @Nullable RenderType renderType,
			@Nullable Item barrelItem, @Nullable String woodName, boolean hasMainColor, boolean hasAccentColor, boolean packed, boolean showsLock,
			boolean showsTier, boolean flatTopItem, Map<BarrelMaterial, ResourceLocation> materials, Map<ResourceLocation, Integer> materialTintColors,
			@Nullable List<RenderInfo.DisplayItem> displayItems, @Nullable List<Integer> inaccessibleSlots) {
		this.model = model;
		this.state = normalizeState(state);
		this.side = side;
		this.renderType = renderType;
		this.barrelItem = barrelItem;
		this.woodName = woodName;
		this.hasMainColor = hasMainColor;
		this.hasAccentColor = hasAccentColor;
		this.packed = packed;
		this.showsLock = showsLock;
		this.showsTier = showsTier;
		this.flatTopItem = flatTopItem;
		this.materials = materials;
		this.materialTintColors = materialTintColors;
		this.displayItems = displayItems == null ? List.of() : displayItems;
		this.inaccessibleSlots = inaccessibleSlots == null ? List.of() : inaccessibleSlots;
		hashCode = calculateHashCode();
	}

	@Nullable
	static BlockState normalizeState(@Nullable BlockState state) {
		if (state == null) {
			return null;
		}
		if (state.hasProperty(StorageBlockBase.TICKING) && state.getValue(StorageBlockBase.TICKING)) {
			state = state.setValue(StorageBlockBase.TICKING, false);
		}
		if (state.hasProperty(BarrelBlock.OPAQUE) && !state.getValue(BarrelBlock.OPAQUE)) {
			state = state.setValue(BarrelBlock.OPAQUE, true);
		}
		return state;
	}

	private BarrelBakedModelCacheKey(BarrelBakedModelCacheKey key) {
		model = key.model;
		state = key.state;
		side = key.side;
		renderType = key.renderType;
		barrelItem = key.barrelItem;
		woodName = key.woodName;
		hasMainColor = key.hasMainColor;
		hasAccentColor = key.hasAccentColor;
		packed = key.packed;
		showsLock = key.showsLock;
		showsTier = key.showsTier;
		flatTopItem = key.flatTopItem;
		materials = Map.copyOf(key.materials);
		materialTintColors = Map.copyOf(key.materialTintColors);
		displayItems = key.displayItems.stream().map(displayItem -> new RenderInfo.DisplayItem(displayItem.getItem().copy(), displayItem.getRotation(),
				displayItem.getSlotIndex(), displayItem.getDisplaySide(), displayItem.getZOffset())).toList();
		inaccessibleSlots = List.copyOf(key.inaccessibleSlots);
		hashCode = calculateHashCode();
	}

	BarrelBakedModelCacheKey immutableCopy() {
		return new BarrelBakedModelCacheKey(this);
	}

	private int calculateHashCode() {
		int hash = System.identityHashCode(model);
		hash = 31 * hash + Objects.hashCode(state);
		hash = 31 * hash + Objects.hashCode(side);
		hash = 31 * hash + Objects.hashCode(renderType);
		hash = 31 * hash + System.identityHashCode(barrelItem);
		hash = 31 * hash + Objects.hashCode(woodName);
		hash = 31 * hash + Boolean.hashCode(hasMainColor);
		hash = 31 * hash + Boolean.hashCode(hasAccentColor);
		hash = 31 * hash + Boolean.hashCode(packed);
		hash = 31 * hash + Boolean.hashCode(showsLock);
		hash = 31 * hash + Boolean.hashCode(showsTier);
		hash = 31 * hash + Boolean.hashCode(flatTopItem);
		hash = 31 * hash + materials.hashCode();
		hash = 31 * hash + materialTintColors.hashCode();
		hash = 31 * hash + getDisplayItemsHashCode();
		return 31 * hash + inaccessibleSlots.hashCode();
	}

	private int getDisplayItemsHashCode() {
		int hash = 1;
		for (RenderInfo.DisplayItem displayItem : displayItems) {
			int displayItemHash = ItemStack.hashItemAndComponents(displayItem.getItem());
			displayItemHash = 31 * displayItemHash + displayItem.getRotation();
			displayItemHash = 31 * displayItemHash + displayItem.getSlotIndex();
			displayItemHash = 31 * displayItemHash + Objects.hashCode(displayItem.getDisplaySide());
			displayItemHash = 31 * displayItemHash + displayItem.getZOffset();
			hash = 31 * hash + displayItemHash;
		}
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BarrelBakedModelCacheKey that)) {
			return false;
		}
		return model == that.model && hasMainColor == that.hasMainColor && hasAccentColor == that.hasAccentColor && packed == that.packed
				&& showsLock == that.showsLock && showsTier == that.showsTier && flatTopItem == that.flatTopItem && Objects.equals(state, that.state)
				&& side == that.side && Objects.equals(renderType, that.renderType) && barrelItem == that.barrelItem && Objects.equals(woodName, that.woodName)
				&& materials.equals(that.materials) && materialTintColors.equals(that.materialTintColors) && displayItemsEqual(that.displayItems)
				&& inaccessibleSlots.equals(that.inaccessibleSlots);
	}

	private boolean displayItemsEqual(List<RenderInfo.DisplayItem> otherDisplayItems) {
		if (displayItems.size() != otherDisplayItems.size()) {
			return false;
		}
		for (int i = 0; i < displayItems.size(); i++) {
			RenderInfo.DisplayItem displayItem = displayItems.get(i);
			RenderInfo.DisplayItem otherDisplayItem = otherDisplayItems.get(i);
			if (displayItem.getRotation() != otherDisplayItem.getRotation() || displayItem.getSlotIndex() != otherDisplayItem.getSlotIndex()
					|| displayItem.getDisplaySide() != otherDisplayItem.getDisplaySide() || displayItem.getZOffset() != otherDisplayItem.getZOffset()
					|| !ItemStack.isSameItemSameComponents(displayItem.getItem(), otherDisplayItem.getItem())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
