package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.registries.ForgeRegistries;
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
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class StorageWrapper implements IStorageWrapper {
	private static final String MAIN_COLOR_TAG = "mainColor";
	private static final String ACCENT_COLOR_TAG = "accentColor";
	private static final String UUID_TAG = "uuid";
	private static final String OPEN_TAB_ID_TAG = "openTabId";
	public static final String CONTENTS_TAG = "contents";
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

	private Runnable upgradeCachesInvalidatedHandler = () -> {};

	private final Map<Class<? extends IUpgradeWrapper>, Consumer<? extends IUpgradeWrapper>> upgradeDefaultsHandlers = new HashMap<>();

	protected StorageWrapper(Supplier<Runnable> getSaveHandler, Runnable onSerializeRenderInfo, Runnable markContentsDirty) {
		this(getSaveHandler, onSerializeRenderInfo, markContentsDirty, 1);
	}

	protected StorageWrapper(Supplier<Runnable> getSaveHandler, Runnable onSerializeRenderInfo, Runnable markContentsDirty, int numberOfDisplayItems) {
		this.getSaveHandler = getSaveHandler;
		renderInfo = new RenderInfo(getSaveHandler) {
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
				inventoryIOHandler = null;
				upgradeCachesInvalidatedHandler.run();
			}) {
				@Override
				public boolean isItemValid(int slot, ItemStack stack) {
					//noinspection ConstantConditions - by this time the upgrade has registryName so it can't be null
					return super.isItemValid(slot, stack) && (stack.isEmpty() || SophisticatedStorage.MOD_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace()) || stack.is(ModItems.STORAGE_UPGRADE_TAG));
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

	public CompoundTag save(CompoundTag tag) {
		saveContents(tag);
		saveData(tag);
		return tag;
	}

	private void saveContents(CompoundTag tag) {
		tag.put(CONTENTS_TAG, getContentsNbt().copy());
	}

	CompoundTag saveData(CompoundTag tag) {
		if (!settingsNbt.isEmpty()) {
			tag.put("settings", settingsNbt);
		}
		if (!renderInfoNbt.isEmpty()) {
			tag.put("renderInfo", renderInfoNbt);
		}
		if (contentsUuid != null) {
			tag.put(UUID_TAG, NbtUtils.createUUID(contentsUuid));
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
		if (mainColor != 0) {
			tag.putInt(MAIN_COLOR_TAG, mainColor);
		}
		if (accentColor != 0) {
			tag.putInt(ACCENT_COLOR_TAG, accentColor);
		}
		return tag;
	}

	public void load(CompoundTag tag) {
		loadContents(tag);
		loadData(tag);
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER && getRenderInfo().getUpgradeItems().size() != getUpgradeHandler().getSlots()) {
			getUpgradeHandler().setRenderUpgradeItems();
		}
	}

	private void loadData(CompoundTag tag) {
		settingsNbt = tag.getCompound("settings");
		settingsHandler.reloadFrom(settingsNbt);
		renderInfoNbt = tag.getCompound("renderInfo");
		renderInfo.deserializeFrom(renderInfoNbt);
		contentsUuid = NBTHelper.getTagValue(tag, UUID_TAG, CompoundTag::get).map(NbtUtils::loadUUID).orElse(null);
		openTabId = NBTHelper.getInt(tag, OPEN_TAB_ID_TAG).orElse(-1);
		sortBy = NBTHelper.getString(tag, "sortBy").map(SortBy::fromName).orElse(SortBy.NAME);
		columnsTaken = NBTHelper.getInt(tag, "columnsTaken").orElse(0);
		loadSlotNumbers(tag);
		mainColor = NBTHelper.getInt(tag, MAIN_COLOR_TAG).orElse(-1);
		accentColor = NBTHelper.getInt(tag, ACCENT_COLOR_TAG).orElse(-1);
	}

	protected void loadSlotNumbers(CompoundTag tag) {
		numberOfInventorySlots = NBTHelper.getInt(tag, "numberOfInventorySlots").orElse(0);
		numberOfUpgradeSlots = NBTHelper.getInt(tag, "numberOfUpgradeSlots").orElse(-1);
	}

	private void loadContents(CompoundTag tag) {
		if (tag.contains(CONTENTS_TAG)) {
			contentsNbt = tag.getCompound(CONTENTS_TAG);
			onContentsNbtUpdated();
		}
	}

	@Override
	public void setSaveHandler(Runnable saveHandler) {
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
		if (numberOfUpgradeSlots > -1) {
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
		return mainColor > -1;
	}

	public void setMainColor(int mainColor) {
		this.mainColor = mainColor;
	}

	@Override
	public int getAccentColor() {
		return accentColor;
	}

	public boolean hasAccentColor() {
		return accentColor > -1;
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

	public void increaseSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		if (additionalInventorySlots > 0) {
			numberOfInventorySlots += additionalInventorySlots;
			getInventoryHandler().changeSlots(additionalInventorySlots);
		}

		if (additionalUpgradeSlots > 0) {
			numberOfUpgradeSlots += additionalUpgradeSlots;
			getUpgradeHandler().increaseSize(additionalUpgradeSlots);
		}
	}

	public <T extends IUpgradeWrapper> void registerUpgradeDefaultsHandler(Class<T> upgradeClass, Consumer<T> defaultsHandler) {
		upgradeDefaultsHandlers.put(upgradeClass, defaultsHandler);
	}
}
