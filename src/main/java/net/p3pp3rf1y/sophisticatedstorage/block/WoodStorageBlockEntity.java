package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public abstract class WoodStorageBlockEntity extends StorageBlockEntity{
	@Nullable
	private WoodType woodType = null;

	protected WoodStorageBlockEntity(BlockPos pos, BlockState state, BlockEntityType<? extends StorageBlockEntity> blockEntityType) {
		super(pos, state, blockEntityType);
	}

	@Override
	protected void saveData(CompoundTag tag) {
		super.saveData(tag);
		if (woodType != null) {
			tag.putString("woodType", woodType.name());
		}
	}

	@Override
	public void loadData(CompoundTag tag) {
		super.loadData(tag);
		woodType = NBTHelper.getString(tag, "woodType").flatMap(woodTypeName -> WoodType.values().filter(wt -> wt.name().equals(woodTypeName)).findFirst())
				.orElse(getStorageWrapper().hasMainColor() && getStorageWrapper().hasAccentColor() ? null : WoodType.ACACIA);
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
		return getWoodType().map(this::makeWoodStorageDescriptionId).orElse(getBlockState().getBlock().getName());
	}
	private Component makeWoodStorageDescriptionId(WoodType wt) {
		ResourceLocation id = Objects.requireNonNull(getBlockState().getBlock().getRegistryName());
		return new TranslatableComponent("item." + id.getNamespace() + "." + wt.name() + "_" + id.getPath().replace('/', '.'));
	}
}
