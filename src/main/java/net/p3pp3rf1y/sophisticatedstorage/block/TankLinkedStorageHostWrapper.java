package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSnapshotProfile;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import java.util.Optional;
import java.util.UUID;

public final class TankLinkedStorageHostWrapper extends TankStorageWrapper implements ILinkedStorageVirtualHost {
	public static final ResourceLocation FACTORY_ID = SophisticatedStorage.getRL("tank");
	static final String DISPLAY_NAME_TAG = "displayName";
	private static final String TIER_TAG = "tier";
	private static final String HEIGHT_TAG = "height";
	private static final String LOCKED_TAG = "locked";
	private final ILinkedStorageContents contents;
	private final CompoundTag carrier;

	public static TankLinkedStorageHostWrapper create(ILinkedStorageContents contents, CompoundTag carrier) {
		return new TankLinkedStorageHostWrapper(contents, carrier.copy());
	}
	private TankLinkedStorageHostWrapper(ILinkedStorageContents contents, CompoundTag carrier) {
		super(contents::markChanged, contents::markRenderDirty, () -> TankTier.valueOf(carrier.getString(TIER_TAG)), () -> carrier.getInt(HEIGHT_TAG),
				() -> carrier.getBoolean(LOCKED_TAG));
		this.contents = contents;
		this.carrier = carrier;
		loadCanonicalData(contents.getContents());
		setUpgradeCachesInvalidatedHandler(contents::onHandlersInvalidated);
		synchronizeLinkedRenderInfo(carrier.getCompound(RENDER_INFO_TAG));
		getRenderInfo().setRenderUpdateChangeListener(renderInfo -> contents.markRenderDirty());
	}
	@Override
	protected void save() {
		if (contents != null) {
			contents.setContents(saveCanonicalData(new CompoundTag()));
			contents.markChanged();
		}
	}
	public static CompoundTag carrier(TankTier tier, int height, Component name, boolean locked) {
		CompoundTag carrier = new CompoundTag();
		carrier.putString(TIER_TAG, tier.name());
		carrier.putInt(HEIGHT_TAG, height);
		carrier.putString(DISPLAY_NAME_TAG, name.getString());
		carrier.putBoolean(LOCKED_TAG, locked);
		return carrier;
	}
	@Override
	protected void onRenderInfoSerialized(CompoundTag renderInfo) {
		if (carrier != null) {
			carrier.put(RENDER_INFO_TAG, renderInfo.copy());
			contents.markVirtualCarrierDirty();
			contents.markRenderDirty();
		}
	}
	@Override
	protected void onUpgradeRefresh() {
		/* Group manager invalidates loaded endpoint handlers. */ }
	@Override
	public String getStorageType() {
		return STORAGE_TYPE;
	}
	@Override
	public Component getDisplayName() {
		return Component.literal(carrier.getString(DISPLAY_NAME_TAG));
	}
	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.of(contents.groupId());
	}
	@Override
	public void onLinkedStorageContentsChanged() {
		loadCanonicalData(contents.getContents());
	}
	@Override
	public void onLinkedStorageLayoutChanged() {
		loadCanonicalData(contents.getContents());
	}
	@Override
	public void onVirtualCarrierChanged(CompoundTag updated) {
		carrier.getAllKeys().stream().toList().forEach(carrier::remove);
		carrier.merge(updated);
		synchronizeLinkedRenderInfo(carrier.getCompound(RENDER_INFO_TAG));
	}
	@Override
	public Optional<CompoundTag> getVirtualCarrierSnapshot() {
		return Optional.of(carrier.copy());
	}
	@Override
	public Optional<Component> getLinkedStorageDisplayName() {
		return Optional.of(getDisplayName());
	}
	@Override
	public Optional<LinkedStorageSnapshotProfile> getLinkedStorageSnapshotProfile() {
		return Optional.of(new LinkedStorageSnapshotProfile(getDisplayName(), 0, getUpgradeHandler().getSlots(), 0));
	}
}
