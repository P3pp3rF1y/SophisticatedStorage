package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageEndpointProvider;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSnapshotProfile;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.IJukeboxPlaybackLocationProvider;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxPlaybackLocation;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class StorageLinkedStorageHostWrapper extends StorageWrapper
		implements
			ILinkedStorageVirtualHost,
			ILinkedStorageEndpointProvider,
			IJukeboxPlaybackLocationProvider {
	public static final Identifier FACTORY_ID = SophisticatedStorage.getIdentifier("storage");
	static final String STANDARD_COMPATIBILITY_KEY = "standard";

	private final ILinkedStorageContents contents;
	private StorageLinkedStorageVirtualCarrier virtualCarrier;
	@Nullable
	private final LinkedStorageEndpointData linkedStorageEndpoint;
	@Nullable
	private final LinkedStorageEndpointRole linkedStorageEndpointRole;

	public static StorageLinkedStorageHostWrapper create(ILinkedStorageContents contents, CompoundTag virtualCarrier) {
		return new StorageLinkedStorageHostWrapper(contents, StorageLinkedStorageVirtualCarrier.fromTag(virtualCarrier), null, null);
	}

	public static StorageLinkedStorageHostWrapper create(ILinkedStorageContents contents, CompoundTag virtualCarrier,
			LinkedStorageEndpointData linkedStorageEndpoint, LinkedStorageEndpointRole linkedStorageEndpointRole) {
		return new StorageLinkedStorageHostWrapper(contents, StorageLinkedStorageVirtualCarrier.fromTag(virtualCarrier), linkedStorageEndpoint,
				linkedStorageEndpointRole);
	}

	public static CompoundTag applyClientSnapshotProfile(CompoundTag virtualCarrier, Component groupName, int inventorySlots, int upgradeSlots) {
		return StorageLinkedStorageVirtualCarrier.fromTag(virtualCarrier).withSnapshotProfile(groupName, inventorySlots, upgradeSlots).toTag();
	}

	private StorageLinkedStorageHostWrapper(ILinkedStorageContents contents, StorageLinkedStorageVirtualCarrier virtualCarrier,
			@Nullable LinkedStorageEndpointData linkedStorageEndpoint, @Nullable LinkedStorageEndpointRole linkedStorageEndpointRole) {
		super(() -> contents::markChanged, contents::markRenderDirty, contents::markChanged, virtualCarrier.numberOfDisplayItems(),
				virtualCarrier.tracksCountsAndFillRatios());
		this.contents = contents;
		this.virtualCarrier = virtualCarrier;
		this.linkedStorageEndpoint = linkedStorageEndpoint;
		this.linkedStorageEndpointRole = linkedStorageEndpointRole;
		loadCanonicalData(contents.contents(), getDefaultNumberOfInventorySlots(), getDefaultNumberOfUpgradeSlots(), contents.getColumnsTaken());
		synchronizeLinkedRenderData(virtualCarrier.renderData());
		refreshRenderData();
	}

	@Override
	protected void onUpgradeRefresh() {
		// A virtual host has no physical block state to update.
	}

	@Override
	protected void onRenderDataSerialized(RenderData renderData) {
		virtualCarrier = virtualCarrier.withRenderData(renderData);
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.of(contents.groupId());
	}

	@Override
	public Optional<LinkedStorageEndpointData> getLinkedStorageEndpoint() {
		return Optional.ofNullable(linkedStorageEndpoint);
	}

	@Override
	public Optional<LinkedStorageEndpointRole> getLinkedStorageEndpointRole() {
		return Optional.ofNullable(linkedStorageEndpointRole);
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		return virtualCarrier.inventorySlots();
	}

	@Override
	protected boolean isAllowedInStorage(ItemResource resource) {
		if (!getStorageType().equals(ShulkerBoxBlockEntity.STORAGE_TYPE)) {
			return true;
		}
		Block block = Block.byItem(resource.getItem());
		return !(block instanceof ShulkerBoxBlock) && !(block instanceof net.minecraft.world.level.block.ShulkerBoxBlock)
				&& !Config.SERVER.shulkerBoxDisallowedItems.isItemDisallowed(resource.getItem());
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return virtualCarrier.upgradeSlots();
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return virtualCarrier.baseStackSizeMultiplier();
	}

	@Override
	public String getStorageType() {
		return virtualCarrier.storageType();
	}

	@Override
	public Component getDisplayName() {
		return Component.literal(virtualCarrier.displayName());
	}

	@Override
	public void onLinkedStorageContentsChanged() {
		loadCanonicalData(contents.contents(), getDefaultNumberOfInventorySlots(), getDefaultNumberOfUpgradeSlots(), contents.getColumnsTaken());
		refreshRenderData();
	}

	@Override
	public void onLinkedStorageLayoutChanged() {
		onLinkedStorageContentsChanged();
	}

	@Override
	public void onVirtualCarrierChanged(CompoundTag virtualCarrier) {
		this.virtualCarrier = StorageLinkedStorageVirtualCarrier.fromTag(virtualCarrier);
		synchronizeLinkedRenderData(this.virtualCarrier.renderData());
		loadCanonicalData(contents.contents(), getDefaultNumberOfInventorySlots(), getDefaultNumberOfUpgradeSlots(), contents.getColumnsTaken());
	}

	@Override
	public void setColumnsTaken(int columnsTaken, boolean hasChanged) {
		contents.setColumnsTaken(columnsTaken);
	}

	@Override
	public int getColumnsTaken() {
		return contents.getColumnsTaken();
	}

	@Override
	public Optional<CompoundTag> getVirtualCarrierSnapshot() {
		return Optional.of(virtualCarrier.toTag());
	}

	@Override
	public Optional<Component> getLinkedStorageDisplayName() {
		return Optional.of(getDisplayName());
	}

	@Override
	public Optional<LinkedStorageSnapshotProfile> getLinkedStorageSnapshotProfile() {
		return Optional.of(new LinkedStorageSnapshotProfile(getDisplayName(), getInventoryHandler().size(), getUpgradeHandler().size(), getColumnsTaken()));
	}

	@Override
	public Optional<JukeboxPlaybackLocation> getJukeboxPlaybackLocation(ServerLevel initiatingLevel) {
		return StorageLinkedStorageJukeboxPlaybackAnchors.getPlaybackLocation(initiatingLevel, contents.groupId());
	}

	public static CompoundTag createVirtualCarrier(StorageBlockEntity storageBlockEntity) {
		return StorageLinkedStorageVirtualCarrier.from(storageBlockEntity).toTag();
	}

	public static CompoundTag createVirtualCarrier(ItemStack stack, net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper storageWrapper) {
		return new StorageLinkedStorageVirtualCarrier(storageWrapper.getStorageType(), getCompatibilityKey(stack), stack.getHoverName().getString(),
				storageWrapper.getInventoryHandler().size(), storageWrapper.getUpgradeHandler().size(), storageWrapper.getBaseStackSizeMultiplier(),
				storageWrapper.getRenderDataHandler().getData()).toTag();
	}

	public void persistCanonicalContents() {
		contents.setContents(contents.groupId(), contents.contents().copy());
	}

	public static String getCompatibilityKey(StorageBlockEntity storageBlockEntity) {
		if (storageBlockEntity instanceof LimitedBarrelBlockEntity && storageBlockEntity.getBlockState().getBlock() instanceof LimitedBarrelBlock block) {
			return "limited:" + block.getNumberOfInventorySlots();
		}
		return STANDARD_COMPATIBILITY_KEY;
	}

	public static String getCompatibilityKey(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof LimitedBarrelBlock block) {
			return "limited:" + block.getNumberOfInventorySlots();
		}
		return STANDARD_COMPATIBILITY_KEY;
	}

	public static String getCompatibilityKey(CompoundTag virtualCarrier) {
		return StorageLinkedStorageVirtualCarrier.fromTag(virtualCarrier).compatibilityKey();
	}

	public static CompoundTag withDisplayName(CompoundTag virtualCarrier, Component displayName) {
		return StorageLinkedStorageVirtualCarrier.fromTag(virtualCarrier).withDisplayName(displayName).toTag();
	}

	private void refreshRenderData() {
		getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
	}

	private void synchronizeLinkedRenderData(RenderData renderData) {
		getRenderDataHandler().reloadFrom(renderData);
	}
}
