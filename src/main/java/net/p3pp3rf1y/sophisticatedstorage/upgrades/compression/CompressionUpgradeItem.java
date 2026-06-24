package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartRegistry;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartitioner;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class CompressionUpgradeItem extends UpgradeItemBase<CompressionUpgradeItem.Wrapper> {
	public static final UpgradeType<CompressionUpgradeItem.Wrapper> TYPE = new UpgradeType<>(CompressionUpgradeItem.Wrapper::new);
	private static final String FIRST_INVENTORY_SLOT_TAG = "firstInventorySlot";
	public static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = List.of(
			new UpgradeConflictDefinition(CompactingUpgradeItem.class::isInstance, 0, StorageTranslationHelper.INSTANCE.translError("add.compacting_exists")));

	public CompressionUpgradeItem() {
		super(Config.SERVER.maxUpgradesPerStorage);
		InventoryPartRegistry.registerFactory(CompressionInventoryPart.NAME, CompressionInventoryPart::new);
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	private UpgradeSlotChangeResult checkCompressionSpace(IStorageWrapper storageWrapper) {
		Optional<SlotRange> slotRange = storageWrapper.getInventoryHandler().getInventoryPartitioner()
				.getFirstSpace(Config.SERVER.compressionUpgrade.maxNumberOfSlots.get());

		return slotRange.map(range -> canUseForCompression(storageWrapper, range))
				.orElseGet(() -> new UpgradeSlotChangeResult.Fail(StorageTranslationHelper.INSTANCE.translError("add.compression_no_space"),
						Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
	}

	@Override
	public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide,
			@Nullable IUpgradeItem<?> upgradeInSlot) {
		if (isClientSide) {
			return new UpgradeSlotChangeResult.Success();
		}

		return checkCompressionSpace(storageWrapper);
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}

	UpgradeSlotChangeResult canUseForCompression(IStorageWrapper storageWrapper, SlotRange slotRange) {
		Map<Integer, ItemStack> stacks = new LinkedHashMap<>();
		Set<Integer> errorSlots = new LinkedHashSet<>();
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		MemorySettingsCategory memorySettingsCategory = storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			int finalSlot = slot;
			ItemStack slotStack = inventoryHandler.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				stacks.put(slot, slotStack);
			} else {
				memorySettingsCategory.getSlotFilterStack(slot, false).ifPresent(stack -> stacks.put(finalSlot, stack));
			}
		}
		errorSlots.addAll(CompressionChainHelper.getCompressionChainErrorSlots(slotRange, stacks));

		return !errorSlots.isEmpty()
				? new UpgradeSlotChangeResult.Fail(StorageTranslationHelper.INSTANCE.translError("add.compression_incompatible_items"), Set.of(), errorSlots,
						Set.of())
				: new UpgradeSlotChangeResult.Success();
	}

	@Override
	public ItemStack getCleanedUpgradeStack(ItemStack upgradeStack) {
		upgradeStack.removeTagKey(FIRST_INVENTORY_SLOT_TAG);
		return upgradeStack;
	}

	public static class Wrapper extends UpgradeWrapperBase<Wrapper, CompressionUpgradeItem> {

		@Override
		public void onAdded() {
			upgrade.removeTagKey(FIRST_INVENTORY_SLOT_TAG);
			InventoryPartitioner inventoryPartitioner = storageWrapper.getInventoryHandler().getInventoryPartitioner();
			inventoryPartitioner.getFirstSpace(Config.SERVER.compressionUpgrade.maxNumberOfSlots.get()).ifPresent(slotRange -> {
				setFirstInventorySlot(slotRange.firstSlot());
				inventoryPartitioner.addInventoryPart(slotRange.firstSlot(), slotRange.numberOfSlots(),
						new CompressionInventoryPart(storageWrapper.getInventoryHandler(), slotRange,
								() -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class)));
			});
			storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
		}

		private void setFirstInventorySlot(int firstInventorySlot) {
			upgrade.getOrCreateTag().putInt(FIRST_INVENTORY_SLOT_TAG, firstInventorySlot);
			save();
		}

		@Override
		public boolean canBeDisabled() {
			return false;
		}

		@Override
		public boolean hideSettingsTab() {
			return true;
		}

		@Override
		public void onBeforeRemoved() {
			super.onBeforeRemoved();
			storageWrapper.getInventoryHandler().getInventoryPartitioner().removeInventoryPart(getFirstInventorySlot());
			save();
		}

		private int getFirstInventorySlot() {
			return NBTHelper.getInt(upgrade, FIRST_INVENTORY_SLOT_TAG).orElse(-1);
		}

		protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler);
		}
	}
}
