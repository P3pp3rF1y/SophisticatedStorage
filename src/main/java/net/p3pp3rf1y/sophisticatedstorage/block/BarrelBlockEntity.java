package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import java.util.EnumMap;
import java.util.Map;

public class BarrelBlockEntity extends WoodStorageBlockEntity implements IMaterialHolder {
	public static final String MATERIALS_TAG = "materials";
	public static final String STORAGE_TYPE = "barrel";
	private Map<BarrelMaterial, ResourceLocation> materials = new EnumMap<>(BarrelMaterial.class);
	private final SophisticatedOpenersCounter openersCounter = new SophisticatedOpenersCounter() {
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.BARREL_OPEN);
			updateOpenBlockState(state, true);
		}

		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.BARREL_CLOSE);
			updateOpenBlockState(state, false);
		}

		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousOpenerCount, int newOpenerCount) {
			//noop
		}

		protected boolean isOwnContainer(Player player) {
			if (player.containerMenu instanceof StorageContainerMenu storageContainerMenu) {
				return storageContainerMenu.getStorageBlockEntity() == BarrelBlockEntity.this;
			} else {
				return false;
			}
		}
	};

	private IDynamicRenderTracker dynamicRenderTracker = IDynamicRenderTracker.NOOP;

	@Override
	public SophisticatedOpenersCounter getOpenersCounter() {
		return openersCounter;
	}

	@Override
	protected String getStorageType() {
		return STORAGE_TYPE;
	}

	protected BarrelBlockEntity(BlockPos pos, BlockState state, BlockEntityType<? extends BarrelBlockEntity> blockEntityType) {
		super(pos, state, blockEntityType);
		getStorageWrapper().getRenderInfo().setDisplayItemsChangeListener(ri -> {
			dynamicRenderTracker.onRenderInfoUpdated(ri);
			setUpdateBlockRender();
			WorldHelper.notifyBlockUpdate(this);
		});
	}

	public void setDynamicRenderTracker(IDynamicRenderTracker dynamicRenderTracker) {
		this.dynamicRenderTracker = dynamicRenderTracker;
	}

	public BarrelBlockEntity(BlockPos pos, BlockState state) {
		this(pos, state, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get());
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
		super.onDataPacket(net, pkt, registries);
		CompoundTag tag = pkt.getTag();
		if (tag.getBooleanOr(UPDATE_BLOCK_RENDER_TAG, false)) {
			dynamicRenderTracker.onRenderInfoUpdated(getStorageWrapper().getRenderInfo());
		}
	}

	void updateOpenBlockState(BlockState state, boolean open) {
		if (level == null) {
			return;
		}
		level.setBlock(getBlockPos(), state.setValue(BarrelBlock.OPEN, open), 3);
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		if (level.isClientSide && dynamicRenderTracker == IDynamicRenderTracker.NOOP) {
			dynamicRenderTracker = new DynamicRenderTracker(this);
		}
	}

	public boolean hasDynamicRenderer() {
		return dynamicRenderTracker.isDynamicRenderer();
	}

	public boolean hasFullyDynamicRenderer() {
		return dynamicRenderTracker.isFullyDynamicRenderer();
	}

	@Override
	public void toggleLock() {
		setUpdateBlockRender();
		super.toggleLock();
	}

	@Override
	protected void saveSynchronizedData(CompoundTag tag) {
		super.saveSynchronizedData(tag);
		NBTHelper.putMap(tag, MATERIALS_TAG, materials, BarrelMaterial::getSerializedName, resourceLocation -> StringTag.valueOf(resourceLocation.toString()));
	}

	@Override
	public void loadSynchronizedData(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadSynchronizedData(tag, registries);
		materials = NBTHelper.getMap(tag, MATERIALS_TAG, BarrelMaterial::fromName, (bm, t) -> t.asString().map(ResourceLocation::parse)).orElse(Map.of());
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (level != null && level.isClientSide() && tag.getBooleanOr(UPDATE_BLOCK_RENDER_TAG, false)) {
			dynamicRenderTracker.onRenderInfoUpdated(getStorageWrapper().getRenderInfo());
		}
	}

	@Override
	public void setMaterials(Map<BarrelMaterial, ResourceLocation> materials) {
		this.materials = materials;
		setChanged();
	}

	@Override
	public Map<BarrelMaterial, ResourceLocation> getMaterials() {
		return materials;
	}

	@Override
	public boolean canHoldMaterials() {
		return true;
	}
}
