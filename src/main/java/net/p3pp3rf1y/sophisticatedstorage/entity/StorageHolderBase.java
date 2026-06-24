package net.p3pp3rf1y.sophisticatedstorage.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageSavedData;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeClientTickHandler;
import net.p3pp3rf1y.sophisticatedcore.client.render.UpgradeClientRegistry;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeClientDataType;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.*;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.UnaryOperator;

public abstract class StorageHolderBase implements ILockable, ICountDisplay, ITierDisplay, IUpgradeDisplay, IFillLevelDisplay, IMaterialHolder {
	@Nullable
	private MovingStorageOpenersCounter openersCounter = null;

	protected boolean updateRenderAttributes = false;
	protected IStorageWrapper storageWrapper = NoopStorageWrapper.INSTANCE;
	protected boolean isMainStorage = true;
	private final boolean showChestUpgradesOnTop;

	protected StorageHolderBase(boolean showChestUpgradesOnTop) {
		this.showChestUpgradesOnTop = showChestUpgradesOnTop;
	}

	private MovingStorageOpenersCounter getOpenersCounter() {
		if (openersCounter == null) {
			openersCounter = new MovingStorageOpenersCounter() {
				@Override
				protected void onOpen() {
					if (isBarrel()) {
						playSound(SoundEvents.BARREL_OPEN);
						updateBarrelOpenBlockState(true);
					} else if (isShulkerBox()) {
						playSound(SoundEvents.SHULKER_BOX_OPEN);
						if (getRenderBlockEntity() instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
							shulkerBoxBlockEntity.setAnimationStatus(ShulkerBoxBlockEntity.AnimationStatus.OPENING);
						}
					} else if (isChest()) {
						if (isMainStorage) {
							playSound(SoundEvents.CHEST_OPEN);
						}
						if (getRenderBlockEntity() instanceof ChestBlockEntity chestBlockEntity) {
							chestBlockEntity.getChestLidController().shouldBeOpen(true);
						}
					}
				}

				@Override
				protected void onClose() {
					if (isBarrel()) {
						playSound(SoundEvents.BARREL_CLOSE);
						updateBarrelOpenBlockState(false);
					} else if (isShulkerBox()) {
						playSound(SoundEvents.SHULKER_BOX_CLOSE);
						if (getRenderBlockEntity() instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
							shulkerBoxBlockEntity.setAnimationStatus(ShulkerBoxBlockEntity.AnimationStatus.CLOSING);
						}
					} else if (isChest()) {
						if (isMainStorage) {
							playSound(SoundEvents.CHEST_CLOSE);
						}
						if (getRenderBlockEntity() instanceof ChestBlockEntity chestBlockEntity) {
							chestBlockEntity.getChestLidController().shouldBeOpen(false);
						}
					}
				}

				@Override
				protected boolean isOwnContainer(Player player) {
					return StorageHolderBase.this.isOwnContainer(player);
				}
			};
		}
		return openersCounter;
	}

	protected abstract boolean isOwnContainer(Player player);

	protected abstract void playSound(SoundEvent sound);

	@Nullable
	protected abstract Entity getEntity();

	private void onStackChanged() {
		setSyncedStorageStack(getStorageWrapper().getWrappedStorageStack());
		updateRenderAttributes = true;
	}

	public void updateStorageWrapper() {
		ItemStack storageItem = getSyncedStorageStack();
		UUID id = storageItem.get(ModCoreDataComponents.STORAGE_UUID);
		if (id == null) {
			id = UUID.randomUUID();
			storageItem.set(ModCoreDataComponents.STORAGE_UUID, id);
			setStorageItem(storageItem);
		}

		storageWrapper = MovingStorageWrapper.fromStack(storageItem, this::onContentsChanged, this::onStackChanged, this::getStorageData, this::isLocked,
				this::setLocked, this::isUpgradeRunnable);
	}

	protected boolean isUpgradeRunnable(ItemStack upgrade) {
		return true;
	}

