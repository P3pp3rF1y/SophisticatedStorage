package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSnapshotProfile;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.IJukeboxPlaybackLocationProvider;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxPlaybackLocation;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import java.util.Optional;
import java.util.UUID;

public class StorageLinkedStorageHostWrapper extends StorageWrapper implements ILinkedStorageVirtualHost, IJukeboxPlaybackLocationProvider {
	public static final ResourceLocation FACTORY_ID = SophisticatedStorage.getRL("storage");
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
		loadCanonicalData(contents.getContents());
		synchronizeLinkedRenderInfo(virtualCarrier.getCompound(RENDER_INFO_TAG));
		getRenderInfo().setRenderUpdateChangeListener(renderInfo -> contents.markRenderDirty());
		refreshRenderInfo();
	}

	@Override
	protected void onUpgradeRefresh() {
		// A virtual host has no physical block state to update.
	}

	@Override
	protected void onRenderInfoSerialized(CompoundTag renderInfo) {
		if (virtualCarrier != null) {
			virtualCarrier.put(RENDER_INFO_TAG, renderInfo.copy());
		}
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.of(contents.groupId());
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		return virtualCarrier.getInt(INVENTORY_SLOTS_TAG);
	}

	@Override
	protected boolean isAllowedInStorage(ItemStack stack) {
		return !getStorageType().equals(ShulkerBoxBlockEntity.STORAGE_TYPE) || ShulkerBoxBlockEntity.isItemAllowed(stack);
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return virtualCarrier.getInt(UPGRADE_SLOTS_TAG);
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return virtualCarrier.getInt(BASE_STACK_SIZE_MULTIPLIER_TAG);
	}

	@Override
	public String getStorageType() {
		return virtualCarrier.getString(STORAGE_TYPE_TAG);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal(virtualCarrier.getString(DISPLAY_NAME_TAG));
	}

	@Override
	public void onLinkedStorageContentsChanged() {
		loadCanonicalData(contents.getContents());
		refreshRenderInfo();
	}

	@Override
	public void onLinkedStorageLayoutChanged() {
		loadCanonicalData(contents.getContents());
		refreshRenderInfo();
	}

	@Override
	public void onVirtualCarrierChanged(CompoundTag virtualCarrier) {
		this.virtualCarrier = virtualCarrier.copy();
		synchronizeLinkedRenderInfo(virtualCarrier.getCompound(RENDER_INFO_TAG));
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
		return Optional
				.of(new LinkedStorageSnapshotProfile(getDisplayName(), getInventoryHandler().getSlots(), getUpgradeHandler().getSlots(), getColumnsTaken()));
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
		virtualCarrier.putInt(INVENTORY_SLOTS_TAG, storageWrapper.getInventoryHandler().getSlots());
		virtualCarrier.putInt(UPGRADE_SLOTS_TAG, storageWrapper.getUpgradeHandler().getSlots());
		virtualCarrier.putInt(BASE_STACK_SIZE_MULTIPLIER_TAG, storageWrapper.getBaseStackSizeMultiplier());
		virtualCarrier.put(RENDER_INFO_TAG, storageWrapper.getRenderInfoNbt());
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
		String compatibilityKey = virtualCarrier.getString(COMPATIBILITY_KEY_TAG);
		return compatibilityKey.isEmpty() ? STANDARD_COMPATIBILITY_KEY : compatibilityKey;
	}

	private static int getNumberOfDisplayItems(CompoundTag virtualCarrier) {
		String storageType = virtualCarrier.getString(STORAGE_TYPE_TAG);
		return storageType.equals(BarrelBlockEntity.STORAGE_TYPE) || storageType.equals(LimitedBarrelBlockEntity.STORAGE_TYPE) ? 4 : 1;
	}

	private static boolean tracksCountsAndFillRatios(CompoundTag virtualCarrier) {
		return virtualCarrier.getString(STORAGE_TYPE_TAG).equals(LimitedBarrelBlockEntity.STORAGE_TYPE);
	}

	public static CompoundTag withDisplayName(CompoundTag virtualCarrier, Component displayName) {
		CompoundTag updatedCarrier = virtualCarrier.copy();
		updatedCarrier.putString(DISPLAY_NAME_TAG, displayName.getString());
		return updatedCarrier;
	}

	private void refreshRenderInfo() {
		getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
	}
}
