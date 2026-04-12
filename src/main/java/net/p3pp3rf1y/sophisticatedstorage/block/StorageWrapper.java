package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryIOHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class StorageWrapper implements IStorageWrapper, ValueIOSerializable {
	public static final String MAIN_COLOR = "mainColor";
	public static final String ACCENT_COLOR = "accentColor";
	private static final String UUID = "uuid";
	private static final String OPEN_TAB_ID = "openTabId";
	public static final String CONTENTS_TAG = "contents";
	public static final String NUMBER_OF_INVENTORY_SLOTS = "numberOfInventorySlots";
	public static final String NUMBER_OF_UPGRADE_SLOTS = "numberOfUpgradeSlots";
	public static final String RENDER_INFO_TAG = "renderInfo";
	public static final String SORT_BY = "sortBy";
	private final Supplier<Runnable> getSaveHandler;

	@Nullable
	private InventoryHandler inventoryHandler = null;
	@Nullable
	private InventoryIOHandler inventoryIOHandler = null;
	@Nullable
	private UpgradeHandler upgradeHandler = null;
	private CompoundTag contentsNbt = new CompoundTag();
	private CompoundTag settingsNbt = new CompoundTag();
	private final SettingsHandler settingsHandler;
	private final RenderInfo renderInfo;
	private boolean renderInfoValidationPending = true;

	private CompoundTag renderInfoNbt = new CompoundTag();

	@Nullable
	protected UUID contentsUuid = null;

	private int openTabId = -1;

	protected int numberOfInventorySlots = 0;
	protected int numberOfUpgradeSlots = -1;

	private SortBy sortBy = SortBy.NAME;
	private int columnsTaken = 0;
	private int mainColor = -1;
	private int accentColor = -1;
	private Runnable upgradeCachesInvalidatedHandler = () -> {
	};

	private final Map<Class<? extends IUpgradeWrapper>, Consumer<? extends IUpgradeWrapper>> upgradeDefaultsHandlers = new HashMap<>();
	private Runnable onInventoryForInputOutputHandlerRefresh = () -> {
	};

	protected StorageWrapper(Supplier<Runnable> getSaveHandler, Runnable onSerializeRenderInfo, Runnable markContentsDirty) {
		this(getSaveHandler, onSerializeRenderInfo, markContentsDirty, 1, false);
	}

	protected StorageWrapper(Supplier<Runnable> getSaveHandler, Runnable onSerializeRenderInfo, Runnable markContentsDirty, int numberOfDisplayItems, boolean showsCountsAndFillRatios) {
		this.getSaveHandler = getSaveHandler;
		renderInfo = new RenderInfo(getSaveHandler, showsCountsAndFillRatios) {
			@Override
			protected void serializeRenderInfo(CompoundTag renderInfo) {
				renderInfoNbt = renderInfo;
				onSerializeRenderInfo.run();
			}

			@Override
			protected Optional<CompoundTag> getRenderInfoTag() {
				return Optional.of(renderInfoNbt);
			}
		};
		settingsHandler = new StorageSettingsHandler(settingsNbt, markContentsDirty, this::getInventoryHandler, () -> renderInfo) {

			@Override
			protected int getNumberOfDisplayItems() {
				return numberOfDisplayItems;
			}
		};
	}

	public void setContentsUuid(@Nullable UUID contentsUuid) {
		this.contentsUuid = contentsUuid;
	}

	@Override
	public SettingsHandler getSettingsHandler() {
		return settingsHandler;
	}

	@Override
	public UpgradeHandler getUpgradeHandler() {
		if (upgradeHandler == null) {
			upgradeHandler = new UpgradeHandler(getNumberOfUpgradeSlots(), this, getContentsNbt(), getSaveHandler.get(), () -> {
				if (inventoryHandler != null) {
					inventoryHandler.clearListeners();
					inventoryHandler.setBaseSlotLimit(StackUpgradeItem.getInventorySlotLimit(this));
				}
				getInventoryHandler().addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
				refreshInventoryForInputOutput();
				getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged(); //in case stack upgrade changed need to send updated fill ratios to client
			}) {
				@Override
				public boolean isItemValid(int slot, ItemStack stack) {
					return super.isItemValid(slot, stack) && (stack.isEmpty() || stack.is(ModItems.STORAGE_UPGRADE_TAG));
				}

				@Override
				public void refreshUpgradeWrappers() {
					super.refreshUpgradeWrappers();
					onUpgradeRefresh();
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

	@Override
	public void setUpgradeCachesInvalidatedHandler(Runnable handler) {
		upgradeCachesInvalidatedHandler = handler;
	}

	protected abstract void onUpgradeRefresh();

	@Override
	public void serialize(ValueOutput out) {
		saveContents(out);
		saveData(out);
	}

	private void saveContents(ValueOutput out) {
		out.store(CONTENTS_TAG, CompoundTag.CODEC, getContentsNbt().copy());
	}

	void saveData(ValueOutput out) {
		if (!settingsNbt.isEmpty()) {
			out.store(SETTINGS_TAG, CompoundTag.CODEC, settingsNbt);
		}
		if (!renderInfoNbt.isEmpty()) {
			out.store(RENDER_INFO_TAG, CompoundTag.CODEC, renderInfoNbt);
		}
		if (contentsUuid != null) {
			out.store(UUID, UUIDUtil.CODEC, contentsUuid);
		}
		if (openTabId >= 0) {
			out.putInt(OPEN_TAB_ID, openTabId);
		}
		out.putString(SORT_BY, sortBy.getSerializedName());
		if (columnsTaken > 0) {
			out.putInt("columnsTaken", columnsTaken);
		}
		if (numberOfInventorySlots > 0) {
			out.putInt(NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
		}
		if (numberOfUpgradeSlots > -1) {
			out.putInt(NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
		}
		if (mainColor != -1) {
			out.putInt(MAIN_COLOR, mainColor);
		}
		if (accentColor != -1) {
			out.putInt(ACCENT_COLOR, accentColor);
		}
	}

	@Override
	public void deserialize(ValueInput in) {
		loadContents(in);
		loadData(in);

		if (inventoryHandler != null) {
			initInventoryHandler();
		}
		if (upgradeHandler != null) {
			getUpgradeHandler().refreshUpgradeWrappers();
		}
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER && getRenderInfo().getUpgradeItems().size() != getUpgradeHandler().getSlots()) {
			getUpgradeHandler().setRenderUpgradeItems();
		}

	}

	private void loadData(ValueInput in) {
		settingsNbt = in.read(SETTINGS_TAG, CompoundTag.CODEC).orElse(new CompoundTag());
		settingsHandler.reloadFrom(settingsNbt);
		renderInfoNbt = in.read(RENDER_INFO_TAG, CompoundTag.CODEC).orElse(new CompoundTag());
		renderInfo.deserializeFrom(renderInfoNbt);
		renderInfoValidationPending = true;
		contentsUuid = in.read(UUID, UUIDUtil.CODEC).orElse(null);
		openTabId = in.getIntOr(OPEN_TAB_ID, -1);
		sortBy = in.read(SORT_BY, SortBy.CODEC).orElse(SortBy.NAME);
		columnsTaken = in.getIntOr("columnsTaken", 0);
		loadSlotNumbers(in);
		mainColor = in.getIntOr(MAIN_COLOR, -1);
		accentColor = in.getIntOr(ACCENT_COLOR, -1);
	}

	@Override
	public void onInit(Level level) {
		IStorageWrapper.super.onInit(level);
		if (renderInfoValidationPending && !level.isClientSide()) {
			getRenderInfo().validate(this, level);
			renderInfoValidationPending = false;
		}
	}

	protected void loadSlotNumbers(ValueInput in) {
		numberOfInventorySlots = in.getIntOr(NUMBER_OF_INVENTORY_SLOTS, 0);
		numberOfUpgradeSlots = in.getIntOr(NUMBER_OF_UPGRADE_SLOTS, -1);
	}

	private void loadContents(ValueInput in) {
		in.read(CONTENTS_TAG, CompoundTag.CODEC)
				.ifPresent(contents -> {
					contentsNbt = contents;
					onContentsNbtUpdated();
				});
	}

	@Override
	public void setContentsChangeHandler(Runnable contentsChangeHandler) {
		//noop
	}

	@Override
	public ITrackedContentsItemHandler getInventoryForUpgradeProcessing() {
		return getInventoryHandler();
	}

	@Override
	public InventoryHandler getInventoryHandler() {
		if (inventoryHandler == null) {
			initInventoryHandler();
		}
		return inventoryHandler;
	}

	private void initInventoryHandler() {
		inventoryHandler = new InventoryHandler(getNumberOfInventorySlots(), this, getContentsNbt(), getSaveHandler.get(), StackUpgradeItem.getInventorySlotLimit(this), Config.SERVER.stackUpgrade) {
			@Override
			protected boolean isAllowed(ItemStack stack) {
				return isAllowedInStorage(stack);
			}
		};
		inventoryHandler.addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
		inventoryHandler.setShouldInsertIntoEmpty(this::emptyInventorySlotsAcceptItems);
	}

	protected boolean emptyInventorySlotsAcceptItems() {
		return true;
	}

	protected CompoundTag getContentsNbt() {
		return contentsNbt;
	}

	public int getNumberOfInventorySlots() {
		if (numberOfInventorySlots > 0) {
			return numberOfInventorySlots;
		}
		numberOfInventorySlots = getDefaultNumberOfInventorySlots();
		save();

		return numberOfInventorySlots;
	}

	protected void save() {
		getSaveHandler.get().run();
	}

	public abstract int getDefaultNumberOfInventorySlots();

	protected abstract boolean isAllowedInStorage(ItemStack stack);

	@Override
	public int getNumberOfSlotRows() {
		int itemInventorySlots = getNumberOfInventorySlots();
		return (int) Math.ceil(itemInventorySlots <= 81 ? (double) itemInventorySlots / 9 : (double) itemInventorySlots / 12);
	}

	@Override
	public ITrackedContentsItemHandler getInventoryForInputOutput() {
		if (inventoryIOHandler == null) {
			inventoryIOHandler = new InventoryIOHandler(this);
		}
		return inventoryIOHandler.getFilteredItemHandler();
	}

	private int getNumberOfUpgradeSlots() {
		if (numberOfUpgradeSlots >= getDefaultNumberOfUpgradeSlots()) {
			return numberOfUpgradeSlots;
		}
		numberOfUpgradeSlots = getDefaultNumberOfUpgradeSlots();
		save();

		return numberOfUpgradeSlots;
	}

	public abstract int getDefaultNumberOfUpgradeSlots();

	@Override
	public int getMainColor() {
		return mainColor;
	}

	public boolean hasMainColor() {
		return mainColor != -1;
	}

	@Override
	public int getAccentColor() {
		return accentColor;
	}

	public boolean hasAccentColor() {
		return accentColor != -1;
	}

	@Override
	public Optional<Integer> getOpenTabId() {
		return openTabId >= 0 ? Optional.of(openTabId) : Optional.empty();
	}

	@Override
	public void setOpenTabId(int openTabId) {
		this.openTabId = openTabId;
		save();
	}

	@Override
	public void removeOpenTabId() {
		openTabId = -1;
		save();
	}

	@Override
	public void setColors(int mainColor, int accentColor) {
		this.mainColor = mainColor;
		this.accentColor = accentColor;
		save();
	}

	@Override
	public void setSortBy(SortBy sortBy) {
		this.sortBy = sortBy;
		save();
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
		upgradeCachesInvalidatedHandler.run();
		onInventoryForInputOutputHandlerRefresh.run();
	}

	@Override
	public void registerOnInventoryInputOutputHandlerRefreshListener(Runnable onInventoryForInputOutputHandlerRefresh) {
		this.onInventoryForInputOutputHandlerRefresh = onInventoryForInputOutputHandlerRefresh;
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
	public void setColumnsTaken(int columnsTaken, boolean hasChanged) {
		this.columnsTaken = columnsTaken;
		save();
	}

	@Override
	public int getColumnsTaken() {
		return columnsTaken;
	}

	public void changeSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		numberOfInventorySlots += additionalInventorySlots;
		getInventoryHandler().changeSlots(additionalInventorySlots);

		numberOfUpgradeSlots += additionalUpgradeSlots;
		getUpgradeHandler().increaseSize(additionalUpgradeSlots);
	}

	public <T extends IUpgradeWrapper> void registerUpgradeDefaultsHandler(Class<T> upgradeClass, Consumer<T> defaultsHandler) {
		upgradeDefaultsHandlers.put(upgradeClass, defaultsHandler);
	}
}
