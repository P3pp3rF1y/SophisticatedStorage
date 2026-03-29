package net.p3pp3rf1y.sophisticatedstorage.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import net.p3pp3rf1y.sophisticatedcore.api.IStorageSavedData;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderer;
import net.p3pp3rf1y.sophisticatedcore.client.render.UpgradeRenderRegistry;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeRenderDataType;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.item.*;

import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.UnaryOperator;

public abstract class StorageHolderBase implements ILockable, ICountDisplay, ITierDisplay, IUpgradeDisplay, IFillLevelDisplay, IMaterialHolder {
	public static final String UPGRADES_VISIBLE_TAG = "upgradesVisible";
	public static final String SORT_BY_TAG = "sortBy";
	private static final String LOCK_VISIBLE_TAG = "lockVisible";
	private static final String COUNTS_VISIBLE_TAG = "countsVisible";
	private static final String FILL_LEVELS_VISIBLE_TAG = "fillLevelsVisible";
	public static final String LOCKED_TAG = "locked";
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

	public void onStackChanged() {
		setSyncedStorageStack(getStorageWrapper().getWrappedStorageStack());
		updateRenderAttributes = true;
	}

	public void updateStorageWrapper() {
		ItemStack storageItem = getSyncedStorageStack();
		if (!NBTHelper.hasTag(storageItem, StorageWrapper.UUID_TAG)) {
			NBTHelper.setUniqueId(storageItem, StorageWrapper.UUID_TAG, UUID.randomUUID());
			setStorageItem(storageItem);
		}

		storageWrapper = MovingStorageWrapper.fromStack(storageItem, this::onContentsChanged, this::onStackChanged, this::getStorageData, this::isLocked, this::setLocked, this::isUpgradeRunnable);
	}

	protected boolean isUpgradeRunnable(ItemStack upgrade) {
		return true;
	}

	protected abstract IStorageSavedData getStorageData(UUID storageId);

	private void onContentsChanged() {
		if (getLevel() == null || getLevel().isClientSide()) {
			return;
		}

		ItemStack storageItem = getSyncedStorageStack();
		NBTHelper.getUniqueId(storageItem, StorageWrapper.UUID_TAG).ifPresent(uuid -> getStorageData(uuid).markChanged());
	}

	public void setStorageItem(ItemStack storageItem) {
		setSyncedStorageStack(storageItem);
		storageWrapper = NoopStorageWrapper.INSTANCE; //reset storage wrapper to force update when it's next requested
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
		return NBTHelper.getBoolean(getSyncedStorageStack(), UPGRADES_VISIBLE_TAG).orElse(false);
	}

	public boolean areCountsVisible() {
		return NBTHelper.getBoolean(getSyncedStorageStack(), COUNTS_VISIBLE_TAG).orElse(true);
	}

	public boolean areFillLevelsVisible() {
		return NBTHelper.getBoolean(getSyncedStorageStack(), FILL_LEVELS_VISIBLE_TAG).orElse(false);
	}

	public boolean isLockVisible() {
		return NBTHelper.getBoolean(getSyncedStorageStack(), LOCK_VISIBLE_TAG).orElse(true);
	}

	protected boolean isPacked(ItemStack storageItem) {
		return WoodStorageBlockItem.isPacked(getSyncedStorageStack());
	}

