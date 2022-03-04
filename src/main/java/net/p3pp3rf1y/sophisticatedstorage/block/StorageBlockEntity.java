package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryIOHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StorageBlockEntity extends BlockEntity implements IStorageWrapper {
	private static final int DYNAMIC_CHECK_WINDOW_TICKS = 100;
	private static final int MAX_ITEM_CHANGES_IN_WINDOW = 4;
	private static final String UUID_TAG = "uuid";
	private static final String MAIN_COLOR_TAG = "mainColor";
	private static final String ACCENT_COLOR_TAG = "accentColor";
	private static final String OPEN_TAB_ID_TAG = "openTabId";
	@Nullable
	private InventoryHandler inventoryHandler = null;
	@Nullable
	private InventoryIOHandler inventoryIOHandler = null;
	@Nullable
	private UpgradeHandler upgradeHandler = null;
	private CompoundTag contentsNbt = new CompoundTag();
	private CompoundTag settingsNbt = new CompoundTag();
	private SettingsHandler settingsHandler;
	private final RenderInfo renderInfo;
	private CompoundTag renderInfoNbt = new CompoundTag();
	@Nullable
	private UUID contentsUuid = null;
	private int openTabId = -1;

	private int numberOfInventorySlots = 0;
	private int numberOfUpgradeSlots = -1;
	private int mainColor = -1;
	private int accentColor = -1;
	private SortBy sortBy = SortBy.NAME;
	private int columnsTaken = 0;
	@Nullable
	private Component displayName = null;
	@Nullable
	private WoodType woodType = null;

	private boolean updateBlockRender = false;

	private long lastItemChangeTime = 0;
	private final int[] itemChangeExpirationTimes = new int[MAX_ITEM_CHANGES_IN_WINDOW];
	private boolean dynamicRenderer = false;
	private ItemStack lastRenderItem = ItemStack.EMPTY;

	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.BARREL_OPEN);
			updateBlockState(state, true);
		}

		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.BARREL_CLOSE);
			updateBlockState(state, false);
		}

		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousOpenerCount, int newOpenerCount) {
			//noop
		}

		protected boolean isOwnContainer(Player player) {
			if (player.containerMenu instanceof StorageContainerMenu storageContainerMenu) {
				return storageContainerMenu.getStorageBlockEntity() == StorageBlockEntity.this;
			} else {
				return false;
			}
		}
	};
	private boolean isDroppingContents = false;

	public StorageBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.BARREL_TILE_TYPE.get(), pos, state);
		renderInfo = new RenderInfo(getSaveHandler()) {
			@Override
			protected void serializeRenderInfo(CompoundTag renderInfo) {
				renderInfoNbt = renderInfo;
			}

			@Override
			protected Optional<CompoundTag> getRenderInfoTag() {
				return Optional.of(renderInfoNbt);
			}
		};
		settingsHandler = new StorageSettingsHandler(settingsNbt, () -> {
			setChanged();
			WorldHelper.notifyBlockUpdate(this);
		}, () -> inventoryHandler, () -> renderInfo);
	}

	public boolean hasDynamicRenderer() {
		return dynamicRenderer;
	}

	private Supplier<Consumer<RenderInfo>> getSaveHandler() {
		return () -> ri -> {
			setChanged();
			if (level != null && level.isClientSide) {
				ItemStack item = ri.getItemDisplayRenderInfo().getItem();
				if (ItemHandlerHelper.canItemStacksStack(lastRenderItem, item)) {
					return;
				}
				lastRenderItem = item;

				boolean wasDynamic = dynamicRenderer;
				checkForItemModelCustomRenderer(item);

				if (!updateItemChangeExpirations()) {
					dynamicRenderer = true;
				}

				if (!dynamicRenderer || !wasDynamic) {
					WorldHelper.notifyBlockUpdate(StorageBlockEntity.this);
				}
			} else {
				WorldHelper.notifyBlockUpdate(StorageBlockEntity.this);
			}
		};
	}

	private void checkForItemModelCustomRenderer(ItemStack item) {
		Supplier<Runnable> modelUpdate = () -> () -> {
			Minecraft minecraft = Minecraft.getInstance();
			ItemRenderer itemRenderer = minecraft.getItemRenderer();
			BakedModel model = itemRenderer.getModel(item, null, minecraft.player, 0);
			dynamicRenderer = model.isCustomRenderer();
		};
		modelUpdate.get().run();
	}

	private boolean updateItemChangeExpirations() {
		boolean timeSet = false;
		//noinspection ConstantConditions - only called when level was already checked not to be null
		int timeDiff = (int) Math.min(DYNAMIC_CHECK_WINDOW_TICKS, level.getGameTime() - lastItemChangeTime);
		lastItemChangeTime = level.getGameTime();
		for (int i = 0; i < itemChangeExpirationTimes.length; i++) {
			int val = Math.max(0, itemChangeExpirationTimes[i] - timeDiff);
			if (!timeSet && val == 0) {
				timeSet = true;
				val = DYNAMIC_CHECK_WINDOW_TICKS;
			}
			itemChangeExpirationTimes[i] = val;
		}
		return timeSet;
	}

	public boolean isOpen() {
		return openersCounter.getOpenerCount() > 0;
	}

	public Optional<Component> getCustomName() {
		return Optional.ofNullable(displayName);
	}

	@Override
	public void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		saveContents(tag);
		saveData(tag);
	}

	private void saveContents(CompoundTag tag) {
		tag.put("contents", contentsNbt);
	}

	private void saveData(CompoundTag tag) {
		tag.put("settings", settingsNbt);
		tag.put("renderInfo", renderInfoNbt);
		if (contentsUuid != null) {
			tag.put(UUID_TAG, NbtUtils.createUUID(contentsUuid));
		}
		if (mainColor != 0) {
			tag.putInt(MAIN_COLOR_TAG, mainColor);
		}
		if (accentColor != 0) {
			tag.putInt(ACCENT_COLOR_TAG, accentColor);
		}
		if (openTabId >= 0) {
			tag.putInt(OPEN_TAB_ID_TAG, openTabId);
		}
		tag.putString("sortBy", sortBy.getSerializedName());
		if (columnsTaken > 0) {
			tag.putInt("columnsTaken", columnsTaken);
		}
		if (numberOfInventorySlots > 0) {
			tag.putInt("numberOfInventorySlots", numberOfInventorySlots);
		}
		if (numberOfUpgradeSlots > -1) {
			tag.putInt("numberOfUpgradeSlots", numberOfUpgradeSlots);
		}
		if (woodType != null) {
			tag.putString("woodType", woodType.name());
		}
		if (displayName != null) {
			tag.putString("displayName", Component.Serializer.toJson(displayName));
		}
		if (updateBlockRender) {
			tag.putBoolean("updateBlockRender", true);
		}
	}

	public void startOpen(Player player) {
		if (!remove && !player.isSpectator() && level != null) {
			openersCounter.incrementOpeners(player, level, getBlockPos(), getBlockState());
		}

	}

	public void stopOpen(Player player) {
		if (!remove && !player.isSpectator() && level != null) {
			openersCounter.decrementOpeners(player, level, getBlockPos(), getBlockState());
		}
	}

	public void recheckOpen() {
		if (!remove && level != null) {
			openersCounter.recheckOpeners(level, getBlockPos(), getBlockState());
		}
	}

	void updateBlockState(BlockState pState, boolean open) {
		if (level == null) {
			return;
		}
		level.setBlock(getBlockPos(), pState.setValue(BarrelBlock.OPEN, open), 3);
	}

	void playSound(BlockState pState, SoundEvent pSound) {
		if (level == null) {
			return;
		}
		Vec3i vec3i = pState.getValue(BarrelBlock.FACING).getNormal();
		double d0 = worldPosition.getX() + 0.5D + vec3i.getX() / 2.0D;
		double d1 = worldPosition.getY() + 0.5D + vec3i.getY() / 2.0D;
		double d2 = worldPosition.getZ() + 0.5D + vec3i.getZ() / 2.0D;
		level.playSound(null, d0, d1, d2, pSound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		loadContents(tag);
		loadData(tag);
	}

	private void loadContents(CompoundTag tag) {
		contentsNbt = tag.getCompound("contents");
		onContentsNbtUpdated();
	}

	public void loadData(CompoundTag tag) {
		settingsNbt = tag.getCompound("settings");
		settingsHandler.reloadFrom(settingsNbt);
		renderInfoNbt = tag.getCompound("renderInfo");
		renderInfo.deserializeFrom(renderInfoNbt);
		contentsUuid = NBTHelper.getTagValue(tag, UUID_TAG, CompoundTag::getCompound).map(NbtUtils::loadUUID).orElse(null);
		mainColor = NBTHelper.getInt(tag, MAIN_COLOR_TAG).orElse(-1);
		accentColor = NBTHelper.getInt(tag, ACCENT_COLOR_TAG).orElse(-1);
		openTabId = NBTHelper.getInt(tag, OPEN_TAB_ID_TAG).orElse(-1);
		sortBy = NBTHelper.getString(tag, "sortBy").map(SortBy::fromName).orElse(SortBy.NAME);
		columnsTaken = NBTHelper.getInt(tag, "columnsTaken").orElse(0);
		numberOfInventorySlots = NBTHelper.getInt(tag, "numberOfInventorySlots").orElse(0);
		numberOfUpgradeSlots = NBTHelper.getInt(tag, "numberOfUpgradeSlots").orElse(-1);
		woodType = NBTHelper.getString(tag, "woodType").flatMap(woodTypeName -> WoodType.values().filter(wt -> wt.name().equals(woodTypeName)).findFirst()).orElse(null);
		displayName = NBTHelper.getComponent(tag, "displayName").orElse(null);
		if (level != null && level.isClientSide) {
			if (tag.getBoolean("updateBlockRender")) {
				WorldHelper.notifyBlockUpdate(this);
			}
		} else {
			updateBlockRender = true;
		}
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

		loadData(tag);
	}

	public void setUpdateBlockRender() {
		updateBlockRender = true;
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		saveData(tag);
		return tag;
	}

	public static void serverTick(Level level, BlockPos blockPos, StorageBlockEntity storageBlockEntity) {
		storageBlockEntity.getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).forEach(upgrade -> upgrade.tick(null, level, blockPos));
	}

	public Component getDisplayName() {
		if (displayName != null) {
			return displayName;
		}
		return getBlockState().getBlock().getName();
	}

	@Override
	public void setSaveHandler(Runnable saveHandler) {
		//noop
	}

	@Override
	public IItemHandlerSimpleInserter getInventoryForUpgradeProcessing() {
		return getInventoryHandler();
	}

	//TODO make sure that inventory handlers are properly refreshed if that require to be based on upgrade changes - definitely IO handler needs to be when filter upgrades are put in
	@Override
	public InventoryHandler getInventoryHandler() {
		if (inventoryHandler == null) {
			initInventoryHandler();
		}
		return inventoryHandler;
	}

	private void initInventoryHandler() {
		inventoryHandler = new InventoryHandler(getNumberOfInventorySlots(), this, contentsNbt, this::setChanged, StackUpgradeItem.getInventorySlotLimit(this), Config.COMMON.stackUpgrade) {
			@Override
			protected boolean isAllowed(ItemStack stack) {
				return true;
			}
		};
		inventoryHandler.addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
	}

	private int getNumberOfInventorySlots() {
		if (numberOfInventorySlots > 0) {
			return numberOfInventorySlots;
		}
		if (getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
			numberOfInventorySlots = storageBlock.getNumberOfInventorySlots();
			setChanged();
		}

		return numberOfInventorySlots;
	}

	@Override
	public int getNumberOfSlotRows() {
		int itemInventorySlots = getNumberOfInventorySlots();
		return (int) Math.ceil(itemInventorySlots <= 81 ? (double) itemInventorySlots / 9 : (double) itemInventorySlots / 12);
	}

	@Override
	public IItemHandlerSimpleInserter getInventoryForInputOutput() {
		if (inventoryIOHandler == null) {
			inventoryIOHandler = new InventoryIOHandler(this);
		}
		return inventoryIOHandler.getFilteredItemHandler();
	}

	public SettingsHandler getSettingsHandler() {
		return settingsHandler;
	}

	public UpgradeHandler getUpgradeHandler() {
		if (upgradeHandler == null) {
			upgradeHandler = new UpgradeHandler(getNumberOfUpgradeSlots(), this, contentsNbt, this::setChanged, () -> {
				if (inventoryHandler != null) {
					inventoryHandler.clearListeners();
					inventoryHandler.setSlotLimit(StackUpgradeItem.getInventorySlotLimit(this));
				}
				getInventoryHandler().addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
				inventoryIOHandler = null;
			}) {
				@Override
				public boolean isItemValid(int slot, ItemStack stack) {
					//noinspection ConstantConditions - by this time the upgrade has registryName so it can't be null
					return super.isItemValid(slot, stack) && (stack.isEmpty() || SophisticatedStorage.MOD_ID.equals(stack.getItem().getRegistryName().getNamespace()) || stack.is(ModItems.STORAGE_UPGRADE_TAG));
				}

				@Override
				public void refreshUpgradeWrappers() {
					super.refreshUpgradeWrappers();
					if (!isDroppingContents && level != null && !level.isClientSide && getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
						storageBlock.setTicking(level, getBlockPos(), getBlockState(), !getWrappersThatImplement(ITickableUpgrade.class).isEmpty());
					}
				}
			};
		}
		return upgradeHandler;
	}

	private int getNumberOfUpgradeSlots() {
		if (numberOfUpgradeSlots > -1) {
			return numberOfUpgradeSlots;
		}
		if (getBlockState().getBlock() instanceof IStorageBlock storageBlock) {
			numberOfUpgradeSlots = storageBlock.getNumberOfUpgradeSlots();
			setChanged();
		}

		return numberOfUpgradeSlots;
	}

	public Optional<UUID> getContentsUuid() {
		if (contentsUuid == null) {
			contentsUuid = UUID.randomUUID();
			setChanged();
		}
		return Optional.of(contentsUuid);
	}

	@Override
	public int getMainColor() {
		return mainColor;
	}

	public void setMainColor(int mainColor) {
		this.mainColor = mainColor;
	}

	@Override
	public int getAccentColor() {
		return accentColor;
	}

	public void setAccentColor(int accentColor) {
		this.accentColor = accentColor;
	}

	@Override
	public Optional<Integer> getOpenTabId() {
		return openTabId >= 0 ? Optional.of(openTabId) : Optional.empty();
	}

	@Override
	public void setOpenTabId(int openTabId) {
		this.openTabId = openTabId;
		setChanged();
	}

	@Override
	public void removeOpenTabId() {
		openTabId = -1;
		setChanged();
	}

	@Override
	public void setColors(int mainColor, int accentColor) {
		this.mainColor = mainColor;
		this.accentColor = accentColor;
		setChanged();
	}

	@Override
	public void setSortBy(SortBy sortBy) {
		this.sortBy = sortBy;
		setChanged();
	}

	@Override
	public SortBy getSortBy() {
		return sortBy;
	}

	@Override
	public void sort() {
		Set<Integer> slotIndexesExcludedFromSort = new HashSet<>();
		slotIndexesExcludedFromSort.addAll(getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class).getNoSortSlots());
		slotIndexesExcludedFromSort.addAll(getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotIndexes());
		InventorySorter.sortHandler(getInventoryHandler(), getComparator(), slotIndexesExcludedFromSort);
	}

	private Comparator<Map.Entry<ItemStackKey, Integer>> getComparator() {
		return switch (getSortBy()) {
			case COUNT -> InventorySorter.BY_COUNT;
			case TAGS -> InventorySorter.BY_TAGS;
			case NAME -> InventorySorter.BY_NAME;
		};
	}

	@Override
	public void onContentsNbtUpdated() {
		inventoryHandler = null;
		upgradeHandler = null;
		refreshInventoryForUpgradeProcessing();
	}

	@Override
	public void refreshInventoryForUpgradeProcessing() {
		refreshInventoryForInputOutput();
	}

	@Override
	public void refreshInventoryForInputOutput() {
		inventoryIOHandler = null;
	}

	@Override
	public void setPersistent(boolean persistent) {
		//noop
	}

	@Override
	public void fillWithLoot(Player playerEntity) {
		//noop
	}

	@Override
	public RenderInfo getRenderInfo() {
		return renderInfo;
	}

	@Override
	public void setColumnsTaken(int columnsTaken) {
		this.columnsTaken = columnsTaken;
		setChanged();
	}

	@Override
	public int getColumnsTaken() {
		return columnsTaken;
	}

	public void dropContents() {
		if (level == null || level.isClientSide || inventoryHandler == null) {
			return;
		}
		isDroppingContents = true;
		InventoryHelper.dropItems(inventoryHandler, level, worldPosition);

		if (upgradeHandler != null) {
			InventoryHelper.dropItems(upgradeHandler, level, worldPosition);
		}
		isDroppingContents = false;
	}

	public Optional<WoodType> getWoodType() {
		return Optional.ofNullable(woodType);
	}

	public void setCustomName(Component customName) {
		displayName = customName;
		setChanged();
	}

	public void setWoodType(WoodType woodType) {
		this.woodType = woodType;
		setChanged();
	}

	public void increaseSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		numberOfInventorySlots += additionalInventorySlots;
		numberOfUpgradeSlots += additionalUpgradeSlots;

		getInventoryHandler().increaseSize(additionalInventorySlots);
		getUpgradeHandler().increaseSize(additionalUpgradeSlots);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return LazyOptional.of(this::getInventoryForInputOutput).cast();
		}
		return super.getCapability(cap, side);
	}
}
