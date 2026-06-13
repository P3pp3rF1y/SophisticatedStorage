package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.*;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class StackStorageWrapper extends StorageWrapper {
	private static final String CONTENTS_TAG = "contents";
	private ItemStack storageStack;

	public StackStorageWrapper(ItemStack storageStack) {
		super(() -> () -> {}, () -> {}, () -> {});
		setStorageStack(storageStack);
	}

	public static StackStorageWrapper fromStack(HolderLookup.Provider registries, ItemStack stack) {
		StackStorageWrapper stackStorageWrapper = StorageWrapperRepository.getStorageWrapper(stack, StackStorageWrapper.class, StackStorageWrapper::new);
		UUID uuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		if (uuid != null) {
			CompoundTag compoundtag = ItemContentsStorage.get().getOrCreateStorageContents(uuid).getCompoundOrEmpty(StorageBlockEntity.STORAGE_WRAPPER_TAG);
			stackStorageWrapper.load(compoundtag);
			stackStorageWrapper.setContentsUuid(uuid); //setting here because client side the uuid isn't in contentsnbt before this data is synced from server and it would create a new one otherwise
		}

		return stackStorageWrapper;
	}

	public void ensureContentsUuid() {
		if (contentsUuid == null) {
			setContentsUuid(UUID.randomUUID());
		}
	}

	private UUID getNewUuid() {
		UUID newUuid = UUID.randomUUID();
		setContentsUuid(newUuid);
		return newUuid;
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.ofNullable(contentsUuid);
	}

	public boolean hasContents() {
		return StorageBlockItem.getEntityWrapperTagFromStack(storageStack).isPresent() || contentsUuid != null;
	}

	@Override
	public void setContentsUuid(@Nullable UUID contentsUuid) {
		super.setContentsUuid(contentsUuid);
		if (contentsUuid != null) {
			storageStack.set(ModCoreDataComponents.STORAGE_UUID, contentsUuid);
			ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
			CompoundTag storageContents = itemContentsStorage.getOrCreateStorageContents(contentsUuid);
			if (!storageContents.contains(StorageBlockEntity.STORAGE_WRAPPER_TAG)) {
				CompoundTag storageWrapperTag = new CompoundTag();
				storageWrapperTag.put(CONTENTS_TAG, new CompoundTag());
				storageContents.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, storageWrapperTag);
			}

			onContentsNbtUpdated();
		}
	}

	@Override
	protected CompoundTag getContentsNbt() {
		return StorageBlockItem.getEntityWrapperTagFromStack(storageStack).map(wrapperTag -> wrapperTag.getCompoundOrEmpty(CONTENTS_TAG)).orElseGet(() -> {
			if (contentsUuid == null) {
				contentsUuid = getNewUuid();
			}
			return ItemContentsStorage.get().getOrCreateStorageContents(contentsUuid).getCompoundOrEmpty(StorageBlockEntity.STORAGE_WRAPPER_TAG).getCompoundOrEmpty(CONTENTS_TAG);
		});
	}

	@Override
	protected void onUpgradeRefresh() {
		//noop - there should be no upgrade refresh happening here
	}

	@Override
	protected void save() {
		if (contentsUuid != null) {
			ItemContentsStorage.get().setDirty();
		}
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		return StorageBlockItem.getDefaultNumberOfInventorySlots(storageStack);
	}

	@Override
	protected void loadSlotNumbers(CompoundTag tag) {
		StorageBlockItem.getEntityWrapperTagFromStack(storageStack).ifPresentOrElse(wrapperTag -> {
			numberOfInventorySlots = wrapperTag.getIntOr(StorageWrapper.NUMBER_OF_INVENTORY_SLOTS_TAG, 0);
			numberOfUpgradeSlots = wrapperTag.getIntOr(StorageWrapper.NUMBER_OF_UPGRADE_SLOTS_TAG, 0);
			promoteSlotNumbersToDefaults();
			StorageBlockItem.setNumberOfInventorySlots(storageStack, numberOfInventorySlots);
			StorageBlockItem.setNumberOfUpgradeSlots(storageStack, numberOfUpgradeSlots);
		}, () -> {
			numberOfInventorySlots = StorageBlockItem.getNumberOfInventorySlots(storageStack);
			numberOfUpgradeSlots = StorageBlockItem.getNumberOfUpgradeSlots(storageStack);
		});
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return StorageBlockItem.getDefaultNumberOfUpgradeSlots(storageStack);
	}

	protected void setStorageStack(ItemStack storageStack) {
		this.storageStack = storageStack;
	}

	@Override
	protected boolean isAllowedInStorage(ItemStack stack) {
		if (!(storageStack.getItem() instanceof ShulkerBoxItem)) {
			return false;
		}

		Block block = Block.byItem(stack.getItem());
		return !(block instanceof ShulkerBoxBlock) && !(block instanceof net.minecraft.world.level.block.ShulkerBoxBlock) && !Config.SERVER.shulkerBoxDisallowedItems.isItemDisallowed(stack.getItem());
	}

	@Override
	public String getStorageType() {
		return "irrelevant"; //because this is only used when determining upgrade errors in gui which storage stacks can't have open
	}

	@Override
	public Component getDisplayName() {
		return Component.empty(); //because this is only used when determining upgrade errors in gui which storage stacks can't have open
	}

	@Override
	public void setColors(int mainColor, int accentColor) {
		storageStack.set(ModCoreDataComponents.MAIN_COLOR, mainColor);
		storageStack.set(ModCoreDataComponents.ACCENT_COLOR, accentColor);
		save();
	}

	@Override
	public int getMainColor() {
		return storageStack.getOrDefault(ModCoreDataComponents.MAIN_COLOR, -1);
	}


	@Override
	public boolean hasMainColor() {
		return storageStack.has(ModCoreDataComponents.MAIN_COLOR);
	}

	@Override
	public int getAccentColor() {
		return storageStack.getOrDefault(ModCoreDataComponents.ACCENT_COLOR, -1);
	}

	@Override
	public boolean hasAccentColor() {
		return storageStack.has(ModCoreDataComponents.ACCENT_COLOR);
	}

}
