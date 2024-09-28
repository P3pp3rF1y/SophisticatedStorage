package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class CompressionUpgradeItem extends UpgradeItemBase<CompressionUpgradeItem.Wrapper> {
    public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
    public static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = List.of(new UpgradeConflictDefinition(CompactingUpgradeItem.class::isInstance, 0, StorageTranslationHelper.INSTANCE.translError("add.compacting_exists")));

    public CompressionUpgradeItem() {
        super(Config.SERVER.maxUpgradesPerStorage);
        InventoryPartRegistry.registerFactory(CompressionInventoryPart.NAME, CompressionInventoryPart::new);
    }

    @Override
    public UpgradeType<Wrapper> getType() {
        return TYPE;
    }

    private UpgradeSlotChangeResult checkCompressionSpace(IStorageWrapper storageWrapper) {
        Optional<InventoryPartitioner.SlotRange> slotRange = storageWrapper.getInventoryHandler().getInventoryPartitioner().getFirstSpace(Config.SERVER.compressionUpgrade.maxNumberOfSlots.get());

        return slotRange.map(range -> canUseForCompression(storageWrapper, range))
                .orElseGet(() -> UpgradeSlotChangeResult.fail(StorageTranslationHelper.INSTANCE.translError("add.compression_no_space"), Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
    }

    @Override
    public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide, @Nullable IUpgradeItem<?> upgradeInSlot) {
        if (isClientSide) {
            return UpgradeSlotChangeResult.success();
        }

        return checkCompressionSpace(storageWrapper);
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return UPGRADE_CONFLICT_DEFINITIONS;
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

        return !errorSlots.isEmpty() ? UpgradeSlotChangeResult.fail(StorageTranslationHelper.INSTANCE.translError("add.compression_incompatible_items"), Set.of(), errorSlots, Set.of()) : UpgradeSlotChangeResult.success();
    }

    @Override
    public ItemStack getCleanedUpgradeStack(ItemStack upgradeStack) {
        upgradeStack.remove(ModDataComponents.FIRST_INVENTORY_SLOT);
        return upgradeStack;
    }

    public static class Wrapper extends UpgradeWrapperBase<Wrapper, CompressionUpgradeItem> {

        @Override
        public void onAdded() {
            upgrade.remove(ModDataComponents.FIRST_INVENTORY_SLOT);
            InventoryPartitioner inventoryPartitioner = storageWrapper.getInventoryHandler().getInventoryPartitioner();
            inventoryPartitioner.getFirstSpace(Config.SERVER.compressionUpgrade.maxNumberOfSlots.get()).ifPresent(slotRange -> {
                setFirstInventorySlot(slotRange.firstSlot());
                inventoryPartitioner.addInventoryPart(slotRange.firstSlot(), slotRange.numberOfSlots(), new CompressionInventoryPart(storageWrapper.getInventoryHandler(), slotRange, () -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class)));
            });
            storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
        }

        private void setFirstInventorySlot(int firstInventorySlot) {
            upgrade.set(ModDataComponents.FIRST_INVENTORY_SLOT, firstInventorySlot);
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
            return upgrade.getOrDefault(ModDataComponents.FIRST_INVENTORY_SLOT, -1);
        }

        protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
            super(storageWrapper, upgrade, upgradeSaveHandler);
        }
    }
}
