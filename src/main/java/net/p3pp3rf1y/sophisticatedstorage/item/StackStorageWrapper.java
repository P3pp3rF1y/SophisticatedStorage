package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.*;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class StackStorageWrapper extends StorageWrapper {
	private static final String CONTENTS_TAG = "contents";
	private ItemStack storageStack;

	public StackStorageWrapper(ItemStack storageStack) {
		super(() -> () -> {
		}, () -> {
		}, () -> {
		});
		setStorageStack(storageStack);
	}

	public static StackStorageWrapper fromStack(HolderLookup.Provider registries, ItemStack stack) {
		StackStorageWrapper stackStorageWrapper = StorageWrapperRepository.getStorageWrapper(stack, StackStorageWrapper.class, StackStorageWrapper::new);
		UUID uuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		if (uuid != null) {
			ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
			CompoundTag storageWrappertag = itemContentsStorage.getOrCreateAddtionalBeData(uuid).getCompoundOrEmpty(StorageBlockEntity.STORAGE_WRAPPER);
			ContainerContents contents = itemContentsStorage.getOrCreateContents(uuid);
			Tag contentsTag = ContainerContents.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), contents).getOrThrow();
			storageWrappertag.put(StorageWrapper.CONTENTS, contentsTag);
			stackStorageWrapper.deserialize(ValueIOHelper.inputFromCompoundTag(registries, storageWrappertag));
			stackStorageWrapper.setContentsUuid(uuid); //setting here because client side the uuid isn't in contentsnbt before this data is synced from server and it would create a new one otherwise
		}

		return stackStorageWrapper;
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
		return contentsUuid != null;
	}

	@Override
	public void setContentsUuid(@Nullable UUID contentsUuid) {
		super.setContentsUuid(contentsUuid);
		if (contentsUuid != null) {
			storageStack.set(ModCoreDataComponents.STORAGE_UUID, contentsUuid);
			onContentsUpdated();
		}
	}

	@Override
	public ContainerContents getContents() {
		if (contentsUuid == null) {
			contentsUuid = getNewUuid();
			setContentsUuid(contentsUuid);
		}
		return ItemContentsStorage.get().getOrCreateContents(contentsUuid);
	}

	@Override
	protected void onUpgradeRefresh() {
		//noop - there should be no upgrade refresh happening here
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfInventorySlots() : 0;
	}

	@Override
	protected void loadSlotNumbers(ValueInput in) {
		numberOfInventorySlots = storageStack.getOrDefault(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, 0);
		numberOfUpgradeSlots = storageStack.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, 0);
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfUpgradeSlots() : 0;
	}

	protected void setStorageStack(ItemStack storageStack) {
		this.storageStack = storageStack;
	}

	@Override
	protected boolean isAllowedInStorage(ItemResource resource) {
		if (!(storageStack.getItem() instanceof ShulkerBoxItem)) {
			return false;
		}

		Block block = Block.byItem(resource.getItem());
		return !(block instanceof ShulkerBoxBlock) && !(block instanceof net.minecraft.world.level.block.ShulkerBoxBlock) && !Config.SERVER.shulkerBoxDisallowedItems.isItemDisallowed(resource.getItem());
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
