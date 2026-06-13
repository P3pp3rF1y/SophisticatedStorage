package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
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
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class StorageWrapper implements IStorageWrapper, ValueIOSerializable {
	public static final String MAIN_COLOR = "mainColor";
	public static final String ACCENT_COLOR = "accentColor";
	private static final String UUID = "uuid";
	private static final String OPEN_TAB_ID = "openTabId";
	public static final String CONTENTS = "contents";
	public static final String NUMBER_OF_INVENTORY_SLOTS = "numberOfInventorySlots";
	public static final String NUMBER_OF_UPGRADE_SLOTS = "numberOfUpgradeSlots";
	public static final String RENDER_DATA = "renderData";
	public static final String SORT_BY = "sortBy";
	private final Supplier<Runnable> getSaveHandler;

	@Nullable
	private InventoryHandler inventoryHandler = null;
	@Nullable
	private InventoryIOHandler inventoryIOHandler = null;
	@Nullable
	private UpgradeHandler upgradeHandler = null;
	private boolean upgradeHandlerInitializing = false;
	private ContainerContents contents = new ContainerContents();
	private final SettingsHandler settingsHandler;
	private final RenderDataHandler renderDataHandler;
	private boolean renderDataValidationPending = true;

	private RenderData renderData = new RenderData();

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

	protected StorageWrapper(Supplier<Runnable> getSaveHandler, Runnable onSerializeRenderData, Runnable markContentsDirty) {
		this(getSaveHandler, onSerializeRenderData, markContentsDirty, 1, false);
	}

	protected StorageWrapper(Supplier<Runnable> getSaveHandler, Runnable onSerializeRenderData, Runnable markContentsDirty, int numberOfDisplayItems, boolean showsCountsAndFillRatios) {
		this.getSaveHandler = getSaveHandler;
		renderDataHandler = new RenderDataHandler(renderData,
				renderData -> {
					this.renderData = renderData;
					onSerializeRenderData.run();
					getSaveHandler.get().run();
				}, showsCountsAndFillRatios);
		settingsHandler = new StorageSettingsHandler(contents.settings(), markContentsDirty, this::getInventoryHandler, () -> renderDataHandler) {

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
		UpgradeHandler handler = upgradeHandler;
		if (handler == null) {
			upgradeHandlerInitializing = true;
			handler = new UpgradeHandler(getNumberOfUpgradeSlots(), this, getContents(), getSaveHandler.get(), () -> {
				if (inventoryHandler != null) {
					inventoryHandler.clearListeners();
					inventoryHandler.setBaseSlotLimit(StackUpgradeItem.getInventorySlotLimit(this));
				}
				getInventoryHandler().addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
				refreshInventoryForInputOutput();
				getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged(); //in case stack upgrade changed need to send updated fill ratios to client
			}) {
				@Override
				public boolean isValid(int index, ItemResource resource) {
					return super.isValid(index, resource) && (resource.isEmpty() || resource.is(ModItems.STORAGE_UPGRADE_TAG));
				}

				@Override
				public void refreshUpgradeWrappers() {
					super.refreshUpgradeWrappers();
					onUpgradeRefresh();
				}

			};
			upgradeHandler = handler;
			upgradeDefaultsHandlers.forEach(this::registerUpgradeDefaultsHandlerInUpgradeHandler);
			upgradeHandlerInitializing = false;
		}
		return handler;
	}

	public boolean isUpgradeHandlerInitializing() {
		return upgradeHandlerInitializing;
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
		out.store(CONTENTS, ContainerContents.CODEC, getContents().copy());
	}

	void saveClientData(ValueOutput out) {
		out.store(SETTINGS, ContainerContents.SettingsData.CODEC, contents.settings());
		saveData(out);
	}

	void saveData(ValueOutput out) {
		out.store(RENDER_DATA, RenderData.CODEC, renderData);
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
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER && getRenderDataHandler().getUpgradeItems().size() != getUpgradeHandler().size()) {
			getUpgradeHandler().setRenderUpgradeItems();
		}
	}

	public void loadClientData(ValueInput in) {
		in.read(SETTINGS, ContainerContents.SettingsData.CODEC).ifPresent(contents.settings()::reloadFrom);
		settingsHandler.reloadFrom(contents.settings());
		loadData(in);

		if (inventoryHandler != null) {
			initInventoryHandler();
		}
		if (upgradeHandler != null) {
			getUpgradeHandler().refreshUpgradeWrappers();
		}
	}

	private void loadData(ValueInput in) {
		renderData = in.read(RENDER_DATA, RenderData.CODEC)
				.or(() -> in.read("renderInfo", RenderData.CODEC)) //TODO remove legacy deserialization likely after major 1.22 release
				.orElse(RenderData.EMPTY.copy());
		renderDataHandler.reloadFrom(renderData);
		renderDataValidationPending = true;
		contentsUuid = in.read(UUID, UUIDUtil.CODEC).orElse(null);
		openTabId = in.getIntOr(OPEN_TAB_ID, -1);
		sortBy = in.read(SORT_BY, SortBy.CODEC).orElse(SortBy.NAME);
		columnsTaken = in.getIntOr("columnsTaken", 0);
		loadSlotNumbers(in);
		mainColor = in.getIntOr(MAIN_COLOR, -1);
		accentColor = in.getIntOr(ACCENT_COLOR, -1);
	}

	protected void loadSlotNumbers(ValueInput in) {
		numberOfInventorySlots = in.getIntOr(NUMBER_OF_INVENTORY_SLOTS, 0);
		numberOfUpgradeSlots = in.getIntOr(NUMBER_OF_UPGRADE_SLOTS, -1);
		if (promoteSlotNumbersToDefaults()) {
			save();
		}
	}

	protected boolean promoteSlotNumbersToDefaults() {
		boolean changed = false;
		int defaultNumberOfInventorySlots = getDefaultNumberOfInventorySlots();
		if (numberOfInventorySlots < defaultNumberOfInventorySlots) {
			numberOfInventorySlots = defaultNumberOfInventorySlots;
			changed = true;
		}

		int defaultNumberOfUpgradeSlots = getDefaultNumberOfUpgradeSlots();
		if (numberOfUpgradeSlots < defaultNumberOfUpgradeSlots) {
			numberOfUpgradeSlots = defaultNumberOfUpgradeSlots;
			changed = true;
		}
		return changed;
	}

	@Override
	public void onInit(Level level) {
		IStorageWrapper.super.onInit(level);
		if (renderDataValidationPending && !level.isClientSide()) {
			getRenderDataHandler().validate(this, level);
			renderDataValidationPending = false;
		}
	}

	private void loadContents(ValueInput in) {
		readContainerContents(in).ifPresent(c -> {
			contents = c;
			onContentsUpdated();
		});
	}

	private Optional<ContainerContents> readContainerContents(ValueInput in) {
		return in.read(CONTENTS, ContainerContents.CODEC).map(contents -> {
			in.read(SETTINGS, CompoundTag.CODEC).ifPresent(settingsTag -> { // TODO remove legacy deserialization likely after major 1.22 release
				CompoundTag settingsNbt = in.read(SETTINGS, CompoundTag.CODEC).orElse(new CompoundTag());
				contents.settings().reloadFrom(ContainerContents.LegacyDeserialization.deserializeSettingsData(settingsNbt));
			});
			return contents;
		});
	}

	@Override
	public void setContentsChangeHandler(Runnable contentsChangeHandler) {
		//noop
	}

	@Override
	public ITrackedContentsItemResourceHandler getInventoryForUpgradeProcessing() {
		return getInventoryHandler();
	}

	@Override
	public InventoryHandler getInventoryHandler() {
		InventoryHandler handler = inventoryHandler;
		if (handler == null) {
			handler = initInventoryHandler();
		}
		return handler;
	}

	private InventoryHandler initInventoryHandler() {
		InventoryHandler handler = new InventoryHandler(getNumberOfInventorySlots(), this, getContents(), getSaveHandler.get(), StackUpgradeItem.getInventorySlotLimit(this), Config.SERVER.stackUpgrade) {
			@Override
			protected boolean isAllowed(ItemResource resource) {
				return isAllowedInStorage(resource);
			}
		};
		inventoryHandler = handler;
		handler.addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
		handler.setShouldInsertIntoEmpty(this::emptyInventorySlotsAcceptItems);
		return handler;
	}

	protected boolean emptyInventorySlotsAcceptItems() {
		return true;
	}

	public ContainerContents getContents() {
		return contents;
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

	protected abstract boolean isAllowedInStorage(ItemResource resource);

	@Override
	public int getNumberOfSlotRows() {
		int itemInventorySlots = getNumberOfInventorySlots();
		return (int) Math.ceil(itemInventorySlots <= 81 ? (double) itemInventorySlots / 9 : (double) itemInventorySlots / 12);
	}

	@Override
	public ITrackedContentsItemResourceHandler getInventoryForInputOutput() {
		if (inventoryIOHandler == null) {
			inventoryIOHandler = new InventoryIOHandler(this);
		}
		return inventoryIOHandler.getFilteredItemHandler();
	}

	private int getNumberOfUpgradeSlots() {
		if (numberOfUpgradeSlots >= 0) {
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
	public void onContentsUpdated() {
		inventoryHandler = null;
		upgradeHandler = null;
		settingsHandler.reloadFrom(contents.settings());
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
	public RenderDataHandler getRenderDataHandler() {
		return renderDataHandler;
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
		numberOfUpgradeSlots += additionalUpgradeSlots;
		save();
		onContentsUpdated();
	}

	public <T extends IUpgradeWrapper> void registerUpgradeDefaultsHandler(Class<T> upgradeClass, Consumer<T> defaultsHandler) {
		upgradeDefaultsHandlers.put(upgradeClass, defaultsHandler);
	}
}
