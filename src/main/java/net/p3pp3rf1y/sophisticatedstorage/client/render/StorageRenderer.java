package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class StorageRenderer<T extends StorageBlockEntity, R extends StorageRenderState> implements BlockEntityRenderer<T, R> {
	private final ItemModelResolver itemModelResolver;
	private long lastCacheTime = -1;
	private boolean holdsItemThatShowsUpgrades = false;
	private boolean holdsStorageToolSetToToggleUpgrades = false;
	private boolean holdsItemThatShowsHiddenTiers = false;

	private boolean holdsItemThatShowsFillLevels = false;

	private boolean holdsToolInToggleLockOrLockDisplay = false;
	private boolean holdsToolInToggleFillLevelDisplay = false;

	public StorageRenderer(BlockEntityRendererProvider.Context context) {
		itemModelResolver = context.itemModelResolver();
	}

	protected boolean holdsItemThatShowsUpgrades() {
		refreshCache();
		return holdsItemThatShowsUpgrades;
	}

	public boolean holdsItemThatShowsFillLevels() {
		return holdsItemThatShowsFillLevels;
	}

	private void refreshCache() {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null && level.getGameTime() != lastCacheTime) {
			lastCacheTime = level.getGameTime();

			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null) {
				holdsItemThatShowsUpgrades = false;
				holdsStorageToolSetToToggleUpgrades = false;
				holdsItemThatShowsHiddenTiers = false;
				holdsToolInToggleLockOrLockDisplay = false;
				return;
			}

			boolean holdsStorageTool = holdsItem(player, this::isStorageTool);
			holdsStorageToolSetToToggleUpgrades = holdsStorageTool && InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get())
					.map(item -> StorageToolItem.getMode(item) == StorageToolItem.Mode.UPGRADES_DISPLAY).orElse(false);

			holdsItemThatShowsUpgrades = holdsStorageTool || holdsItem(player, this::isUpgrade);
			holdsItemThatShowsFillLevels = holdsStorageTool || holdsItem(player, this::isStorageTierUpgrade) || holdsItem(player, stack -> isUpgrade(stack) && stack.getItem() instanceof StackUpgradeItem);
			holdsItemThatShowsHiddenTiers = (holdsStorageTool && InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get())
					.map(item -> StorageToolItem.getMode(item) == StorageToolItem.Mode.TIER_DISPLAY).orElse(false))
					|| holdsItem(player, this::isStorageTierUpgrade);
			holdsToolInToggleLockOrLockDisplay = holdsStorageTool && InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get())
					.map(item -> {
						StorageToolItem.Mode mode = StorageToolItem.getMode(item);
						return mode == StorageToolItem.Mode.LOCK_DISPLAY || mode == StorageToolItem.Mode.LOCK;
					}).orElse(false);
			holdsToolInToggleFillLevelDisplay = holdsStorageTool && InventoryHelper.getItemFromEitherHand(player, ModItems.STORAGE_TOOL.get())
					.map(item -> StorageToolItem.getMode(item) == StorageToolItem.Mode.FILL_LEVEL_DISPLAY).orElse(false);
		}
	}

	public boolean holdsItemThatShowsHiddenTiers() {
		refreshCache();
		return holdsItemThatShowsHiddenTiers;
	}

	public boolean holdsToolInToggleLockOrLockDisplay() {
		refreshCache();
		return holdsToolInToggleLockOrLockDisplay;
	}

	public boolean holdsToolInToggleFillLevelDisplay() {
		refreshCache();
		return holdsToolInToggleFillLevelDisplay;
	}

	private boolean holdsItem(LocalPlayer player, Predicate<ItemStack> itemMatcher) {
		return itemMatcher.test(player.getItemInHand(InteractionHand.MAIN_HAND))
				|| itemMatcher.test(player.getItemInHand(InteractionHand.OFF_HAND));
	}

	private boolean isStorageTool(ItemStack stack) {
		return stack.getItem() == ModItems.STORAGE_TOOL.get();
	}

	private boolean isUpgrade(ItemStack stack) {
		return stack.getItem() instanceof UpgradeItemBase && stack.is(ModItems.STORAGE_UPGRADE_TAG);
	}

	private boolean isStorageTierUpgrade(ItemStack stack) {
		return stack.getItem() instanceof StorageTierUpgradeItem;
	}

	public boolean shouldShowDisabledUpgradesDisplay(T storageBlockEntity) {
		refreshCache();
		return holdsStorageToolSetToToggleUpgrades && !storageBlockEntity.shouldShowUpgrades();
	}

	@Override
	public void extractRenderState(T blockEntity, R renderState, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);

		BlockState blockState = blockEntity.getBlockState();
		if (!(blockState.getBlock() instanceof StorageBlockBase storageBlock)) {
			return;
		}
		RenderDataHandler renderDataHandler = getStorageWrapper(blockEntity).getRenderDataHandler();
		renderState.upgradeItems = renderDataHandler.getUpgradeItems().stream().flatMap(upgradeItem -> {
			ItemStackRenderState stackRenderState = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(stackRenderState, upgradeItem, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);

			return Stream.of(stackRenderState);
		}).toList();

		renderState.showsUpgrades = blockEntity.shouldShowUpgrades();
		renderState.showsTier = blockEntity.shouldShowTier();
		renderState.isLocked = blockEntity.isLocked();
		renderState.showsLock = blockEntity.shouldShowLock();
		renderState.showsDisabledUpgradeDisplay = shouldShowDisabledUpgradesDisplay(blockEntity);

		RenderData.DisplayData displayData = renderDataHandler.getDisplayData();
		List<RenderData.DisplayItemData> displayItems = displayData.displayItems();
		renderState.inaccessibleSlots = new HashSet<>(displayData.inaccessibleSlots());
		renderState.displayItemSlots = storageBlock.getDisplayItemsCount(displayItems);

		renderState.displayItems = new ArrayList<>();
		for (int i = 0; i < displayItems.size(); i++) {
			RenderData.DisplayItemData displayItem = displayItems.get(i);
			ItemStack stack = displayItem.createItemStack();
			if (stack.isEmpty()) {
				continue;
			}

			ItemStackRenderState stackRenderState = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(stackRenderState, stack, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);

			float itemOffset = (float) DisplayItemRenderer.getDisplayItemOffset(stack, stackRenderState, DisplayItemRenderer.isGui3d(stackRenderState), renderState.displayItemSlots == 1 ? 1 : DisplayItemRenderer.SMALL_BLOCK_ITEM_OFFSET);

			renderState.displayItems.add(new StorageRenderState.DisplayItemInfo(stackRenderState, storageBlock.hasFixedIndexDisplayItems() ? displayItem.slotIndex() : i, displayItem.rotation(), stack.getItem() instanceof BlockItem, itemOffset, displayItem.displaySide()));
		}
	}

	protected StorageWrapper getStorageWrapper(T blockEntity) {
		return blockEntity.getStorageWrapper();
	}
}
