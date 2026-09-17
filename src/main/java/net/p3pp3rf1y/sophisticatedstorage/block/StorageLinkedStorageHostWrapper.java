package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.UUID;

public class StorageLinkedStorageHostWrapper extends StorageWrapper
		implements
			ILinkedStorageVirtualHost,
			ILinkedStorageEndpointProvider,
			IJukeboxPlaybackLocationProvider {
	public static final Identifier FACTORY_ID = SophisticatedStorage.getIdentifier("storage");
	private static final String STORAGE_TYPE = "storageType";
	private static final String COMPATIBILITY_KEY = "compatibilityKey";
	private static final String DISPLAY_NAME = "displayName";
	private static final String INVENTORY_SLOTS = "inventorySlots";
	private static final String UPGRADE_SLOTS = "upgradeSlots";
	private static final String BASE_STACK_SIZE_MULTIPLIER = "baseStackSizeMultiplier";
	private static final String RENDER_INFO = "renderInfo";
	private static final String STANDARD_COMPATIBILITY_KEY = "standard";

	private final ILinkedStorageContents binding;
	private CompoundTag virtualCarrier;
	@Nullable
	private final LinkedStorageEndpointData linkedStorageEndpoint;
	@Nullable
	private final LinkedStorageEndpointRole linkedStorageEndpointRole;

	public static StorageLinkedStorageHostWrapper create(ILinkedStorageContents binding, CompoundTag virtualCarrier) {
		return new StorageLinkedStorageHostWrapper(binding, virtualCarrier, null, null);
	}

	public static StorageLinkedStorageHostWrapper create(ILinkedStorageContents binding, CompoundTag virtualCarrier,
			LinkedStorageEndpointData linkedStorageEndpoint, LinkedStorageEndpointRole linkedStorageEndpointRole) {
		return new StorageLinkedStorageHostWrapper(binding, virtualCarrier, linkedStorageEndpoint, linkedStorageEndpointRole);
	}

	private StorageLinkedStorageHostWrapper(ILinkedStorageContents binding, CompoundTag virtualCarrier,
			@Nullable LinkedStorageEndpointData linkedStorageEndpoint, @Nullable LinkedStorageEndpointRole linkedStorageEndpointRole) {
		super(() -> binding::markDirty, binding::markRenderDirty, binding::markDirty, displayItemCount(virtualCarrier), tracksCounts(virtualCarrier));
		this.binding = binding;
		this.virtualCarrier = virtualCarrier.copy();
		this.linkedStorageEndpoint = linkedStorageEndpoint;
		this.linkedStorageEndpointRole = linkedStorageEndpointRole;
		replaceContents(binding.contents());
		synchronizeLinkedRenderInfo(virtualCarrier);
		getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
	}

	@Override
	protected void onUpgradeRefresh() {
		// A virtual host has no block state to schedule.
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.of(binding.groupId());
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
		return virtualCarrier.getIntOr(INVENTORY_SLOTS, 0);
	}

	@Override
	protected boolean isAllowedInStorage(ItemResource resource) {
		return !getStorageType().equals(ShulkerBoxBlockEntity.STORAGE_TYPE) || ShulkerBoxBlockEntity.isItemAllowed(resource);
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return virtualCarrier.getIntOr(UPGRADE_SLOTS, 0);
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return virtualCarrier.getIntOr(BASE_STACK_SIZE_MULTIPLIER, 1);
	}

	@Override
	public String getStorageType() {
		return virtualCarrier.getStringOr(STORAGE_TYPE, "");
	}

	@Override
	public Component getDisplayName() {
		return Component.literal(virtualCarrier.getStringOr(DISPLAY_NAME, ""));
	}

	@Override
	public void onLinkedStorageContentsChanged() {
		replaceContents(binding.contents());
		getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
	}

	@Override
	public void onLinkedStorageLayoutChanged() {
		onLinkedStorageContentsChanged();
	}

	@Override
	public void onVirtualCarrierChanged(CompoundTag virtualCarrier) {
		this.virtualCarrier = virtualCarrier.copy();
		synchronizeLinkedRenderInfo(virtualCarrier);
		numberOfInventorySlots = getDefaultNumberOfInventorySlots();
		numberOfUpgradeSlots = getDefaultNumberOfUpgradeSlots();
		onContentsUpdated();
	}

	@Override
	public void setColumnsTaken(int columnsTaken, boolean hasChanged) {
		binding.setColumnsTaken(columnsTaken);
	}

	@Override
	public int getColumnsTaken() {
		return binding.getColumnsTaken();
	}

	@Override
	public Optional<CompoundTag> getVirtualCarrierSnapshot() {
		CompoundTag snapshot = virtualCarrier.copy();
		snapshot.put(RENDER_INFO, (CompoundTag) RenderData.CODEC.encodeStart(NbtOps.INSTANCE, getRenderDataHandler().getData()).getOrThrow());
		return Optional.of(snapshot);
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
		return StorageLinkedStorageJukeboxPlaybackAnchors.getPlaybackLocation(initiatingLevel, binding.groupId());
	}

	public static CompoundTag createVirtualCarrier(StorageBlockEntity storage) {
		StorageWrapper wrapper = storage.getStorageWrapper();
		CompoundTag carrier = new CompoundTag();
		carrier.putString(STORAGE_TYPE, wrapper.getStorageType());
		carrier.putString(COMPATIBILITY_KEY, getCompatibilityKey(storage));
		carrier.putString(DISPLAY_NAME, storage.getDisplayName().getString());
		carrier.putInt(INVENTORY_SLOTS, wrapper.getInventoryHandler().size());
		carrier.putInt(UPGRADE_SLOTS, wrapper.getUpgradeHandler().size());
		carrier.putInt(BASE_STACK_SIZE_MULTIPLIER, wrapper.getBaseStackSizeMultiplier());
		carrier.put(RENDER_INFO, (CompoundTag) RenderData.CODEC.encodeStart(NbtOps.INSTANCE, wrapper.getRenderDataHandler().getData()).getOrThrow());
		return carrier;
	}

	public static CompoundTag withDisplayName(CompoundTag carrier, Component displayName) {
		CompoundTag renamedCarrier = carrier.copy();
		renamedCarrier.putString(DISPLAY_NAME, displayName.getString());
		return renamedCarrier;
	}

	public static String getCompatibilityKey(StorageBlockEntity storage) {
		if (storage instanceof LimitedBarrelBlockEntity limitedBarrel && storage.getBlockState().getBlock() instanceof LimitedBarrelBlock block) {
			return "limited:" + block.getNumberOfInventorySlots();
		}
		return STANDARD_COMPATIBILITY_KEY;
	}

	public static String getCompatibilityKey(CompoundTag carrier) {
		return carrier.getStringOr(COMPATIBILITY_KEY, STANDARD_COMPATIBILITY_KEY);
	}

	private static int displayItemCount(CompoundTag carrier) {
		String storageType = carrier.getStringOr(STORAGE_TYPE, "");
		return storageType.equals(BarrelBlockEntity.STORAGE_TYPE) || storageType.equals(LimitedBarrelBlockEntity.STORAGE_TYPE) ? 4 : 1;
	}

	private static boolean tracksCounts(CompoundTag carrier) {
		return carrier.getStringOr(STORAGE_TYPE, "").equals(LimitedBarrelBlockEntity.STORAGE_TYPE);
	}

	private void synchronizeLinkedRenderInfo(CompoundTag carrier) {
		RenderData.CODEC.parse(NbtOps.INSTANCE, carrier.getCompoundOrEmpty(RENDER_INFO)).result()
				.ifPresent(renderData -> getRenderDataHandler().reloadFrom(renderData.copy()));
	}
}
