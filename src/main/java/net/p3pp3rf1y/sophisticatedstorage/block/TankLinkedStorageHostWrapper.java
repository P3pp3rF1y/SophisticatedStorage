package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageVirtualHost;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageSnapshotProfile;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import java.util.Optional;
import java.util.UUID;

public final class TankLinkedStorageHostWrapper extends TankStorageWrapper implements ILinkedStorageVirtualHost {
	public static final Identifier FACTORY_ID = SophisticatedStorage.getIdentifier("tank");
	static final String DISPLAY_NAME_TAG = "displayName";
	private static final String TIER_TAG = "tier";
	private static final String HEIGHT_TAG = "height";
	private static final String LOCKED_TAG = "locked";
	private static final String RENDER_INFO_TAG = "renderInfo";
	private final ILinkedStorageContents contents;
	private final CompoundTag carrier;

	public static TankLinkedStorageHostWrapper create(ILinkedStorageContents contents, CompoundTag carrier) {
		return new TankLinkedStorageHostWrapper(contents, carrier.copy());
	}

	private TankLinkedStorageHostWrapper(ILinkedStorageContents contents, CompoundTag carrier) {
		super(contents::markChanged, contents::markRenderDirty, () -> TankTier.valueOf(carrier.getStringOr(TIER_TAG, TankTier.IRON.name())),
				() -> carrier.getIntOr(HEIGHT_TAG, 1), () -> RegistryHelper.getRegistryAccess().orElseThrow(), () -> carrier.getBooleanOr(LOCKED_TAG, false));
		this.contents = contents;
		this.carrier = carrier;
		replaceContents(contents.contents());
		setUpgradeCachesInvalidatedHandler(contents::onHandlersInvalidated);
		getRenderDataHandler()
				.reloadFrom(RenderData.CODEC.parse(NbtOps.INSTANCE, carrier.getCompoundOrEmpty(RENDER_INFO_TAG)).result().orElse(RenderData.EMPTY));
	}

	@Override
	protected void save() {
		if (contents != null) {
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
	protected void onRenderDataSerialized(RenderData renderData) {
		if (carrier != null) {
			carrier.put(RENDER_INFO_TAG, RenderData.CODEC.encodeStart(NbtOps.INSTANCE, renderData).getOrThrow());
		}
	}

	@Override
	protected void onUpgradeRefresh() {
		// Group change listeners invalidate endpoint handlers.
	}

	@Override
	public String getStorageType() {
		return STORAGE_TYPE;
	}

	@Override
	public Component getDisplayName() {
		return Component.literal(carrier.getStringOr(DISPLAY_NAME_TAG, ""));
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.of(contents.groupId());
	}

	@Override
	public void onLinkedStorageContentsChanged() {
		replaceContents(contents.contents());
	}

	@Override
	public void onLinkedStorageLayoutChanged() {
		onLinkedStorageContentsChanged();
	}

	@Override
	public void onVirtualCarrierChanged(CompoundTag updated) {
		carrier.keySet().stream().toList().forEach(carrier::remove);
		carrier.merge(updated);
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
		return Optional.of(new LinkedStorageSnapshotProfile(getDisplayName(), 0, getUpgradeHandler().size(), 0));
	}
}