	public CompoundTag getRenderInfoNbt(ItemStack storageItem) {
		return NBTHelper.getCompound(storageItem, StorageWrapper.RENDER_INFO_TAG).orElse(new CompoundTag());
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
			renderBlockEntity.getStorageWrapper().getRenderInfo().deserializeFrom(getRenderInfoNbt(storageItem));
			if (renderBlockEntity.shouldShowUpgrades() != areUpgradesVisible()) {
				renderBlockEntity.toggleUpgradesVisiblity();
			}
			if (storageItem.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				renderBlockEntity.getStorageWrapper().setColors(tintableBlockItem.getMainColor(storageItem).orElse(-1), tintableBlockItem.getAccentColor(storageItem).orElse(-1));
			}
			if (renderBlockEntity instanceof WoodStorageBlockEntity woodStorage) {
				WoodStorageBlockItem.getWoodType(storageItem).ifPresent(woodType -> {
					if (woodStorage.getWoodType() != WoodStorageBlockItem.getWoodType(storageItem)) {
						woodStorage.setWoodType(woodType);
					}
				});
				boolean isPacked = isPacked(storageItem);
				if (woodStorage.isPacked() != isPacked) {
					woodStorage.setPacked(isPacked);
				}
			}
			if (renderBlockEntity instanceof BarrelBlockEntity barrel) {
				barrel.setDynamicRenderTracker(new DynamicRenderTracker(barrel) {
					@Override
					public boolean isDynamicRenderer() {
						return true;
					}
				});
				Map<BarrelMaterial, ResourceLocation> materials = BarrelBlockItem.getMaterials(storageItem);
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
		PiglinAi.angerNearbyPiglins(player, true);
		sendOpenness(entity);
	}

	protected abstract void sendOpenness(Entity entity);

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
		getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).forEach(upgrade -> upgrade.tick(getEntity(), level, new BlockPos((int) getPosition().x(), (int) getPosition().y(), (int) getPosition().z())));
	}

	private void clientTick(Level level) {
		if (updateRenderAttributes && getRenderBlockEntity() != null) {
			updateRenderBlockEntityAttributes(getSyncedStorageStack(), getRenderBlockEntity());
		}
		if (level.random.nextInt(10) == 0) {
			RenderInfo renderInfo = getStorageWrapper().getRenderInfo();
			renderUpgrades(level, level.random, renderInfo);
		}
		if (getRenderBlockEntity() instanceof ChestBlockEntity chestBlockEntity) {
			ChestBlockEntity.lidAnimateTick(chestBlockEntity);
		} else if (getRenderBlockEntity() instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
			ShulkerBoxBlockEntity.tick(null, BlockPos.ZERO, getRenderBlockEntity().getBlockState(), shulkerBoxBlockEntity);
		}
	}

	protected void renderUpgrades(Level level, RandomSource rand, RenderInfo renderInfo) {
		if (Minecraft.getInstance().isPaused()) {
			return;
		}
		renderInfo.getUpgradeRenderData().forEach((type, data) -> UpgradeRenderRegistry.getUpgradeRenderer(type)
				.ifPresent(renderer -> renderUpgrade(renderer, level, rand, type, data)));
	}

	private <T extends IUpgradeRenderData> void renderUpgrade(IUpgradeRenderer<T> renderer, Level level, RandomSource rand, UpgradeRenderDataType<?> type, IUpgradeRenderData data) {
		//noinspection unchecked
		type.cast(data).ifPresent(renderData -> renderer.render(level, rand, getUpgradeRenderPosition(), (T) renderData));
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
		ItemStack remainingStack = itemEntity.getItem().copy();
		remainingStack = InventoryHelper.runPickupOnPickupResponseUpgrades(level, getStorageWrapper().getUpgradeHandler(), remainingStack, false);
		if (remainingStack.getCount() < itemEntity.getItem().getCount()) {
			itemEntity.setItem(remainingStack);
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
		if (isPacked(getSyncedStorageStack())) {
			return InteractionResult.PASS;
		}

		openMenu(player);
		return player.level().isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
	}

	protected abstract void openMenu(Player player);

	@Override
	public void toggleLock() {
		ItemStack storageItem = getSyncedStorageStack();
		boolean locked = !isLocked(storageItem);

		if (memorizesItemsWhenLocked()) {
			if (locked) {
				getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).selectSlots(0, getStorageWrapper().getInventoryHandler().getSlots());
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
		storageItem.getOrCreateTag().putBoolean(LOCK_VISIBLE_TAG, !isLockVisible());
		setStorageItem(storageItem);
	}

	@Override
	public boolean shouldShowCounts() {
		return areCountsVisible();
	}

	@Override
	public void toggleCountVisibility() {
		ItemStack storageItem = getSyncedStorageStack();
		storageItem.getOrCreateTag().putBoolean(COUNTS_VISIBLE_TAG, !areCountsVisible());
		setStorageItem(storageItem);
	}

	@Override
	public List<Integer> getSlotCounts() {
		return MovingStorageWrapper.isLimitedBarrel(getSyncedStorageStack()) ? getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getSlotCounts() : List.of();
	}

	@Override
	public boolean shouldShowFillLevels() {
		return areFillLevelsVisible();
	}

	@Override
	public void toggleFillLevelVisibility() {
		ItemStack storageItem = getSyncedStorageStack();
		storageItem.getOrCreateTag().putBoolean(FILL_LEVELS_VISIBLE_TAG, !areFillLevelsVisible());
		setStorageItem(storageItem);
	}

	@Override
	public List<Float> getSlotFillLevels() {
		return MovingStorageWrapper.isLimitedBarrel(getSyncedStorageStack()) ? getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getSlotFillRatios() : List.of();
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
		storageItem.getOrCreateTag().putBoolean(UPGRADES_VISIBLE_TAG, !areUpgradesVisible());
		setStorageItem(storageItem);
	}

	public boolean isOpen() {
		return getOpenersCounter().getOpenerCount() > 0;
	}

	public boolean isPacked() {
		return isPacked(getSyncedStorageStack());
	}

	@Override
	public void setMaterials(Map<BarrelMaterial, ResourceLocation> materials) {
		ItemStack storageItem = getSyncedStorageStack();
		if (isBarrel()) {
			BarrelBlockItem.setMaterials(storageItem, materials);
			setStorageItem(storageItem);
		}
	}

	@Override
	public Map<BarrelMaterial, ResourceLocation> getMaterials() {
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
