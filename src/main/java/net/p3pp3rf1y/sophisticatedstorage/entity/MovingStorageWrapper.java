package net.p3pp3rf1y.sophisticatedstorage.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageSavedData;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.*;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderDataHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class MovingStorageWrapper implements IStorageWrapper {
	private final Runnable stackChangeHandler;
	private final ItemStack storageStack;
	private final Runnable contentsChangeHandler;

	@Nullable
	private InventoryHandler inventoryHandler = null;
	@Nullable
	private ContentsFilteredItemHandler contentsFilteredItemHandler = null;

	@Nullable
	private ITrackedContentsItemResourceHandler inventoryForUpgradeProcessing = null;
	@Nullable
	private InventoryIOHandler inventoryIOHandler = null;
	@Nullable
	private UpgradeHandler upgradeHandler = null;

	@Nullable
	private SettingsHandler settingsHandler;
	private final RenderDataHandler renderDataHandler;
	private final Supplier<IStorageSavedData> getStorageData;

	private final Map<Class<? extends IUpgradeWrapper>, Consumer<? extends IUpgradeWrapper>> upgradeDefaultsHandlers = new HashMap<>();
	private final Predicate<ItemStack> isUpgradeRunnable;

	private MovingStorageWrapper(ItemStack storageStack, Runnable onContentsChanged, Runnable onStackChanged, Supplier<IStorageSavedData> getStorageData, Predicate<ItemStack> isUpgradeRunnable) {
		this.storageStack = storageStack;
		contentsChangeHandler = onContentsChanged;
		stackChangeHandler = onStackChanged;
		renderDataHandler = new MovingStorageRenderDataHandler(storageStack);
		this.getStorageData = getStorageData;
		this.isUpgradeRunnable = isUpgradeRunnable;

		if (isLimitedBarrel(storageStack)) {
			registerUpgradeDefaultsHandler(VoidUpgradeWrapper.class, LimitedBarrelBlockEntity.VOID_UPGRADE_VOIDING_OVERFLOW_OF_EVERYTHING_BY_DEFAULT);
		}
	}

	private static int getNumberOfDisplayItems(ItemStack stack) {
		return stack.getItem() instanceof BarrelBlockItem ? 4 : 1;
	}

	public static MovingStorageWrapper fromStack(ItemStack stack, Runnable onContentsChanged, Runnable onStackChanged, Supplier<IStorageSavedData> getStorageData, BooleanSupplier isLocked, Consumer<Boolean> setLocked, Predicate<ItemStack> isUpgradeRunnable) {
		MovingStorageWrapper movingStorageWrapper = StorageWrapperRepository.getStorageWrapper(stack, MovingStorageWrapper.class, s -> new MovingStorageWrapper(s, onContentsChanged, onStackChanged, getStorageData, isUpgradeRunnable) {
			@Override
			public boolean isLocked() {
				return isLocked.getAsBoolean();
			}

			@Override
			public void setLocked(boolean locked) {
				setLocked.accept(locked);
			}
		});
		UUID uuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		if (uuid != null) {
			movingStorageWrapper.setContentsUuid(uuid); //setting here because client side the uuid isn't in contentsnbt before this data is synced from server and it would create a new one otherwise
		}

		return movingStorageWrapper;
	}

	@Override
	public boolean isUpgradeRunnable(ItemStack upgrade) {
		return isUpgradeRunnable.test(upgrade);
	}

	private UUID getNewUuid() {
		UUID newUuid = UUID.randomUUID();
		setContentsUuid(newUuid);
		return newUuid;
	}

	public abstract boolean isLocked();

	public abstract void setLocked(boolean locked);

	@Override
	public void setContentsChangeHandler(Runnable contentsChangeHandler) {
		//noop
	}

	@Override
	public int getNumberOfSlotRows() {
		int itemInventorySlots = getNumberOfInventorySlots();
		return (int) Math.ceil(itemInventorySlots <= 81 ? (double) itemInventorySlots / 9 : (double) itemInventorySlots / 12);
	}

	@Override
	public ITrackedContentsItemResourceHandler getInventoryForUpgradeProcessing() {
		if (inventoryForUpgradeProcessing == null) {
			inventoryForUpgradeProcessing = new OverflowAwareInventoryHandler(getInventoryHandler());
		}

		return inventoryForUpgradeProcessing;
	}

	@Override
	public InventoryHandler getInventoryHandler() {
		if (inventoryHandler == null) {
			initInventoryHandler();
		}
		return inventoryHandler;
	}

	private void initInventoryHandler() {
		inventoryHandler = new InventoryHandler(getNumberOfInventorySlots(), this, getContents(), contentsChangeHandler, StackUpgradeItem.getInventorySlotLimit(this), Config.SERVER.stackUpgrade) {
			@Override
			protected boolean isAllowed(ItemResource resource) {
				return isAllowedInStorage(resource);
			}
		};
		inventoryHandler.addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
		inventoryHandler.setShouldInsertIntoEmpty(this::emptyInventorySlotsAcceptItems);
		inventoryHandler.onInit();
	}

	private boolean emptyInventorySlotsAcceptItems() {
		return !StorageBlockItem.isLocked(storageStack) || allowsEmptySlotsMatchingItemInsertsWhenLocked();
	}

	private boolean allowsEmptySlotsMatchingItemInsertsWhenLocked() {
		return !isLimitedBarrel(storageStack);
	}

	public int getNumberOfInventorySlots() {
		Integer numberOfInventorySlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS);
		if (numberOfInventorySlots != null) {
			return numberOfInventorySlots;
		}
		numberOfInventorySlots = getDefaultNumberOfInventorySlots(storageStack);
		storageStack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
		stackChangeHandler.run();

		return numberOfInventorySlots;
	}

	@Override
	public ITrackedContentsItemResourceHandler getInventoryForInputOutput() {
		if (isLocked() && allowsEmptySlotsMatchingItemInsertsWhenLocked()) {
			if (contentsFilteredItemHandler == null) {
				contentsFilteredItemHandler = new ContentsFilteredItemHandler(this::getInventoryIOHandler, () -> getInventoryHandler().getSlotTracker(), () -> getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
			}
			return contentsFilteredItemHandler;
		}
		return getInventoryIOHandler();
	}

	private ITrackedContentsItemResourceHandler getInventoryIOHandler() {
		if (inventoryIOHandler == null) {
			inventoryIOHandler = new InventoryIOHandler(this);
		}
		return inventoryIOHandler.getFilteredItemHandler();
	}

	@Override
	public SettingsHandler getSettingsHandler() {
		if (settingsHandler == null) {
			if (getContentsUuid().isPresent()) {
				settingsHandler = new StorageSettingsHandler(getContents().settings(), contentsChangeHandler, this::getInventoryHandler, () -> renderDataHandler) {
					@Override
					protected int getNumberOfDisplayItems() {
						return MovingStorageWrapper.getNumberOfDisplayItems(storageStack);
					}
				};
			} else {
				settingsHandler = NoopStorageWrapper.INSTANCE.getSettingsHandler();
			}
		}
		return settingsHandler;
	}

	@Override
	public UpgradeHandler getUpgradeHandler() {
		if (upgradeHandler == null) {
			upgradeHandler = new UpgradeHandler(getNumberOfUpgradeSlots(), this, getContents(), contentsChangeHandler, () -> {
				if (inventoryHandler != null) {
					inventoryHandler.clearListeners();
					inventoryHandler.setBaseSlotLimit(StackUpgradeItem.getInventorySlotLimit(this));
				}
				getInventoryHandler().addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
				inventoryIOHandler = null;
				getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged(); //in case stack upgrade changed need to send updated fill ratios to client
			}) {
				@Override
				public boolean isValid(int index, ItemResource resource) {
					return super.isValid(index, resource) && (resource.isEmpty() || resource.is(ModItems.STORAGE_UPGRADE_TAG));
				}
			};
			upgradeDefaultsHandlers.forEach(this::registerUpgradeDefaultsHandlerInUpgradeHandler);
		}
		return upgradeHandler;
	}

	private <T extends IUpgradeWrapper> void registerUpgradeDefaultsHandlerInUpgradeHandler(Class<T> wrapperClass, Consumer<? extends IUpgradeWrapper> defaultsHandler) {
		//noinspection DataFlowIssue, unchecked - only called after upgradeHandler is initialized
		upgradeHandler.registerUpgradeDefaultsHandler(wrapperClass, (Consumer<T>) defaultsHandler);
	}

	public int getNumberOfUpgradeSlots() {
		@Nullable Integer numberOfUpgradeSlots = storageStack.get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS);
		if (numberOfUpgradeSlots != null) {
			return numberOfUpgradeSlots;
		}
		numberOfUpgradeSlots = getDefaultNumberOfUpgradeSlots(storageStack);
		storageStack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
		stackChangeHandler.run();

		return numberOfUpgradeSlots;
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.ofNullable(getContentsUuid(storageStack));
	}

	@Nullable
	private static UUID getContentsUuid(ItemStack storageStack) {
		return storageStack.get(ModCoreDataComponents.STORAGE_UUID);
	}

	public static boolean hasContentsUuid(ItemStack storageStack) {
		return getContentsUuid(storageStack) != null;
	}

	private ContainerContents getContents() {
		UUID storageId = getContentsUuid().orElseGet(this::getNewUuid);
		IStorageSavedData storageData = getStorageData.get();
		return storageData.getContents(storageId);
	}

	@Override
	public int getMainColor() {
		return StorageBlockItem.getMainColorFromComponentHolder(storageStack).orElse(-1);
	}

	@Override
	public int getAccentColor() {
		return StorageBlockItem.getAccentColorFromComponentHolder(storageStack).orElse(-1);
	}

	@Override
	public Optional<Integer> getOpenTabId() {
		return Optional.ofNullable(storageStack.get(ModCoreDataComponents.OPEN_TAB_ID));
	}

	@Override
	public void setOpenTabId(int openTabId) {
		storageStack.set(ModCoreDataComponents.OPEN_TAB_ID, openTabId);
		stackChangeHandler.run();
	}

	@Override
	public void removeOpenTabId() {
		storageStack.remove(ModCoreDataComponents.OPEN_TAB_ID);
		stackChangeHandler.run();
	}

	@Override
	public void setColors(int mainColor, int accentColor) {
		storageStack.set(ModCoreDataComponents.MAIN_COLOR, mainColor);
		storageStack.set(ModCoreDataComponents.ACCENT_COLOR, accentColor);
		stackChangeHandler.run();
	}

	@Override
	public void setSortBy(SortBy sortBy) {
		storageStack.set(ModCoreDataComponents.SORT_BY, sortBy);
		stackChangeHandler.run();
	}

	@Override
	public SortBy getSortBy() {
		return storageStack.getOrDefault(ModCoreDataComponents.SORT_BY, SortBy.NAME);
	}

	@Override
	public void sort() {
		Set<Integer> slotIndexesExcludedFromSort = new HashSet<>();
		slotIndexesExcludedFromSort.addAll(getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class).getNoSortSlots());
		slotIndexesExcludedFromSort.addAll(getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotIndexes());
		slotIndexesExcludedFromSort.addAll(getInventoryHandler().getNoSortSlots());
		InventorySorter.sortHandler(getInventoryHandler(), getComparator(), slotIndexesExcludedFromSort);
	}

	private Comparator<Map.Entry<ItemStackKey, Integer>> getComparator() {
		return switch (getSortBy()) {
			case COUNT -> InventorySorter.BY_COUNT;
			case TAGS -> InventorySorter.BY_TAGS;
			case NAME -> InventorySorter.BY_NAME;
			case MOD -> InventorySorter.BY_MOD;
		};
	}

	@Override
	public void onContentsUpdated() {
		inventoryHandler = null;
		upgradeHandler = null;
		refreshInventoryForUpgradeProcessing();
	}

	@Override
	public void refreshInventoryForUpgradeProcessing() {
		inventoryForUpgradeProcessing = null;
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
	public RenderDataHandler getRenderDataHandler() {
		return renderDataHandler;
	}

	@Override
	public void setColumnsTaken(int columnsTaken, boolean hasChanged) {
		//noop - would require a change if there ever was support for this in storage which is not a plan
	}

	@Override
	public int getColumnsTaken() {
		return 0;
	}

	public void setContentsUuid(UUID contentsUuid) {
		storageStack.set(ModCoreDataComponents.STORAGE_UUID, contentsUuid);
		onContentsUpdated();
	}

	public static int getDefaultNumberOfInventorySlots(ItemStack storageStack) {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfInventorySlots() : 0;
	}

	public static int getDefaultNumberOfUpgradeSlots(ItemStack storageStack) {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfUpgradeSlots() : 0;
	}

	private boolean isAllowedInStorage(ItemResource resource) {
		if (!(storageStack.getItem() instanceof ShulkerBoxItem)) {
			return true;
		}

		Block block = Block.byItem(resource.getItem());
		return !(block instanceof ShulkerBoxBlock) && !(block instanceof net.minecraft.world.level.block.ShulkerBoxBlock) && !Config.SERVER.shulkerBoxDisallowedItems.isItemDisallowed(resource.getItem());
	}

	@Override
	public String getStorageType() {
		Item storageItem = storageStack.getItem();
		if (!(storageItem instanceof BlockItem blockItem)) {
			return "undefined";
		}

		return switch (blockItem.getBlock()) {
			case ChestBlock chestBlock -> ChestBlockEntity.STORAGE_TYPE;
			case ShulkerBoxBlock shulkerBoxBlock -> ShulkerBoxBlockEntity.STORAGE_TYPE;
			case LimitedBarrelBlock limitedBarrelBlock -> LimitedBarrelBlockEntity.STORAGE_TYPE;
			case BarrelBlock barrelBlock -> BarrelBlockEntity.STORAGE_TYPE;
			default -> "undefined";
		};

	}

	@Override
	public Component getDisplayName() {
		return storageStack.getDisplayName();
	}

	public void changeSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		setNumberOfInventorySlots(getNumberOfInventorySlots() + additionalInventorySlots);
		setNumberOfUpgradeSlots(getNumberOfUpgradeSlots() + additionalUpgradeSlots);
		onContentsUpdated();
	}

	public void setNumberOfInventorySlots(int numberOfInventorySlots) {
		storageStack.set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
		stackChangeHandler.run();
	}

	public void setNumberOfUpgradeSlots(int numberOfUpgradeSlots) {
		storageStack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
		stackChangeHandler.run();
	}

	public <T extends IUpgradeWrapper> void registerUpgradeDefaultsHandler(Class<T> upgradeClass, Consumer<T> defaultsHandler) {
		upgradeDefaultsHandlers.put(upgradeClass, defaultsHandler);
	}

	@Override
	public ItemStack getWrappedStorageStack() {
		return storageStack;
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return storageStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getBaseStackSizeMultiplier() : 1;
	}

	public static boolean isLimitedBarrel(ItemStack storageItem) { //TODO better place for this method
		return storageItem.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof LimitedBarrelBlock;
	}

	private class MovingStorageRenderDataHandler extends RenderDataHandler {
		public MovingStorageRenderDataHandler(ItemStack storageStack) {
			super(Optional.ofNullable(storageStack.get(ModCoreDataComponents.RENDER_DATA.get())).map(RenderData::copy).orElseGet(RenderData::new),
					renderData -> {
						storageStack.set(ModCoreDataComponents.RENDER_DATA, renderData.copy());
						stackChangeHandler.run();
					}, isLimitedBarrel(storageStack));
		}
	}
}
