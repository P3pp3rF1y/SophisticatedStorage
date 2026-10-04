package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ISyncedContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SophisticatedMenuProvider;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSettingsPayload;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Objects;
import java.util.Optional;

public class StorageContainerMenu extends StorageContainerMenuBase<IStorageWrapper> implements ISyncedContainer {
	private static final String SOURCE_CONTAINER_ID_TAG = "sourceContainerId";
	private final StorageBlockEntity storageBlockEntity;
	private boolean stopOpenersOnRemove = true;

	public StorageContainerMenu(int containerId, Player player, BlockPos pos) {
		this(containerId, player, pos, false);
	}

	public StorageContainerMenu(int containerId, Player player, BlockPos pos, boolean openersAlreadyActive) {
		this(ModBlocks.STORAGE_CONTAINER_TYPE.get(), containerId, player, pos, openersAlreadyActive);
	}

	public StorageContainerMenu(MenuType<?> menuType, int containerId, Player player, BlockPos pos) {
		this(menuType, containerId, player, pos, false);
	}

	protected StorageContainerMenu(MenuType<?> menuType, int containerId, Player player, BlockPos pos, boolean openersAlreadyActive) {
		super(menuType, containerId, player, getWrapper(player.level(), pos), NoopStorageWrapper.INSTANCE, -1, false);
		storageBlockEntity = WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class)
				.orElseThrow(() -> new IllegalArgumentException("Incorrect block entity at " + pos + " exptected to find StorageBlockEntity"));
		if (!player.level().isClientSide() && !openersAlreadyActive) {
			storageBlockEntity.startOpen(player);
		}
	}

	public StorageBlockEntity getStorageBlockEntity() {
		return storageBlockEntity;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (!player.level().isClientSide() && stopOpenersOnRemove) {
			storageBlockEntity.stopOpen(player);
		}
	}

	public void transferOpenersToSettingsMenu() {
		stopOpenersOnRemove = false;
	}

	private static IStorageWrapper getWrapper(Level level, BlockPos pos) {
		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(be -> (IStorageWrapper) be.getMenuStorageWrapper())
				.orElse(NoopStorageWrapper.INSTANCE);
	}

	public static StorageContainerMenu fromBuffer(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
		return new StorageContainerMenu(windowId, playerInventory.player, readMenuData(buffer, playerInventory.player));
	}

	public static void writeMenuData(FriendlyByteBuf buffer, Player player, BlockPos pos) {
		buffer.writeBlockPos(pos);
		WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class)
				.ifPresentOrElse(storageBlockEntity -> storageBlockEntity.writeLinkedStorageMenuData(buffer, player), () -> buffer.writeBoolean(false));
	}

	public static BlockPos readMenuData(FriendlyByteBuf buffer, Player player) {
		BlockPos pos = buffer.readBlockPos();
		if (!buffer.readBoolean()) {
			return pos;
		}

		LinkedStorageEndpointData endpoint = new LinkedStorageEndpointData(buffer.readUUID(), buffer.readUUID());
		LinkedStorageEndpointRole endpointRole = buffer.readBoolean() ? LinkedStorageEndpointRole.PRIMARY : LinkedStorageEndpointRole.SECONDARY;
		long revision = buffer.readVarLong();
		Component groupName = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer);
		ContainerContents contents = ContainerContents.CODEC
				.parse(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), Objects.requireNonNull(buffer.readNbt())).getOrThrow();
		int inventorySlots = buffer.readVarInt();
		int upgradeSlots = buffer.readVarInt();
		int columnsTaken = buffer.readVarInt();
		CompoundTag virtualCarrier = Objects.requireNonNull(buffer.readNbt());
		ClientLinkedStorageContents.updateContents(endpoint.groupId(), revision, contents, groupName, inventorySlots, upgradeSlots, columnsTaken);
		ClientLinkedStorageContents.getContents(endpoint.groupId()).ifPresent(
				linkedContents -> WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class).ifPresent(storageBlockEntity -> storageBlockEntity
						.bindClientLinkedStorage(endpoint, endpointRole, linkedContents, virtualCarrier, groupName, inventorySlots, upgradeSlots)));
		ClientLinkedStorageContents.removeUpdatedGroup(endpoint.groupId());
		return pos;
	}

	@Override
	public Optional<BlockPos> getBlockPosition() {
		return Optional.of(storageBlockEntity.getBlockPos());
	}

	@Override
	public Optional<Entity> getEntity() {
		return Optional.empty();
	}

	@Override
	protected void onUpgradeChanged() {
		if (player.level().isClientSide()) {
			return;
		}
		storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
		if (player instanceof ServerPlayer serverPlayer) {
			storageBlockEntity.syncLinkedStorageContentsToPlayer(serverPlayer);
		}
	}

	@Override
	protected void sendStorageSettingsToClient() {
		if (player instanceof ServerPlayer serverPlayer && storageBlockEntity.getLinkedStorageEndpointData() != null) {
			PacketDistributor.sendToPlayer(serverPlayer, new LinkedStorageSettingsPayload(storageBlockEntity.getLinkedStorageEndpointData().groupId(),
					storageWrapper.getSettingsHandler().getSettingsData().copy()));
		}
	}

	@Override
	public boolean detectSettingsChangeAndReload() {
		if (player.level().isClientSide() && storageBlockEntity.getLinkedStorageEndpointData() != null) {
			var endpoint = storageBlockEntity.getLinkedStorageEndpointData();
			boolean snapshotChanged = ClientLinkedStorageContents.removeUpdatedGroup(endpoint.groupId());
			boolean settingsChanged = ClientLinkedStorageContents.removeUpdatedSettings(endpoint.groupId());
			if (!snapshotChanged && !settingsChanged) {
				return false;
			}
			ClientLinkedStorageContents.getContents(endpoint.groupId()).ifPresent(contents -> {
				storageWrapper.getSettingsHandler().reloadFrom(contents.contents().settings());
				if (snapshotChanged) {
					refreshUpgradeControls();
				}
			});
			return true;
		}
		return false;
	}

	@Override
	public void openSettings() {
		if (isClientSide()) {
			sendToServer(data -> {
				data.putString(ACTION_TAG, "openSettings");
				data.putInt(SOURCE_CONTAINER_ID_TAG, containerId);
			});
			return;
		}
		if (!stillValid(player)) {
			return;
		}
		getBlockPosition().ifPresent(pos -> {
			transferOpenersToSettingsMenu();
			player.openMenu(
					new SophisticatedMenuProvider((w, p, pl) -> instantiateSettingsContainerMenu(w, pl, pos, true),
							Component.translatable(StorageTranslationHelper.INSTANCE.translGui("settings.title")), false),
					buffer -> writeMenuData(buffer, player, storageBlockEntity.getBlockPos()));
		});
	}

	@Override
	public void handlePacket(CompoundTag data) {
		if ("openSettings".equals(data.getStringOr(ACTION_TAG, ""))
				&& (!data.contains(SOURCE_CONTAINER_ID_TAG) || data.getInt(SOURCE_CONTAINER_ID_TAG).orElse(-1) != containerId)) {
			return;
		}
		super.handlePacket(data);
	}

	protected StorageSettingsContainerMenu instantiateSettingsContainerMenu(int windowId, Player player, BlockPos pos) {
		return instantiateSettingsContainerMenu(windowId, player, pos, false);
	}

	protected StorageSettingsContainerMenu instantiateSettingsContainerMenu(int windowId, Player player, BlockPos pos, boolean openersAlreadyActive) {
		return new StorageSettingsContainerMenu(windowId, player, pos, openersAlreadyActive);
	}

	@Override
	protected boolean storageItemHasChanged() {
		return false; // storage blocks never have the issue of needing to close gui when item has moved in inventory
	}

	@Override
	public boolean stillValid(Player player) {
		BlockPos pos = storageBlockEntity.getBlockPos();
		BlockEntity be = player.level().getBlockEntity(pos);
		return be instanceof StorageBlockEntity && player.isWithinBlockInteractionRange(pos, 4.0F)
				&& (!(be instanceof WoodStorageBlockEntity woodStorageBlockEntity) || !woodStorageBlockEntity.isPacked());
	}

	@Override
	protected void onStorageInventorySlotSet(int slotIndex) {
		super.onStorageInventorySlotSet(slotIndex);

		if (getStorageBlockEntity().isLocked() && getStorageBlockEntity().memorizesItemsWhenLocked() && !getSlot(slotIndex).getItem().isEmpty()) {
			MemorySettingsCategory memorySettings = getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			if (!memorySettings.isSlotSelected(slotIndex)) {
				memorySettings.selectSlot(slotIndex);
			}
		}
	}

	public float getSlotFillPercentage(int slot) {
		return storageBlockEntity.getSlotFillPercentage(slot);
	}
}
