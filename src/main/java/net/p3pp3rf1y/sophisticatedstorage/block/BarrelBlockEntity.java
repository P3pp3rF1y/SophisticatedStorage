package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

import java.util.EnumMap;
import java.util.Map;

public class BarrelBlockEntity extends WoodStorageBlockEntity implements IMaterialHolder {
	public static final String MATERIALS = "materials";
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
			WorldHelper.notifyBlockUpdate(this);
		});
	}

	public BarrelBlockEntity(BlockPos pos, BlockState state) {
		this(pos, state, ModBlocks.BARREL_BLOCK_ENTITY_TYPE.get());
	}

	void updateOpenBlockState(BlockState state, boolean open) {
		if (level == null) {
			return;
		}
		level.setBlock(getBlockPos(), state.setValue(BarrelBlock.OPEN, open), 3);
	}

	@Override
	public void toggleLock() {
		setUpdateBlockRender();
		super.toggleLock();
	}

	@Override
	protected void saveSynchronizedData(ValueOutput out) {
		super.saveSynchronizedData(out);
		out.store(MATERIALS, BarrelBlockItem.MATERIALS_CODEC, materials);
	}

	@Override
	public void loadSynchronizedData(ValueInput in) {
		super.loadSynchronizedData(in);
		materials = in.read(MATERIALS, BarrelBlockItem.MATERIALS_CODEC).orElse(Map.of());
	}

	@Override
	public void setMaterials(Map<BarrelMaterial, ResourceLocation> materials) {
		this.materials = materials;
		updateOpaqueState();
		setChanged();
	}

	private void updateOpaqueState() {
		if (level == null || level.isClientSide || !(getBlockState().getBlock() instanceof BarrelBlock)) {
			return;
		}

		BlockState state = getBlockState();
		boolean opaque = BarrelBlock.areMaterialsOpaque(materials);
		if (state.getValue(BarrelBlock.OPAQUE) != opaque) {
			level.setBlock(getBlockPos(), state.setValue(BarrelBlock.OPAQUE, opaque), 3);
		}
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
