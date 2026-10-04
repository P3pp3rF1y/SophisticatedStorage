package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
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
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSettingsPayload;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkedStorageHostWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class StorageContainerMenu extends StorageContainerMenuBase<IStorageWrapper> implements ISyncedContainer {
	private static final String SOURCE_CONTAINER_ID_TAG = "sourceContainerId";
	private final StorageBlockEntity storageBlockEntity;
	private final BlockPos storageBlockPosition;
	private boolean stopOpenersOnRemove = true;
	private ContainerContents.SettingsData lastSettingsData;

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
		storageBlockPosition = pos;
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
		Optional<LinkedStorageSnapshot> snapshot = WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class)
				.filter(StorageBlockEntity::isLinkedStorage).map(storage -> getLinkedStorageSnapshot(player, storage));
		buffer.writeBoolean(snapshot.isPresent());
		snapshot.ifPresent(value -> writeLinkedStorageSnapshot(buffer, player, value));
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
		WorldHelper.getBlockEntity(player.level(), pos, StorageBlockEntity.class)
				.ifPresent(storage -> storage.bindClientLinkedStorage(endpoint, endpointRole, virtualCarrier));
		ClientLinkedStorageContents.removeUpdatedGroup(endpoint.groupId());
		return pos;
	}

	private static LinkedStorageSnapshot getLinkedStorageSnapshot(Player player, StorageBlockEntity storage) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			throw new IllegalStateException("Linked storage menu data must be written on the server");
		}

		LinkedStorageEndpointData endpoint = Objects.requireNonNull(storage.getLinkedStorageEndpointData());
		LinkedStorageGroupManager manager = LinkedStorageGroupsSavedData.get(serverLevel).manager();
		if (!manager.isEndpointMember(endpoint.groupId(), endpoint.endpointId())) {
			throw new IllegalStateException("Linked storage endpoint is not registered in its group");
		}
		StorageLinkedStorageHostWrapper host = manager.resolveVirtualHost(endpoint.groupId()).filter(StorageLinkedStorageHostWrapper.class::isInstance)
				.map(StorageLinkedStorageHostWrapper.class::cast)
				.orElseThrow(() -> new IllegalStateException("Linked storage group " + endpoint.groupId() + " does not have a storage host"));
		ILinkedStorageContents contents = manager.resolveContents(endpoint.groupId())
				.orElseThrow(() -> new IllegalStateException("Failed to resolve linked storage contents for group " + endpoint.groupId()));
		CompoundTag virtualCarrier = host.getVirtualCarrierSnapshot()
				.orElseThrow(() -> new IllegalStateException("Linked storage group " + endpoint.groupId() + " does not have a virtual carrier"));
		LinkedStorageEndpointRole endpointRole = manager.isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())
				? LinkedStorageEndpointRole.PRIMARY
				: LinkedStorageEndpointRole.SECONDARY;
		return new LinkedStorageSnapshot(endpoint, endpointRole, manager.getRevision(endpoint.groupId()), contents.contents().copy(), host.getDisplayName(),
				host.getInventoryHandler().size(), host.getUpgradeHandler().size(), host.getColumnsTaken(), virtualCarrier);
	}

	private static void writeLinkedStorageSnapshot(FriendlyByteBuf buffer, Player player, LinkedStorageSnapshot snapshot) {
		buffer.writeUUID(snapshot.endpoint().groupId());
		buffer.writeUUID(snapshot.endpoint().endpointId());
		buffer.writeBoolean(snapshot.endpointRole() == LinkedStorageEndpointRole.PRIMARY);
		buffer.writeVarLong(snapshot.revision());
		ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, snapshot.groupName());
		FriendlyByteBuf.writeNbt(buffer, (CompoundTag) ContainerContents.CODEC
				.encodeStart(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), snapshot.contents()).getOrThrow());
		buffer.writeVarInt(snapshot.inventorySlots());
		buffer.writeVarInt(snapshot.upgradeSlots());
		buffer.writeVarInt(snapshot.columnsTaken());
		FriendlyByteBuf.writeNbt(buffer, snapshot.virtualCarrier());
	}

	private record LinkedStorageSnapshot(LinkedStorageEndpointData endpoint, LinkedStorageEndpointRole endpointRole, long revision, ContainerContents contents,
			Component groupName, int inventorySlots, int upgradeSlots, int columnsTaken, CompoundTag virtualCarrier) {
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
		sendLinkedStorageSettingsToClient();
	}

	@Override
	protected void sendStorageSettingsToClient() {
		sendLinkedStorageSettingsToClient();
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (!(player instanceof ServerPlayer serverPlayer) || !storageBlockEntity.isLinkedStorage()) {
			return;
		}
		ContainerContents.SettingsData settingsData = storageWrapper.getSettingsHandler().getSettingsData();
		if (lastSettingsData == null || !lastSettingsData.equals(settingsData)) {
			lastSettingsData = settingsData.copy();
			PacketDistributor.sendToPlayer(serverPlayer, new LinkedStorageSettingsPayload(storageBlockEntity.getLinkedStorageEndpointData().groupId(),
					storageWrapper.getSettingsHandler().getSettingsData().copy()));
		}
	}

	private void sendLinkedStorageSettingsToClient() {
		if (player instanceof ServerPlayer serverPlayer && storageBlockEntity.getLinkedStorageEndpointData() != null) {
			PacketDistributor.sendToPlayer(serverPlayer, new LinkedStorageSettingsPayload(storageBlockEntity.getLinkedStorageEndpointData().groupId(),
					storageWrapper.getSettingsHandler().getSettingsData().copy()));
		}
	}

	@Override
	public boolean detectSettingsChangeAndReload() {
		if (!player.level().isClientSide() || storageBlockEntity.getLinkedStorageEndpointData() == null) {
			return false;
		}
		UUID groupId = storageBlockEntity.getLinkedStorageEndpointData().groupId();
		boolean snapshotChanged = ClientLinkedStorageContents.removeUpdatedGroup(groupId);
		boolean settingsChanged = ClientLinkedStorageContents.removeUpdatedSettings(groupId);
		if (!snapshotChanged && !settingsChanged) {
			return false;
		}
		return ClientLinkedStorageContents.getContents(groupId).map(contents -> {
			if (snapshotChanged) {
				storageBlockEntity.updateClientLinkedStorageContents(groupId);
			}
			storageWrapper.getSettingsHandler().reloadFrom(contents.contents().settings());
			if (snapshotChanged) {
				refreshUpgradeControls();
			}
			return true;
		}).orElse(false);
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
		if ("openSettings".equals(data.getString(ACTION_TAG))
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
