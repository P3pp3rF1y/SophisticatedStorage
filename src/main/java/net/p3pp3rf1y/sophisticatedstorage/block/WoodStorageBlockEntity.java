package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public abstract class WoodStorageBlockEntity extends StorageBlockEntity {
	public static final String PACKED = "packed";
	@Nullable
	private WoodType woodType = null;

	private boolean packed = false;

	protected WoodStorageBlockEntity(BlockPos pos, BlockState state, BlockEntityType<? extends StorageBlockEntity> blockEntityType) {
		super(pos, state, blockEntityType);
	}

	@Override
	protected void saveSynchronizedData(ValueOutput out) {
		super.saveSynchronizedData(out);
		if (woodType != null) {
			out.putString("woodType", woodType.name());
		}
		out.putBoolean(PACKED, packed);
	}

	public CompoundTag getStorageContentsTag() {
		CompoundTag contents = saveWithoutMetadata(level.registryAccess());
		contents.putBoolean(PACKED, false);
		return contents;
	}

	@Override
	public void loadSynchronizedData(ValueInput in) {
		super.loadSynchronizedData(in);
		woodType = in.read("woodType", WoodType.CODEC).orElse(getStorageWrapper().hasMainColor() && getStorageWrapper().hasAccentColor() ? null : WoodType.ACACIA);
		packed = in.getBooleanOr(PACKED, false);
	}

	public Optional<WoodType> getWoodType() {
		return Optional.ofNullable(woodType);
	}

	public void setWoodType(WoodType woodType) {
		this.woodType = woodType;
		setChanged();
	}

	@Override
	public Component getDisplayName() {
		if (displayName != null) {
			return displayName;
		}
		return makeWoodStorageDescriptionId(getWoodType().orElse(null));
	}

	private Component makeWoodStorageDescriptionId(@Nullable WoodType wt) {
		String id = Util.makeDescriptionId("block", Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock())));
		return WoodStorageBlockItem.getDisplayName(id, wt);
	}

	public boolean isPacked() {
		return packed;
	}

	public void setPacked(boolean packed) {
		this.packed = packed;
		if (packed) {
			RenderInfo renderInfo = getStorageWrapper().getRenderInfo();
			renderInfo.removeAllUpgradeClientData();
		}
	}

	@Override
	public boolean shouldDropContents() {
		return !isPacked();
	}

	@Nullable
	@Override
	public IItemHandler getExternalItemHandler(@Nullable Direction side) {
		if (isPacked()) {
			return null;
		}
		return super.getExternalItemHandler(side);
	}

	@Override
	public boolean canConnectStorages() {
		return !packed && super.canConnectStorages();
	}

	@Override
	public boolean canBeConnected() {
		return !packed && super.canBeConnected();
	}

	@Override
	public boolean canBeLinked() {
		return !packed;
	}

	@Override
	protected boolean canRefreshUpgrades() {
		return super.canRefreshUpgrades() && !packed;
	}
}
