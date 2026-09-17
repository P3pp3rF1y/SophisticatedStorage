package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSnapshotProfile;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.IJukeboxPlaybackLocationProvider;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxPlaybackLocation;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import java.util.Optional;
import java.util.UUID;

public class StorageLinkedStorageHostWrapper extends StorageWrapperBase implements ILinkedStorageVirtualHost, IJukeboxPlaybackLocationProvider {
	public static final Identifier FACTORY_ID = SophisticatedStorage.getIdentifier("storage");
	private static final String STANDARD_COMPATIBILITY_KEY = "standard";
	private static final String STORAGE_TYPE_TAG = "storageType";
	private static final String COMPATIBILITY_KEY_TAG = "compatibilityKey";
	private static final String DISPLAY_NAME_TAG = "displayName";
	private static final String INVENTORY_SLOTS_TAG = "inventorySlots";
	private static final String UPGRADE_SLOTS_TAG = "upgradeSlots";
	private static final String BASE_STACK_SIZE_MULTIPLIER_TAG = "baseStackSizeMultiplier";
	private static final String RENDER_INFO_TAG = "renderInfo";

	private final ILinkedStorageContents contents;
	private CompoundTag virtualCarrier;

	public static StorageLinkedStorageHostWrapper create(ILinkedStorageContents contents, CompoundTag virtualCarrier) {
		return new StorageLinkedStorageHostWrapper(contents, virtualCarrier);
	}

	public static void applyClientSnapshotProfile(CompoundTag virtualCarrier, Component groupName, int inventorySlots, int upgradeSlots) {
		virtualCarrier.putString(DISPLAY_NAME_TAG, groupName.getString());
		virtualCarrier.putInt(INVENTORY_SLOTS_TAG, inventorySlots);
		virtualCarrier.putInt(UPGRADE_SLOTS_TAG, upgradeSlots);
	}

	private StorageLinkedStorageHostWrapper(ILinkedStorageContents contents, CompoundTag virtualCarrier) {
		super(() -> contents::markChanged, contents::markRenderDirty, contents::markChanged, getNumberOfDisplayItems(virtualCarrier),
				tracksCountsAndFillRatios(virtualCarrier));
		this.contents = contents;
		this.virtualCarrier = virtualCarrier.copy();
		replaceContents(contents.contents());
		synchronizeLinkedRenderData(virtualCarrier.getCompoundOrEmpty(RENDER_INFO_TAG));
		refreshRenderData();
	}

	@Override
	protected void onUpgradeRefresh() {
		// A virtual host has no physical block state to update.
	}

	@Override
	protected void onRenderDataSerialized(RenderData renderData) {
		if (virtualCarrier != null) {
			virtualCarrier.put(RENDER_INFO_TAG, (CompoundTag) RenderData.CODEC.encodeStart(NbtOps.INSTANCE, renderData).getOrThrow());
		}
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.of(contents.groupId());
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		return virtualCarrier.getIntOr(INVENTORY_SLOTS_TAG, 0);
	}

	@Override
	protected boolean isAllowedInStorage(ItemResource resource) {
		return !getStorageType().equals(ShulkerBoxBlockEntity.STORAGE_TYPE) || ShulkerBoxBlockEntity.isItemAllowed(resource);
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return virtualCarrier.getIntOr(UPGRADE_SLOTS_TAG, 0);
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return virtualCarrier.getIntOr(BASE_STACK_SIZE_MULTIPLIER_TAG, 0);
	}

	@Override
	public String getStorageType() {
		return virtualCarrier.getStringOr(STORAGE_TYPE_TAG, "");
	}

	@Override
	public Component getDisplayName() {
		return Component.literal(virtualCarrier.getStringOr(DISPLAY_NAME_TAG, ""));
	}

	@Override
	public void onLinkedStorageContentsChanged() {
		replaceContents(contents.contents());
		refreshRenderData();
	}

	@Override
	public void onLinkedStorageLayoutChanged() {
		onLinkedStorageContentsChanged();
	}

	@Override
	public void onVirtualCarrierChanged(CompoundTag virtualCarrier) {
		this.virtualCarrier = virtualCarrier.copy();
		synchronizeLinkedRenderData(virtualCarrier.getCompoundOrEmpty(RENDER_INFO_TAG));
		onContentsUpdated();
	}

	@Override
	public Optional<CompoundTag> getVirtualCarrierSnapshot() {
		return Optional.of(virtualCarrier.copy());
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
		StorageWrapper storageWrapper = storageBlockEntity.getStorageWrapper();
		CompoundTag virtualCarrier = new CompoundTag();
		virtualCarrier.putString(STORAGE_TYPE_TAG, storageWrapper.getStorageType());
		virtualCarrier.putString(COMPATIBILITY_KEY_TAG, getCompatibilityKey(storageBlockEntity));
		virtualCarrier.putString(DISPLAY_NAME_TAG, storageBlockEntity.getDisplayName().getString());
		virtualCarrier.putInt(INVENTORY_SLOTS_TAG, storageWrapper.getInventoryHandler().size());
		virtualCarrier.putInt(UPGRADE_SLOTS_TAG, storageWrapper.getUpgradeHandler().size());
		virtualCarrier.putInt(BASE_STACK_SIZE_MULTIPLIER_TAG, storageWrapper.getBaseStackSizeMultiplier());
		virtualCarrier.put(RENDER_INFO_TAG, RenderData.CODEC.encodeStart(NbtOps.INSTANCE, storageWrapper.getRenderDataHandler().getData()).getOrThrow());
		return virtualCarrier;
	}

	public static String getCompatibilityKey(StorageBlockEntity storageBlockEntity) {
		if (storageBlockEntity instanceof LimitedBarrelBlockEntity
				&& storageBlockEntity.getBlockState().getBlock() instanceof LimitedBarrelBlock limitedBarrelBlock) {
			return "limited:" + limitedBarrelBlock.getNumberOfInventorySlots();
		}
		return STANDARD_COMPATIBILITY_KEY;
	}

	public static String getCompatibilityKey(CompoundTag virtualCarrier) {
		String compatibilityKey = virtualCarrier.getStringOr(COMPATIBILITY_KEY_TAG, "");
		return compatibilityKey.isEmpty() ? STANDARD_COMPATIBILITY_KEY : compatibilityKey;
	}

	private static int getNumberOfDisplayItems(CompoundTag virtualCarrier) {
		String storageType = virtualCarrier.getString(STORAGE_TYPE_TAG).orElse("");
		return storageType.equals(BarrelBlockEntity.STORAGE_TYPE) || storageType.equals(LimitedBarrelBlockEntity.STORAGE_TYPE) ? 4 : 1;
	}

	private static boolean tracksCountsAndFillRatios(CompoundTag virtualCarrier) {
		return virtualCarrier.getString(STORAGE_TYPE_TAG).orElse("").equals(LimitedBarrelBlockEntity.STORAGE_TYPE);
	}

	public static CompoundTag withDisplayName(CompoundTag virtualCarrier, Component displayName) {
		CompoundTag updatedCarrier = virtualCarrier.copy();
		updatedCarrier.putString(DISPLAY_NAME_TAG, displayName.getString());
		return updatedCarrier;
	}

	private void refreshRenderData() {
		getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
	}

	private void synchronizeLinkedRenderData(CompoundTag renderInfo) {
		getRenderDataHandler().reloadFrom(RenderData.CODEC.parse(NbtOps.INSTANCE, renderInfo).result().orElse(RenderData.EMPTY));
	}
}
