package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartRegistry;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartitioner;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class CompressionUpgradeItem extends UpgradeItemBase<CompressionUpgradeItem.Wrapper> {
	public static final UpgradeType<CompressionUpgradeItem.Wrapper> TYPE = new UpgradeType<>(CompressionUpgradeItem.Wrapper::new);
	private static final String FIRST_INVENTORY_SLOT_TAG = "firstInventorySlot";

	public CompressionUpgradeItem() {
		super();
		InventoryPartRegistry.registerFactory(CompressionInventoryPart.NAME, CompressionInventoryPart::new);
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	@Override
	public UpgradeSlotChangeResult canAddUpgradeTo(IStorageWrapper storageWrapper, ItemStack upgradeStack, boolean firstLevelStorage, boolean isClientSide) {
		if (isClientSide) {
			return new UpgradeSlotChangeResult.Success();
		}

		Optional<InventoryPartitioner.SlotRange> slotRange = storageWrapper.getInventoryHandler().getInventoryPartitioner().getFirstSpace(Config.SERVER.compressionUpgrade.maxNumberOfSlots.get());

		return slotRange.map(range -> canUseForCompression(storageWrapper, range))
				.orElseGet(() -> new UpgradeSlotChangeResult.Fail(StorageTranslationHelper.INSTANCE.translError("add.compression_no_space"), Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
	}

	private UpgradeSlotChangeResult canUseForCompression(IStorageWrapper storageWrapper, InventoryPartitioner.SlotRange slotRange) {
		boolean allRemainingSlotsMustBeEmpty = false;
		Item nextItemToMatch = Items.AIR;
		Set<Integer> errorSlots = new LinkedHashSet<>();
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		MemorySettingsCategory memorySettingsCategory = storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
		for (int slot = slotRange.firstSlot() + slotRange.numberOfSlots() - 1; slot >= slotRange.firstSlot(); slot--) {
			Item item;
			ItemStack slotStack = inventoryHandler.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				item = slotStack.getItem();
			} else {
				item = memorySettingsCategory.getSlotFilterStack(slot, false).map(ItemStack::getItem).orElse(Items.AIR);
			}
			if (item != Items.AIR) {
				if (allRemainingSlotsMustBeEmpty) {
					errorSlots.add(slot);
				} else {
					if (nextItemToMatch != Items.AIR && nextItemToMatch != item) {
						errorSlots.add(slot);
						break;
					}

					boolean hasSlotBeforeThisOne = slot - 1 >= slotRange.firstSlot();
					if (hasSlotBeforeThisOne) {
						RecipeHelper.CompactingShape compactingShape = RecipeHelper.getItemCompactingShapes(item).stream().filter(RecipeHelper.CompactingShape::isUncraftable).findFirst().orElse(RecipeHelper.CompactingShape.NONE);
						if (compactingShape == RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE || compactingShape == RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE) {
							nextItemToMatch = RecipeHelper.getCompactingResult(item, compactingShape).getResult().getItem();
						} else {
							allRemainingSlotsMustBeEmpty = true;
						}
					}
				}
			}
		}

		return !errorSlots.isEmpty() ? new UpgradeSlotChangeResult.Fail(StorageTranslationHelper.INSTANCE.translError("add.compression_incompatible_items"), Set.of(), errorSlots, Set.of()) : new UpgradeSlotChangeResult.Success();
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
				inventoryPartitioner.addInventoryPart(slotRange.firstSlot(), slotRange.numberOfSlots(), new CompressionInventoryPart(storageWrapper.getInventoryHandler(), slotRange, () -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class)));
			});
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
