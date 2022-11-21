package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LimitedBarrelBlockEntity extends BarrelBlockEntity {
	private static final String SLOT_COUNTS_TAG = "slotCounts";
	private long lastDepositTime = -100;

	private final List<Integer> slotCounts = new ArrayList<>();

	public LimitedBarrelBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get());
		registerClientNotificationOnCountChange();
	}

	private void registerClientNotificationOnCountChange() {
		getStorageWrapper().getInventoryHandler().addListener(slot -> WorldHelper.notifyBlockUpdate(this));
	}

	@Override
	protected void onUpgradeCachesInvalidated() {
		super.onUpgradeCachesInvalidated();
		registerClientNotificationOnCountChange();
	}

	public List<Integer> getSlotCounts() {
		return slotCounts;
	}

	public void depositItem(Player player, InteractionHand hand, ItemStack stackInHand, int slot) {
		//noinspection ConstantConditions
		long gameTime = getLevel().getGameTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;

		StorageWrapper storageWrapper = getStorageWrapper();
		InventoryHandler invHandler = storageWrapper.getInventoryHandler();
		ItemStack stackInSlot = invHandler.getStackInSlot(slot);
		ItemStack result = stackInHand;

		MemorySettingsCategory memorySettings = getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);

		if (doubleClick) {
			Predicate<ItemStack> memoryItemMatches = itemStack -> memorySettings.matchesFilter(slot, itemStack);
			player.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(
					playerInventory -> InventoryHelper.transferIntoSlot(playerInventory, getStorageWrapper().getInventoryHandler(), slot, s -> memoryItemMatches.test(s) || ItemHandlerHelper.canItemStacksStack(stackInSlot, s)));
			return;
		}

		if (stackInSlot.isEmpty() && (!memorySettings.isSlotSelected(slot) || memorySettings.matchesFilter(slot, result)))  {
			result = result.copy();
			invHandler.setStackInSlot(slot, result.split(Math.min(result.getCount(), invHandler.getStackLimit(slot, result))));
			if (isLocked()) {
				memorySettings.selectSlot(slot);
			}
		} else if (ItemHandlerHelper.canItemStacksStack(stackInSlot, result)) {
			result = invHandler.insertItem(slot, result, false);
		}
		if (result.getCount() != stackInHand.getCount()) {
			player.setItemInHand(hand, result);
		}
	}

	boolean tryToTakeItem(Player player, int slot) {
		InventoryHandler inventoryHandler = getStorageWrapper().getInventoryHandler();
		ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
		if (stackInSlot.isEmpty()) {
			return false;
		}

		int countToTake = player.isShiftKeyDown() ? Math.min(stackInSlot.getMaxStackSize(), stackInSlot.getCount()) : 1;
		ItemStack stackTaken = inventoryHandler.extractItem(slot, countToTake, false);

		if (player.getInventory().add(stackTaken)) {
			//noinspection ConstantConditions
			getLevel().playSound(null, getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f, (RandHelper.getRandomMinusOneToOne(getLevel().random) * .7f + 1) * 2);
		} else {
			player.drop(stackTaken, false);
		}
		return true;
	}

	@Override
	void updateOpenBlockState(BlockState state, boolean open) {
		//noop
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag updateTag = super.getUpdateTag();
		List<Integer> sc = new ArrayList<>();
		InventoryHelper.iterate(getStorageWrapper().getInventoryHandler(), (slot, stack) -> sc.add(slot, stack.getCount()));
		updateTag.putIntArray(SLOT_COUNTS_TAG, sc);
		return updateTag;
	}

	@Override
	public void loadSynchronizedData(CompoundTag tag) {
		super.loadSynchronizedData(tag);
		if (!tag.contains(SLOT_COUNTS_TAG)) {
			return;
		}
		int[] countsArray = tag.getIntArray(SLOT_COUNTS_TAG);
		if (slotCounts.size() != countsArray.length) {
			slotCounts.clear();
			for (int i = 0; i < countsArray.length; i++) {
				slotCounts.add(i, countsArray[i]);
			}
		} else {
			for (int i = 0; i < countsArray.length; i++) {
				slotCounts.set(i, countsArray[i]);
			}
		}
	}
}