	protected abstract IStorageSavedData getStorageData();

	private void onContentsChanged() {
		if (getLevel() == null || getLevel().isClientSide()) {
			return;
		}

		ItemStack storageItem = getSyncedStorageStack();
		@Nullable
		UUID storageId = storageItem.get(ModCoreDataComponents.STORAGE_UUID);
		if (storageId == null) {
			return;
		}
		getStorageData().markChanged();
	}

	public void setStorageItem(ItemStack storageItem) {
		setSyncedStorageStack(storageItem);
		storageWrapper = NoopStorageWrapper.INSTANCE; // reset storage wrapper to force update when it's next requested
		updateRenderAttributes = true;
	}

	public IStorageWrapper getStorageWrapper() {
		if (isPacked()) {
			return NoopStorageWrapper.INSTANCE;
		}

		if (!getSyncedStorageStack().isEmpty() && storageWrapper == NoopStorageWrapper.INSTANCE) {
			updateStorageWrapper();
		}

		return storageWrapper;
	}

	public boolean isBarrel() {
		return getSyncedStorageStack().getItem() instanceof BarrelBlockItem;
	}

	protected boolean isShulkerBox() {
		return getSyncedStorageStack().getItem() instanceof ShulkerBoxItem;
	}

	protected boolean isChest() {
		return getSyncedStorageStack().getItem() instanceof ChestBlockItem;
	}

	public boolean areUpgradesVisible() {
		return getSyncedStorageStack().getOrDefault(ModDataComponents.UPGRADES_VISIBLE, false);
	}

	public boolean areCountsVisible() {
		return getSyncedStorageStack().getOrDefault(ModDataComponents.COUNTS_VISIBLE, true);
	}

	public boolean areFillLevelsVisible() {
		return getSyncedStorageStack().getOrDefault(ModDataComponents.FILL_LEVELS_VISIBLE, false);
	}

	public boolean isLockVisible() {
		return getSyncedStorageStack().getOrDefault(ModDataComponents.LOCK_VISIBLE, true);
	}

	public boolean isPacked() {
		return WoodStorageBlockItem.isPacked(getSyncedStorageStack());
	}

	public RenderData getRenderData(ItemStack storageItem) {
		return storageItem.getOrDefault(ModCoreDataComponents.RENDER_DATA, RenderData.EMPTY).copy();
	}

	protected abstract void setSyncedStorageStack(ItemStack storageStack);

	protected abstract ItemStack getSyncedStorageStack();

	protected abstract boolean isLocked(ItemStack storageItem);

	@Nullable
	protected abstract Level getLevel();

	protected abstract Vec3 getPosition();

	private void updateBarrelOpenBlockState(boolean open) {
		if (getRenderBlockEntity() instanceof BarrelBlockEntity barrelBlockEntity && !(barrelBlockEntity instanceof LimitedBarrelBlockEntity)) {
			barrelBlockEntity.setBlockState(barrelBlockEntity.getBlockState().setValue(BarrelBlock.OPEN, open));
		}
	}

	@Nullable
	public StorageBlockEntity getRenderBlockEntity() {
		StorageBlockEntity renderBlockEntity = retrieveRenderBlockEntity();

		if (renderBlockEntity != null) {
			updateRenderBlockEntityAttributes(getSyncedStorageStack(), renderBlockEntity);
		}

		return renderBlockEntity;
	}

