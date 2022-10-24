package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.controller.ILinkable;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class StorageBlockEntity extends BlockEntity implements IControllableStorage, ILinkable {
	public static final String STORAGE_WRAPPER_TAG = "storageWrapper";
	private final StorageWrapper storageWrapper;
	@Nullable
	protected Component displayName = null;

	private boolean updateBlockRender = false;
	@Nullable
	private BlockPos controllerPos = null;
	private boolean isLinkedToController = false;
	private boolean isBeingUpgraded = false;

	protected abstract ContainerOpenersCounter getOpenersCounter();

	private boolean isDroppingContents = false;

	private boolean chunkBeingUnloaded = false;

	@Nullable
	private LazyOptional<IItemHandler> itemHandlerCap;

	protected StorageBlockEntity(BlockPos pos, BlockState state, BlockEntityType<? extends StorageBlockEntity> blockEntityType) {
		super(blockEntityType, pos, state);
		storageWrapper = new StorageWrapper(() -> this::setChanged, () -> WorldHelper.notifyBlockUpdate(this), () -> {
			setChanged();
			WorldHelper.notifyBlockUpdate(this);
		}, this instanceof BarrelBlockEntity ? 4 : 1) {

			@Override
			public Optional<UUID> getContentsUuid() {
				if (contentsUuid == null) {
					contentsUuid = UUID.randomUUID();
					save();
				}
				return Optional.of(contentsUuid);
			}

			@Override
			public ItemStack getWrappedStorageStack() {
				BlockPos pos = getBlockPos();
				return getBlockState().getBlock().getCloneItemStack(getBlockState(), new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.DOWN, pos, true), getLevel(), pos, null);
			}

			@Override
			protected void onUpgradeRefresh() {
				if (!isDroppingContents && level != null && !level.isClientSide && getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
					storageBlock.setTicking(level, getBlockPos(), getBlockState(), !storageWrapper.getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).isEmpty());
				}
			}

			@Override
			public int getDefaultNumberOfInventorySlots() {
				if (getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
					return storageBlock.getNumberOfInventorySlots();
				}
				return 0;
			}

			@Override
			protected boolean isAllowedInStorage(ItemStack stack) {
				return StorageBlockEntity.this.isAllowedInStorage(stack);
			}

			@Override
			public int getDefaultNumberOfUpgradeSlots() {
				if (getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
					return storageBlock.getNumberOfUpgradeSlots();
				}
				return 0;
			}

			@Override
			public int getBaseStackSizeMultiplier() {
				return getBlockState().getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getBaseStackSizeMultiplier() : super.getBaseStackSizeMultiplier();
			}
		};
		storageWrapper.setUpgradeCachesInvalidatedHandler(this::onUpgradeCachesInvalidated);
	}

	protected void onUpgradeCachesInvalidated() {
		invalidateStorageCap();
	}

	public boolean isOpen() {
		return getOpenersCounter().getOpenerCount() > 0;
	}

	public Optional<Component> getCustomName() {
		return Optional.ofNullable(displayName);
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		saveStorageWrapper(tag);
		saveData(tag);
		saveControllerPos(tag);
		if (isLinkedToController) {
			tag.putBoolean("isLinkedToController", isLinkedToController);
		}
	}

	private void saveStorageWrapper(CompoundTag tag) {
		tag.put(STORAGE_WRAPPER_TAG, storageWrapper.save(new CompoundTag()));
	}

	private void saveStorageWrapperClientData(CompoundTag tag) {
		tag.put(STORAGE_WRAPPER_TAG, storageWrapper.saveData(new CompoundTag()));
	}

	protected void saveData(CompoundTag tag) {
		if (displayName != null) {
			tag.putString("displayName", Component.Serializer.toJson(displayName));
		}
		if (updateBlockRender) {
			tag.putBoolean("updateBlockRender", true);
		}
	}

	public void startOpen(Player player) {
		if (!remove && !player.isSpectator() && level != null) {
			getOpenersCounter().incrementOpeners(player, level, getBlockPos(), getBlockState());
		}

	}

	public void stopOpen(Player player) {
		if (!remove && !player.isSpectator() && level != null) {
			getOpenersCounter().decrementOpeners(player, level, getBlockPos(), getBlockState());
		}
	}

	public void recheckOpen() {
		if (!remove && level != null) {
			getOpenersCounter().recheckOpeners(level, getBlockPos(), getBlockState());
		}
	}

	void playSound(BlockState state, SoundEvent sound) {
		if (level == null || !(state.getBlock() instanceof StorageBlockBase storageBlock)) {
			return;
		}
		Vec3i vec3i = storageBlock.getFacing(state).getNormal();
		double d0 = worldPosition.getX() + 0.5D + vec3i.getX() / 2.0D;
		double d1 = worldPosition.getY() + 0.5D + vec3i.getY() / 2.0D;
		double d2 = worldPosition.getZ() + 0.5D + vec3i.getZ() / 2.0D;
		level.playSound(null, d0, d1, d2, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		loadStorageWrapper(tag);
		loadData(tag);
		loadControllerPos(tag);

		if (level != null && !level.isClientSide()) {
			removeControllerPos();
			tryToAddToController();
		}
		isLinkedToController = NBTHelper.getBoolean(tag, "isLinkedToController").orElse(false);
	}

	private void loadStorageWrapper(CompoundTag tag) {
		NBTHelper.getCompound(tag, STORAGE_WRAPPER_TAG).ifPresent(storageWrapper::load);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		registerWithControllerOnLoad();
	}

	public void loadData(CompoundTag tag) {
		displayName = NBTHelper.getComponent(tag, "displayName").orElse(null);
		if (level != null && level.isClientSide) {
			if (tag.getBoolean("updateBlockRender")) {
				WorldHelper.notifyBlockUpdate(this);
			}
		} else {
			updateBlockRender = true;
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		chunkBeingUnloaded = true;
	}

	@Override
	public void setRemoved() {
		if (!isBeingUpgraded && !chunkBeingUnloaded && level != null) {
			removeFromController();
		}

		super.setRemoved();
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		CompoundTag tag = pkt.getTag();
		if (tag == null) {
			return;
		}

		loadStorageWrapper(tag);
		loadData(tag);
	}

	public void setUpdateBlockRender() {
		updateBlockRender = true;
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		saveStorageWrapperClientData(tag);
		saveData(tag);
		return tag;
	}

	public static void serverTick(Level level, BlockPos blockPos, StorageBlockEntity storageBlockEntity) {
		storageBlockEntity.getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).forEach(upgrade -> upgrade.tick(null, level, blockPos));
	}

	@Override
	public StorageWrapper getStorageWrapper() {
		return storageWrapper;
	}

	public Component getDisplayName() {
		if (displayName != null) {
			return displayName;
		}
		return getBlockState().getBlock().getName();
	}

	@SuppressWarnings("unused") //stack param used in override
	protected boolean isAllowedInStorage(ItemStack stack) {
		return true;
	}

	public void increaseStorageSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		int currentInventorySlots = getStorageWrapper().getInventoryHandler().getSlots();
		getStorageWrapper().increaseSize(additionalInventorySlots, additionalUpgradeSlots);
		if (additionalInventorySlots > 0) {
			changeSlots(currentInventorySlots + additionalInventorySlots);
		}
	}

	public void dropContents() {
		if (level == null || level.isClientSide) {
			return;
		}
		isDroppingContents = true;
		InventoryHelper.dropItems(storageWrapper.getInventoryHandler(), level, worldPosition);

		InventoryHelper.dropItems(storageWrapper.getUpgradeHandler(), level, worldPosition);
		isDroppingContents = false;
	}

	public void setCustomName(Component customName) {
		displayName = customName;
		setChanged();
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (itemHandlerCap == null) {
				itemHandlerCap = LazyOptional.of(getStorageWrapper()::getInventoryForInputOutput);
			}
			return itemHandlerCap.cast();
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		invalidateStorageCap();
	}

	private void invalidateStorageCap() {
		if (itemHandlerCap != null) {
			itemHandlerCap.invalidate();
			itemHandlerCap = null;
		}
	}

	public boolean shouldDropContents() {
		return true;
	}

	@Override
	public void setControllerPos(BlockPos controllerPos) {
		this.controllerPos = controllerPos;
		setChanged();
	}

	@Override
	public Optional<BlockPos> getControllerPos() {
		return Optional.ofNullable(controllerPos);
	}

	@Override
	public void removeControllerPos() {
		controllerPos = null;
		setChanged();
	}

	@Override
	public BlockPos getStorageBlockPos() {
		return getBlockPos();
	}

	@Override
	public Level getStorageBlockLevel() {
		return Objects.requireNonNull(getLevel());
	}

	@Override
	public void linkToController(BlockPos controllerPos) {
		isLinkedToController = true;
		ILinkable.super.linkToController(controllerPos);
		setChanged();
	}

	@Override
	public boolean isLinked() {
		return isLinkedToController && getControllerPos().isPresent();
	}

	@Override
	public void setNotLinked() {
		ILinkable.super.setNotLinked();
		isLinkedToController = false;
		setChanged();
	}

	@Override
	public boolean canConnectStorages() {
		return !isLinkedToController;
	}

	@Override
	public Set<BlockPos> getConnectablePositions() {
		return Collections.emptySet();
	}

	@Override
	public boolean connectLinkedSelf() {
		return true;
	}

	@Override
	public boolean canBeConnected() {
		return isLinked() || IControllableStorage.super.canBeConnected();
	}

	public void setBeingUpgraded() {
		isBeingUpgraded = true;
	}
}
