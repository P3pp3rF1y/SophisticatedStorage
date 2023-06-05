package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LimitedBarrelBlockEntity extends BarrelBlockEntity implements ICountDisplay {
	private static final String SLOT_COUNTS_TAG = "slotCounts";
	private static final Consumer<VoidUpgradeWrapper> VOID_UPGRADE_VOIDING_OVERFLOW_OF_EVERYTHING_BY_DEFAULT = voidUpgrade -> {
		voidUpgrade.getFilterLogic().setAllowByDefault(false);
		voidUpgrade.setShouldVoidOverflowDefaultOrLoadFromNbt(true);
	};
	private long lastDepositTime = -100;

	private final List<Integer> slotCounts = new ArrayList<>();
	private Map<Integer, DyeColor> slotColors = new HashMap<>();
	private boolean showCounts = true;

	public LimitedBarrelBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get());
		registerUpgradeDefaults();
		registerClientNotificationOnCountChange();
	}

	private void registerUpgradeDefaults() {
		getStorageWrapper().registerUpgradeDefaultsHandler(VoidUpgradeWrapper.class, VOID_UPGRADE_VOIDING_OVERFLOW_OF_EVERYTHING_BY_DEFAULT);
	}

	private void registerClientNotificationOnCountChange() {
		getStorageWrapper().getInventoryHandler().addListener(slot -> WorldHelper.notifyBlockUpdate(this));
	}

	@Override
	protected void onUpgradeCachesInvalidated() {
		super.onUpgradeCachesInvalidated();
		registerClientNotificationOnCountChange();
	}

	@Override
	public boolean shouldShowCounts() {
		return showCounts;
	}

	@Override
	public boolean memorizesItemsWhenLocked() {
		return true;
	}

	@Override
	public boolean allowsEmptySlotsMatchingItemInsertsWhenLocked() {
		return false;
	}

	@Override
	public void toggleCountVisibility() {
		showCounts = !showCounts;
		setChanged();
		WorldHelper.notifyBlockUpdate(this);
	}

	public List<Integer> getSlotCounts() {
		return slotCounts;
	}

	public boolean applyDye(int slot, ItemStack dyeStack, DyeColor dyeColor, boolean applyToAll) {
		if (slot < 0 || slot >= getStorageWrapper().getInventoryHandler().getSlots()) {
			return false;
		}

		StorageWrapper storageWrapper = getStorageWrapper();
		InventoryHandler invHandler = storageWrapper.getInventoryHandler();
		if (applyToAll) {
			boolean success = false;
			for(int i = 0; i < invHandler.getSlots(); i++) {
				success |= applyDye(i, dyeColor, invHandler);
			}
			if (!success) {
				return false;
			}
		} else {
			if (!applyDye(slot, dyeColor, invHandler)) {
				return false;
			}
		}
		setChanged();

		dyeStack.shrink(1);

		WorldHelper.notifyBlockUpdate(this);

		return true;
	}

	private boolean applyDye(int slot, DyeColor dyeColor, InventoryHandler invHandler) {
		ItemStack stackInSlot = invHandler.getStackInSlot(slot);
		if (stackInSlot.isEmpty() || dyeColor.equals(slotColors.get(slot))) {
			return false;
		}

		slotColors.put(slot, dyeColor);
		return true;
	}

	public int getSlotColor(int slot) {
		return slotColors.getOrDefault(slot, DyeColor.WHITE).getTextColor();
	}

	public boolean depositItem(Player player, InteractionHand hand, ItemStack stackInHand, int slot) {
		//noinspection ConstantConditions
		long gameTime = getLevel().getGameTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;

		StorageWrapper storageWrapper = getStorageWrapper();
		InventoryHandler invHandler = storageWrapper.getInventoryHandler();
		ItemStack stackInSlot = invHandler.getStackInSlot(slot);

		MemorySettingsCategory memorySettings = getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);

		if (doubleClick) {
			return depositFromAllOfPlayersInventory(player, slot, invHandler, stackInSlot, memorySettings);
		}

		ItemStack result = invHandler.insertItemOnlyToSlot(slot, stackInHand, true);
		if (result.getCount() != stackInHand.getCount()) {
			result = invHandler.insertItemOnlyToSlot(slot, stackInHand, false);
			if (isLocked()) {
				memorySettings.selectSlot(slot);
			}
			player.setItemInHand(hand, result);
			return true;
		}
		return false;
	}

	private boolean depositFromAllOfPlayersInventory(Player player, int slot, InventoryHandler invHandler, ItemStack stackInSlot, MemorySettingsCategory memorySettings) {
		AtomicBoolean success = new AtomicBoolean(false);
		Predicate<ItemStack> memoryItemMatches = itemStack -> memorySettings.isSlotSelected(slot) && memorySettings.matchesFilter(slot, itemStack);
		player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(
				playerInventory -> InventoryHelper.iterate(playerInventory, (playerSlot, playerStack) -> {
					if ((stackInSlot.isEmpty() && (memoryItemMatches.test(playerStack) || invHandler.isFilterItem(playerStack.getItem())) || (!playerStack.isEmpty() && ItemHandlerHelper.canItemStacksStack(stackInSlot, playerStack)))) {

						ItemStack result = invHandler.insertItemOnlyToSlot(slot, playerStack, true);
						if (result.getCount() < playerStack.getCount()) {
							ItemStack extracted = playerInventory.extractItem(playerSlot, playerStack.getCount() - result.getCount(), true);
							if (!extracted.isEmpty()) {
								invHandler.insertItemOnlyToSlot(slot, playerInventory.extractItem(playerSlot, extracted.getCount(), false), false);
								success.set(true);
							}
						}
					}
				}));
		return success.get();
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
		if (tag.contains(SLOT_COUNTS_TAG)) {
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
		showCounts = NBTHelper.getBoolean(tag, "showCounts").orElse(true);
		slotColors = NBTHelper.getMap(tag, "slotColors", Integer::valueOf, (tagName, t) -> Optional.of(DyeColor.byId(((IntTag) t).getAsInt()))).orElseGet(HashMap::new);
	}

	@Override
	protected void saveSynchronizedData(CompoundTag tag) {
		super.saveSynchronizedData(tag);
		if (!showCounts) {
			tag.putBoolean("showCounts", showCounts);
		}
		if (slotColors.size() > 0) {
			NBTHelper.putMap(tag, "slotColors", slotColors, String::valueOf, color -> IntTag.valueOf(color.getId()));
		}
	}
}
