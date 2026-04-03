package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class ShulkerBoxItem extends StorageBlockItem implements IStashStorageItem {
	public ShulkerBoxItem(Block block, Properties properties) {
		super(block, properties.stacksTo(1));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
		if (tooltipFlag.isAdvanced()) {
			HolderLookup.Provider registries = context.registries();
			if (registries != null) {
				StackStorageWrapper.fromStack(registries, stack).getContentsUuid().ifPresent(uuid -> tooltipAdder.accept(Component.literal("UUID: " + uuid).withStyle(ChatFormatting.DARK_GRAY)));
			}
		}
		if (!Minecraft.getInstance().hasShiftDown()) {
			tooltipAdder.accept(Component.translatable(
					TranslationHelper.INSTANCE.translItemTooltip("storage") + ".press_for_contents",
					Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".shift").withStyle(ChatFormatting.AQUA)
			).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (FMLEnvironment.getDist().isClient()) {
			return Optional.ofNullable(StorageItemClient.getTooltipImage(stack));
		}
		return Optional.empty();
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	@Override
	public void onDestroyed(ItemEntity itemEntity) {
		Level level = itemEntity.level();
		if (level.isClientSide()) {
			return;
		}
		ItemStack stack = itemEntity.getItem();
		StackStorageWrapper storageWrapper = StackStorageWrapper.fromStack(level.registryAccess(), stack);
		InventoryHelper.dropItems(storageWrapper.getInventoryHandler(), level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
		InventoryHelper.dropItems(storageWrapper.getUpgradeHandler(), level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
	}

	@Override
	public Optional<TooltipComponent> getInventoryTooltip(ItemStack stack) {
		return Optional.of(new StorageContentsTooltip(stack));
	}

	public int stash(HolderLookup.Provider registries, ItemStack storageStack, ItemResource resource, int amount, TransactionContext tx) {
		StackStorageWrapper wrapper = StackStorageWrapper.fromStack(registries, storageStack);
		if (wrapper.getContentsUuid().isEmpty()) {
			wrapper.ensureContentsUuid();
		}
		return wrapper.getInventoryForUpgradeProcessing().insert(resource, amount, tx);
	}

	@Override
	public StashResult getItemStashable(HolderLookup.Provider registries, ItemStack storageStack, ItemStack stack) {
		StackStorageWrapper wrapper = StackStorageWrapper.fromStack(registries, storageStack);

		try (Transaction tx = Transaction.openRoot()) {
			if (wrapper.getInventoryForUpgradeProcessing().insert(ItemResource.of(stack), stack.getCount(), tx) == 0) {
				return StashResult.NO_SPACE;
			}
		}
		if (wrapper.getInventoryHandler().getSlotTracker().getItems().contains(stack.getItem()) || wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(stack)) {
			return StashResult.MATCH_AND_SPACE;
		}

		return StashResult.SPACE;
	}

	public int getNumberOfInventorySlots(HolderLookup.Provider registries, ItemStack shulkerBoxStack) {
		int defaultNumberOfInventorySlots = StackStorageWrapper.fromStack(registries, shulkerBoxStack).getDefaultNumberOfInventorySlots();
		return Math.max(shulkerBoxStack.getOrDefault(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, defaultNumberOfInventorySlots), defaultNumberOfInventorySlots);
	}

	public int getNumberOfUpgradeSlots(HolderLookup.Provider registries, ItemStack shulkerBoxStack) {
		int defaultNumberOfUpgradeSlots = StackStorageWrapper.fromStack(registries, shulkerBoxStack).getDefaultNumberOfUpgradeSlots();
		return Math.max(shulkerBoxStack.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, defaultNumberOfUpgradeSlots), defaultNumberOfUpgradeSlots);
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack storageStack, Slot slot, ClickAction action, Player player) {
		if (hasCreativeScreenContainerOpen(player) || storageStack.getCount() > 1 || !slot.mayPickup(player) || slot.getItem().isEmpty() || action != ClickAction.SECONDARY) {
			return super.overrideStackedOnOther(storageStack, slot, action, player);
		}

		ItemStack stackToStash = slot.getItem();
		try (Transaction tx = Transaction.openRoot()) {
			int stashed = stash(player.level().registryAccess(), storageStack, ItemResource.of(stackToStash), stackToStash.getCount(), tx);
			if (stashed > 0) {
				tx.commit();
				slot.safeTake(stashed, stashed, player);
				return true;
			}
		}
		return super.overrideStackedOnOther(storageStack, slot, action, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack storageStack, ItemStack otherStack, Slot slot, ClickAction action, Player player, SlotAccess carriedAccess) {
		if (hasCreativeScreenContainerOpen(player) || storageStack.getCount() > 1 || !slot.mayPlace(storageStack) || action != ClickAction.SECONDARY) {
			return super.overrideOtherStackedOnMe(storageStack, otherStack, slot, action, player, carriedAccess);
		}

		try (Transaction tx = Transaction.openRoot()) {
			int stashed = stash(player.level().registryAccess(), storageStack, ItemResource.of(otherStack), otherStack.getCount(), tx);
			if (stashed > 0) {
				tx.commit();
				carriedAccess.set(otherStack.copyWithCount(otherStack.getCount() - stashed));
				slot.set(storageStack);
				return true;
			}
		}

		return super.overrideOtherStackedOnMe(storageStack, otherStack, slot, action, player, carriedAccess);
	}

	private boolean hasCreativeScreenContainerOpen(Player player) {
		return player.level().isClientSide() && player.containerMenu instanceof CreativeModeInventoryScreen.ItemPickerMenu;
	}
}
