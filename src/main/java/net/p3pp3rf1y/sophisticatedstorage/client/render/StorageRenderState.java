package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;

import java.util.List;
import java.util.Set;

public class StorageRenderState extends BlockEntityRenderState {
	public List<ItemStackRenderState> upgradeItems;
	public boolean showsDisabledUpgradeDisplay;
	public boolean showsUpgrades;
	public boolean showsTier;
	public boolean isLocked;
	public boolean showsLock;
	public List<DisplayItemInfo> displayItems;
	public Set<Integer> inaccessibleSlots;
	public int displayItemSlots;

	public record DisplayItemInfo(ItemStackRenderState item, int index, int rotation, boolean isBlockItem, float itemOffset, DisplaySide displaySide) {}
}