	protected void updateRenderBlockEntityAttributes(ItemStack storageItem, StorageBlockEntity renderBlockEntity) {
		if (updateRenderAttributes) {
			updateRenderAttributes = false;
			renderBlockEntity.getOpenersCounter().setForPhysicalBlock(false);
			if (renderBlockEntity.isLocked() != isLocked(storageItem)) {
				renderBlockEntity.toggleLock();
			}
			if (renderBlockEntity.shouldShowLock() != isLockVisible()) {
				renderBlockEntity.toggleLockVisibility();
			}
			if (renderBlockEntity.shouldShowTier() != shouldShowTier()) {
				renderBlockEntity.toggleTierVisiblity();
			}
			renderBlockEntity.getStorageWrapper().getRenderDataHandler().reloadFrom(getRenderData(storageItem));
			if (renderBlockEntity.shouldShowUpgrades() != areUpgradesVisible()) {
				renderBlockEntity.toggleUpgradesVisiblity();
			}
			if (storageItem.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				renderBlockEntity.getStorageWrapper().setColors(tintableBlockItem.getMainColor(storageItem).orElse(-1),
						tintableBlockItem.getAccentColor(storageItem).orElse(-1));
			}
			if (renderBlockEntity instanceof WoodStorageBlockEntity woodStorage) {
				WoodStorageBlockItem.getWoodType(storageItem).ifPresent(woodType -> {
					if (woodStorage.getWoodType() != WoodStorageBlockItem.getWoodType(storageItem)) {
						woodStorage.setWoodType(woodType);
					}
				});
				boolean isPacked = isPacked();
				if (woodStorage.isPacked() != isPacked) {
					woodStorage.setPacked(isPacked);
				}
			}
			if (renderBlockEntity instanceof BarrelBlockEntity barrel) {
				Map<BarrelMaterial, Identifier> materials = BarrelBlockItem.getMaterials(storageItem);
				if (!barrel.getMaterials().equals(materials)) {
					barrel.setMaterials(materials);
				}

				if (renderBlockEntity instanceof LimitedBarrelBlockEntity limitedBarrelBlockEntity) {
					if (limitedBarrelBlockEntity.shouldShowFillLevels() != areFillLevelsVisible()) {
						limitedBarrelBlockEntity.toggleFillLevelVisibility();
					}
					if (limitedBarrelBlockEntity.shouldShowCounts() != areCountsVisible()) {
						limitedBarrelBlockEntity.toggleCountVisibility();
					}
					limitedBarrelBlockEntity.setUseLightInFrontForFrontRender(false);
				}
			}
			if (renderBlockEntity instanceof ChestBlockEntity chestBlockEntity) {
				chestBlockEntity.showUpgradesOnTop = showChestUpgradesOnTop;
			}
		}
	}

	public void startOpen(Player player, Entity entity) {
		if (!(player.level() instanceof ServerLevel)) {
			return;
		}

		if (!player.isSpectator()) {
			getOpenersCounter().incrementOpeners(player, entity);
		}
		if (player.level() instanceof ServerLevel serverLevel) {
			PiglinAi.angerNearbyPiglins(serverLevel, player, true);
		}
		sendOpenness(entity);
	}

	private void sendOpenness(Entity entity) {
		CustomPacketPayload opennessPayload = createOpennessPayload();
		if (opennessPayload == null) {
			return;
		}
		PacketDistributor.sendToPlayersTrackingEntity(entity, opennessPayload);
	}

	@Nullable
	protected abstract CustomPacketPayload createOpennessPayload();

	public void setShouldBeOpen(boolean shouldBeOpen) {
		if (getRenderBlockEntity() != null) {
			getRenderBlockEntity().setShouldBeOpen(shouldBeOpen);
		}
	}

	public void stopOpen(Player player, Entity entity) {
		if (!(player.level() instanceof ServerLevel)) {
			return;
		}

		if (!player.isSpectator()) {
			getOpenersCounter().decrementOpeners(player, entity);
		}
		sendOpenness(entity);
	}

	public void tick(Entity entity) {
		Level level = getLevel();
		if (level == null) {
			return;
		}
		getOpenersCounter().tick(level, entity);
		if (level.isClientSide()) {
			clientTick(level);
			return;
		}
		runTickableUpgrades(level);
		runPickupOnItemEntities(level);
	}

