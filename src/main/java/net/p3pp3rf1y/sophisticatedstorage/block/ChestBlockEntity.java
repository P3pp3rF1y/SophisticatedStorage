package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.INeighborChangeListenerUpgrade;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ChestBlockEntity extends WoodStorageBlockEntity {
	public static final String STORAGE_TYPE = "chest";
	public static final String DOUBLE_CHEST_MAIN_POS_TAG = "doubleMainPos";

	private final ChestLidController chestLidController = new ChestLidController();

	@Nullable
	private BlockPos doubleMainPos = null;
	public boolean showUpgradesOnTop = false;
	private final SophisticatedOpenersCounter openersCounter = new SophisticatedOpenersCounter() {
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			if (state.getValue(ChestBlock.TYPE) != ChestType.LEFT) {
				playSound(state, SoundEvents.CHEST_OPEN);
			}
		}

		protected void onClose(Level level, BlockPos pos, BlockState state) {
			if (state.getValue(ChestBlock.TYPE) != ChestType.LEFT) {
				playSound(state, SoundEvents.CHEST_CLOSE);
			}
		}

		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousOpenCount, int openCount) {
			chestLidController.shouldBeOpen(openCount > 0);
		}

		protected boolean isOwnContainer(Player player) {
			if (player.containerMenu instanceof StorageContainerMenu storageContainerMenu) {
				return storageContainerMenu.getStorageBlockEntity() == getMainChestBlockEntity();
			} else {
				return false;
			}
		}

		@Override
		public void incrementOpeners(Player player, Level level, BlockPos pos, BlockState state) {
			super.incrementOpeners(player, level, pos, state);
			if (isMainChest()) {
				runOnTheOtherPart(level, pos, (blockEntity, neighborPos) -> blockEntity.openersCounter.incrementOpeners(player, level, neighborPos, level.getBlockState(neighborPos)));
			}
		}

		@Override
		public void decrementOpeners(Player player, Level level, BlockPos pos, BlockState state) {
			super.decrementOpeners(player, level, pos, state);
			if (isMainChest()) {
				runOnTheOtherPart(level, pos, (blockEntity, neighborPos) -> blockEntity.openersCounter.decrementOpeners(player, level, neighborPos, level.getBlockState(neighborPos)));
			}
		}
	};

	private boolean isDestroyedByPlayer = false;
	public void joinWithChest(ChestBlockEntity mainBE) {
		expandAndMoveItemsAndSettings(mainBE);
		removeFromController();
		setNotLinked();
		tryToAddToController();
		invalidateCaps();
	}

	public ChestLidController getChestLidController() {
		return chestLidController;
	}

	private void expandAndMoveItemsAndSettings(ChestBlockEntity mainBE) {
		InventoryHandler mainInventoryHandler = mainBE.getStorageWrapper().getInventoryHandler();
		int originalNumberOfSlots = mainInventoryHandler.getSlots();
		InventoryHandler thisInventoryHandler = getStorageWrapper().getInventoryHandler();
		int inventorySlotDiff = 2 * (mainBE.getBlockState().getBlock() instanceof StorageBlockBase storageBlock ? storageBlock.getNumberOfInventorySlots() : 0) - mainInventoryHandler.getSlots();
		mainBE.changeStorageSize(inventorySlotDiff, 0);

		moveStacksToMain(thisInventoryHandler, mainInventoryHandler, originalNumberOfSlots);

		UpgradeHandler mainUpgradeHandler = mainBE.getStorageWrapper().getUpgradeHandler();
		UpgradeHandler thisUpgradeHandler = getStorageWrapper().getUpgradeHandler();
		moveUpgradesToMain(thisUpgradeHandler, mainUpgradeHandler);

		copySettings(this, mainBE, 0, originalNumberOfSlots);
		deleteSettingsFromSlot(this, 0);
		WorldHelper.notifyBlockUpdate(mainBE);
	}

	private void moveUpgradesToMain(UpgradeHandler thisUpgradeHandler, UpgradeHandler mainUpgradeHandler) {
		for (int slot = 0; slot < thisUpgradeHandler.getSlots(); slot++) {
			ItemStack slotStack = thisUpgradeHandler.getStackInSlot(slot);
			if (!slotStack.isEmpty()) {
				mainUpgradeHandler.setStackInSlot(slot, slotStack);
			}
		}
		for (int slot = 0; slot < thisUpgradeHandler.getSlots(); slot++) {
			thisUpgradeHandler.setStackInSlot(slot, ItemStack.EMPTY);
		}
	}

	private void copySettings(ChestBlockEntity from, ChestBlockEntity to, int startFromSlot, int slotOffset) {
		SettingsHandler mainSettingsHandler = to.getStorageWrapper().getSettingsHandler();
		from.getStorageWrapper().getSettingsHandler().getSettingsCategories().forEach((name, category) ->
				copyCategorySettings(category, mainSettingsHandler.getTypeCategory(category.getClass()), startFromSlot, slotOffset)
		);
	}

	private void deleteSettingsFromSlot(ChestBlockEntity from, int startFromSlot) {
		from.getStorageWrapper().getSettingsHandler().getSettingsCategories().forEach((name, category) ->
				category.deleteSlotSettingsFrom(startFromSlot)
		);
	}

	private <T extends ISettingsCategory<?>> void copyCategorySettings(ISettingsCategory<T> category, ISettingsCategory<?> mainCategory, int startFromSlot, int slotOffset) {
		category.copyTo((T) mainCategory, startFromSlot, slotOffset);
	}

	private static void moveStacksToMain(InventoryHandler thisInventoryHandler, InventoryHandler mainInventoryHandler, int originalNumberOfSlots) {
		int thisSlots = thisInventoryHandler.getSlots();
		int mainSlots = mainInventoryHandler.getSlots();
		for (int slot = 0; slot < thisSlots && slot + originalNumberOfSlots < mainSlots; slot++) {
			ItemStack slotStack = thisInventoryHandler.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				mainInventoryHandler.setStackInSlot(slot + originalNumberOfSlots, slotStack);
			}
		}

		for (int slot = 0; slot < thisSlots; slot++) {
			thisInventoryHandler.setStackInSlot(slot, ItemStack.EMPTY);
		}
	}

	public void syncTogglesFrom(ChestBlockEntity chestBE) {
		if (chestBE.isLocked() != isLocked()) {
			toggleJustMyLock();
		}
		if (chestBE.shouldShowLock() != shouldShowLock()) {
			toggleJustMyLockVisibility();
		}
		if (chestBE.shouldShowTier() != shouldShowTier()) {
			toggleJustMyTierVisiblity();
		}
		if (chestBE.shouldShowUpgrades() != shouldShowUpgrades()) {
			toggleJustMyUpgradesVisiblity();
		}
	}

	@Override
	public void dropContents() {
		if (isDestroyedByPlayer && getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			if (!isMainChest()) {
				moveMyStacksFromMain();
			} else {
				moveOtherPartStacksToIt();
			}
		}
		super.dropContents();
	}

	@Override
	public void onNeighborChange(BlockPos neighborPos) {
		Direction direction = getNeighborDirection(neighborPos);
		if (direction == null) {
			return;
		}
		getMainStorageWrapper().getUpgradeHandler().getWrappersThatImplement(INeighborChangeListenerUpgrade.class).forEach(upgrade -> upgrade.onNeighborChange(level, worldPosition, direction));
	}

	private void moveOtherPartStacksToIt() {
		runOnTheOtherPart(level, getBlockPos(), (be, pos) -> {
			InventoryHandler mainInventoryHandler = getStorageWrapper().getInventoryHandler();
			int firstIndex = mainInventoryHandler.getSlots() / 2;

			for (int slot = firstIndex; slot < mainInventoryHandler.getSlots(); slot++) {
				ItemStack slotStack = mainInventoryHandler.getSlotStack(slot);
				be.getStorageWrapper().getInventoryHandler().setSlotStack(slot - firstIndex, slotStack.split(slotStack.getMaxStackSize()));
			}

			copySettings(this, be, firstIndex, -firstIndex);
			be.removeControllerPos();
			be.tryToAddToController();
			be.getStorageWrapper().getUpgradeHandler().refreshUpgradeWrappers();
			WorldHelper.notifyBlockUpdate(be);
		});
	}

	private void moveMyStacksFromMain() {
		BlockPos mainPos = getMainPos();
		if (mainPos.equals(worldPosition)) {
			return;
		}

		level.getBlockEntity(mainPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(mainBE -> {
			StorageWrapper mainStorageWrapper = mainBE.getStorageWrapper();
			InventoryHandler mainInventoryHandler = mainStorageWrapper.getInventoryHandler();
			int firstIndex = mainInventoryHandler.getSlots() / 2;

			for (int slot = firstIndex; slot < mainInventoryHandler.getSlots(); slot++) {
				getStorageWrapper().getInventoryHandler().setSlotStack(slot - firstIndex, mainInventoryHandler.getSlotStack(slot));
				mainInventoryHandler.setSlotStack(slot, ItemStack.EMPTY);
			}
			int inventorySlotDiff = (mainBE.getBlockState().getBlock() instanceof StorageBlockBase storageBlock ? storageBlock.getNumberOfInventorySlots() : 0) - mainInventoryHandler.getSlots();

			mainBE.changeStorageSize(inventorySlotDiff, 0);
			deleteSettingsFromSlot(mainBE, firstIndex);
			mainStorageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).setDisplaySide(DisplaySide.FRONT);
			mainStorageWrapper.getUpgradeHandler().refreshUpgradeWrappers();
			WorldHelper.notifyBlockUpdate(mainBE);
		});
	}

	@Override
	public boolean hasStorageData() {
		return isMainChest();
	}

	@Override
	public BlockPos getControlledStorageBlockPos() {
		return getMainPos();
	}

	@Override
	public SophisticatedOpenersCounter getOpenersCounter() {
		return openersCounter;
	}

	@Override
	protected String getStorageType() {
		return STORAGE_TYPE;
	}

	public ChestBlockEntity(BlockPos pos, BlockState state) {
		super(pos, state, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get());
	}

	public static void lidAnimateTick(ChestBlockEntity chestBlockEntity) {
		chestBlockEntity.chestLidController.tickLid();
	}

	public float getOpenNess(float partialTicks) {
		return chestLidController.getOpenness(partialTicks);
	}

	@Override
	public void toggleLock() {
		super.toggleLock();
		if (level != null) {
			runOnTheOtherPart(level, worldPosition, (be, pos) -> be.toggleJustMyLock());
		}
	}

	private void toggleJustMyLock() {
		super.toggleLock();
	}

	@Override
	public void toggleLockVisibility() {
		super.toggleLockVisibility();
		if (level != null) {
			runOnTheOtherPart(level, worldPosition, (be, pos) -> be.toggleJustMyLockVisibility());
		}
	}

	private void toggleJustMyLockVisibility() {
		super.toggleLockVisibility();
	}

	@Override
	public void toggleTierVisiblity() {
		super.toggleTierVisiblity();
		if (level != null) {
			runOnTheOtherPart(level, worldPosition, (be, pos) -> be.toggleJustMyTierVisiblity());
		}
	}

	private void toggleJustMyTierVisiblity() {
		super.toggleTierVisiblity();
	}

	@Override
	public void toggleUpgradesVisiblity() {
		super.toggleUpgradesVisiblity();
		if (level != null) {
			runOnTheOtherPart(level, worldPosition, (be, pos) -> be.toggleJustMyUpgradesVisiblity());
		}
	}

	private void toggleJustMyUpgradesVisiblity() {
		super.toggleUpgradesVisiblity();
	}

	private void runOnTheOtherPart(Level level, BlockPos pos, BiConsumer<ChestBlockEntity, BlockPos> execute) {
		ChestType chestType = getBlockState().getValue(ChestBlock.TYPE);
		if (chestType == ChestType.SINGLE) {
			return;
		}
		Direction facing = getBlockState().getValue(ChestBlock.FACING);
		BlockPos neighborPos = isMainChest() ? pos.relative(facing.getCounterClockWise()) : pos.relative(facing.getClockWise());
		level.getBlockEntity(neighborPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
				.ifPresent(chestBlockEntity -> execute.accept(chestBlockEntity, neighborPos));
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (level == null) {
			return LazyOptional.empty();
		}

		BlockPos mainPos = getMainPos();
		if (!mainPos.equals(worldPosition)) {
			return level.getBlockEntity(mainPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(be -> be.getCapability(cap, side)).orElseGet(LazyOptional::empty);
		}

		return super.getCapability(cap, side);
	}

	public boolean isMainChest() {
		return getMainPos().equals(worldPosition);
	}

	@Override
	public CompoundTag getStorageContentsTag() {
		CompoundTag tag = super.getStorageContentsTag();
		tag.remove(DOUBLE_CHEST_MAIN_POS_TAG);
		return tag;
	}

	@Override
	protected ItemStack addWrappedStorageStackData(ItemStack cloneItemStack, BlockState state) {
		ItemStack ret = super.addWrappedStorageStackData(cloneItemStack, state);
		if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			ChestBlockItem.setDoubleChest(ret, true);
		}
		return ret;
	}

	public StorageWrapper getMainStorageWrapper() {
		if (level != null) {
			BlockPos mainPos = getMainPos();
			if (!mainPos.equals(worldPosition)) {
				return level.getBlockEntity(mainPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).map(StorageBlockEntity::getStorageWrapper).orElseGet(this::getStorageWrapper);
			}
		}
		return getStorageWrapper();
	}

	@Nullable
	public ChestBlockEntity getMainChestBlockEntity() {
		if (level != null) {
			BlockPos mainPos = getMainPos();
			if (!mainPos.equals(worldPosition)) {
				return level.getBlockEntity(mainPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).orElse(null);
			}
		}
		return this;
	}

	public void dropSecondPartContents(ChestBlock chestBlock, BlockPos dropPosition) {
		InventoryHandler invHandler = getStorageWrapper().getInventoryHandler();

		List<ItemStack> dropItems = new ArrayList<>();

		for (int slot = chestBlock.getNumberOfInventorySlots(); slot < invHandler.getSlots(); slot++) {
			ItemStack slotStack = invHandler.getSlotStack(slot);

			if (!slotStack.isEmpty()) {
				dropItems.add(slotStack.copy());
				invHandler.setStackInSlot(slot, ItemStack.EMPTY);
			}
		}

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.getServer().tell(new TickTask(serverLevel.getServer().getTickCount(), () ->
					dropItems.forEach(itemStack -> Containers.dropItemStack(serverLevel, dropPosition.getX(), dropPosition.getY(), dropPosition.getZ(), itemStack)))
			);
		}

		int inventorySlotDiff = chestBlock.getNumberOfInventorySlots() - invHandler.getSlots();

		changeStorageSize(inventorySlotDiff, 0);
		deleteSettingsFromSlot(this, chestBlock.getNumberOfInventorySlots());
		getStorageWrapper().getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).setDisplaySide(DisplaySide.FRONT);
		getStorageWrapper().getUpgradeHandler().refreshUpgradeWrappers();
		WorldHelper.notifyBlockUpdate(this);
	}

	public void setDestroyedByPlayer() {
		isDestroyedByPlayer = true;
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (!isBeingUpgraded() && getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
			if (getBlockState().getBlock() instanceof ChestBlock chestBlock
					&& getStorageWrapper().getInventoryHandler().getSlots() > chestBlock.getNumberOfInventorySlots()) {
				dropSecondPartContents(chestBlock, worldPosition);
			}
		}
	}

	@Override
	public void changeSlots(int newSlots) {
		if (hasStorageData()) {
			super.changeSlots(newSlots);
		}
	}

	@Override
	public void setShouldBeOpen(boolean shouldBeOpen) {
		chestLidController.shouldBeOpen(shouldBeOpen);
		if (level != null) {
			runOnTheOtherPart(level, worldPosition, (be, pos) -> be.chestLidController.shouldBeOpen(shouldBeOpen));
		}
	}

	public BlockPos getMainPos() {
		BlockState state = getBlockState();
		if (state.getValue(ChestBlock.TYPE) != ChestType.LEFT || level == null) {
			return worldPosition;
		}

		BlockPos mainPos = worldPosition.relative(ChestBlock.getConnectedDirection(state));
		BlockState mainState = level.getBlockState(mainPos);
		if (mainState.is(state.getBlock())
				&& mainState.getValue(ChestBlock.TYPE) == ChestType.RIGHT
				&& mainState.getValue(ChestBlock.FACING) == state.getValue(ChestBlock.FACING)
				&& mainPos.relative(ChestBlock.getConnectedDirection(mainState)).equals(worldPosition)) {
			return mainPos;
		}

		return worldPosition;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level != null && getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE && isMainChest()) {
			// need to update neighbors of the other half as well for comparators to pickup inventory changes
			runOnTheOtherPart(level, worldPosition, (be, pos) -> level.updateNeighbourForOutputSignal(pos, be.getBlockState().getBlock()));
		}
	}

	@Override
	public void linkToController(BlockPos controllerPos) {
		if (level != null) {
			BlockPos mainPos = getMainPos();
			if (!mainPos.equals(worldPosition)) {
				level.getBlockEntity(mainPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
						.ifPresent(be -> be.linkToController(controllerPos));
				return;
			}
		}
		if (level == null) {
			return;
		}
		super.linkToController(controllerPos);
	}

	@Override
	public void unlinkFromController() {
		if (level != null) {
			BlockPos mainPos = getMainPos();
			if (!mainPos.equals(worldPosition)) {
				level.getBlockEntity(mainPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
						.ifPresent(ChestBlockEntity::unlinkFromController);
				return;
			}
		}

		if (level == null) {
			return;
		}

		super.unlinkFromController();
	}
}