	protected void runTickableUpgrades(Level level) {
		getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class)
				.forEach(upgrade -> upgrade.tick(getEntity(), level, new BlockPos((int) getPosition().x(), (int) getPosition().y(), (int) getPosition().z())));
	}

	private void clientTick(Level level) {
		if (updateRenderAttributes && getRenderBlockEntity() != null) {
			updateRenderBlockEntityAttributes(getSyncedStorageStack(), getRenderBlockEntity());
		}
		if (level.random.nextInt(10) == 0) {
			RenderDataHandler renderDataHandler = getStorageWrapper().getRenderDataHandler();
			renderUpgrades(level, level.random, renderDataHandler);
		}
		if (getRenderBlockEntity() instanceof ChestBlockEntity chestBlockEntity) {
			ChestBlockEntity.lidAnimateTick(chestBlockEntity);
		} else if (getRenderBlockEntity() instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
			ShulkerBoxBlockEntity.tick(null, BlockPos.ZERO, getRenderBlockEntity().getBlockState(), shulkerBoxBlockEntity);
		}
	}

	protected void renderUpgrades(Level level, RandomSource rand, RenderDataHandler renderDataHandler) {
		if (Minecraft.getInstance().isPaused()) {
			return;
		}
		renderDataHandler.getUpgradeClientData().forEach((type, data) -> UpgradeClientRegistry.getUpgradeClientTickHandler(type)
				.ifPresent(renderer -> clientTickUpgrade(renderer, level, rand, type, data)));
	}

	private <T extends IUpgradeClientData> void clientTickUpgrade(IUpgradeClientTickHandler<T> renderer, Level level, RandomSource rand,
			UpgradeClientDataType<?> type, IUpgradeClientData data) {
		// noinspection unchecked
		type.cast(data).ifPresent(clientData -> renderer.onClientTick(level, rand, getUpgradeRenderPosition(), (T) clientData));
	}

	protected UnaryOperator<Vector3f> getUpgradeRenderPosition() {
		return vector -> vector.add((float) getPosition().x(), (float) getPosition().y() + getUpgradeRenderYOffset(), (float) getPosition().z());
	}

	protected float getUpgradeRenderYOffset() {
		return 0.8f;
	}

	private void runPickupOnItemEntities(Level level) {
		AABB aabb = getPickupBoundingBox();
		List<ItemEntity> collidedWithItemEntities = level.getEntitiesOfClass(ItemEntity.class, aabb);
		collidedWithItemEntities.forEach(itemEntity -> {
			if (itemEntity.isAlive()) {
				tryToPickup(level, itemEntity);
			}
		});
	}

	protected void tryToPickup(Level level, ItemEntity itemEntity) {
		ItemStack stack = itemEntity.getItem();
		try (Transaction tx = Transaction.openRoot()) {
			ItemResource resource = ItemResource.of(stack);
			int pickedUp = InventoryHelper.runPickupOnPickupResponseUpgrades(level, getStorageWrapper().getUpgradeHandler(), resource, stack.getCount(), tx);
			if (pickedUp > 0) {
				tx.commit();
				itemEntity.setItem(resource.toStack(stack.getCount() - pickedUp));
			}
		}
	}

	@Nullable
	protected abstract StorageBlockEntity retrieveRenderBlockEntity();

	protected abstract void setLocked(boolean locked);

	protected abstract AABB getPickupBoundingBox();

	public void onStorageItemSynced() {
		StorageBlockEntity renderBlockEntity = getRenderBlockEntity();
		if (renderBlockEntity != null && renderBlockEntity.getBlockState().getBlock().asItem() != getSyncedStorageStack().getItem()) {
			refreshRenderBlockEntity();
		}
		updateRenderAttributes = true;
		storageWrapper = NoopStorageWrapper.INSTANCE;
	}

	protected abstract void refreshRenderBlockEntity();

	public InteractionResult openContainerMenu(Player player) {
		if (isPacked()) {
			return InteractionResult.PASS;
		}

		openMenu(player);
		return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
	}

	protected abstract void openMenu(Player player);

	@Override
	public void toggleLock() {
		ItemStack storageItem = getSyncedStorageStack();
		boolean locked = !isLocked(storageItem);

		if (memorizesItemsWhenLocked()) {
			if (locked) {
				getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).selectSlots(0,
						getStorageWrapper().getInventoryHandler().size());
			} else {
				getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).unselectAllSlots();
				ItemDisplaySettingsCategory itemDisplaySettings = getStorageWrapper().getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
				InventoryHelper.iterate(getStorageWrapper().getInventoryHandler(), (slot, stack) -> {
					if (stack.isEmpty()) {
						itemDisplaySettings.itemChanged(slot);
					}
				});
			}
		}

		setLocked(locked);
		setStorageItem(storageItem);
	}

	private boolean memorizesItemsWhenLocked() {
		return MovingStorageWrapper.isLimitedBarrel(getSyncedStorageStack());
	}

	@Override
	public boolean isLocked() {
		return isLocked(getSyncedStorageStack());
	}

	@Override
	public boolean shouldShowLock() {
		return isLockVisible();
	}

	@Override
	public void toggleLockVisibility() {
		ItemStack storageItem = getSyncedStorageStack();
		storageItem.set(ModDataComponents.LOCK_VISIBLE, !isLockVisible());
		setStorageItem(storageItem);
	}

	@Override
	public boolean shouldShowCounts() {
		return areCountsVisible();
	}

	@Override
	public void toggleCountVisibility() {
		ItemStack storageItem = getSyncedStorageStack();
		storageItem.set(ModDataComponents.COUNTS_VISIBLE, !areCountsVisible());
		setStorageItem(storageItem);
	}

	@Override
	public List<Integer> getSlotCounts() {
		return MovingStorageWrapper.isLimitedBarrel(getSyncedStorageStack())
				? getStorageWrapper().getRenderDataHandler().getDisplayData().slotCounts()
				: List.of();
	}

	@Override
	public boolean shouldShowFillLevels() {
		return areFillLevelsVisible();
	}

	@Override
	public void toggleFillLevelVisibility() {
		ItemStack storageItem = getSyncedStorageStack();
		storageItem.set(ModDataComponents.FILL_LEVELS_VISIBLE, !areFillLevelsVisible());
		setStorageItem(storageItem);
	}

	@Override
	public List<Float> getSlotFillLevels() {
		return MovingStorageWrapper.isLimitedBarrel(getSyncedStorageStack())
				? getStorageWrapper().getRenderDataHandler().getDisplayData().slotFillRatios()
				: List.of();
	}

	@Override
	public boolean shouldShowTier() {
		return StorageBlockItem.showsTier(getSyncedStorageStack());
	}

	@Override
	public void toggleTierVisiblity() {
		ItemStack storageItem = getSyncedStorageStack();
		StorageBlockItem.setShowsTier(storageItem, !StorageBlockItem.showsTier(storageItem));
		setStorageItem(storageItem);
	}

	@Override
	public boolean shouldShowUpgrades() {
		return areUpgradesVisible();
	}

	@Override
	public void toggleUpgradesVisiblity() {
		ItemStack storageItem = getSyncedStorageStack();
		storageItem.set(ModDataComponents.UPGRADES_VISIBLE, !areUpgradesVisible());
		setStorageItem(storageItem);
	}

	public boolean isOpen() {
		return getOpenersCounter().getOpenerCount() > 0;
	}

	@Override
	public void setMaterials(Map<BarrelMaterial, Identifier> materials) {
		ItemStack storageItem = getSyncedStorageStack();
		if (isBarrel()) {
			BarrelBlockItem.setMaterials(storageItem, materials);
			setStorageItem(storageItem);
		}
	}

	@Override
	public Map<BarrelMaterial, Identifier> getMaterials() {
		return isBarrel() ? BarrelBlockItem.getMaterials(getSyncedStorageStack()) : Collections.emptyMap();
	}

	@Override
	public boolean canHoldMaterials() {
		return isBarrel();
	}

	public StorageHolderBase getMainStorageHolder() {
		return this;
	}

	public Optional<StorageHolderBase> getAuxiliaryStorageHolder() {
		return Optional.empty();
	}
}
