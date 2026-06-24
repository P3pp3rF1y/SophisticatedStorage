package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ISyncedContainer;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerClientDataMessage;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DecorationTableMenu extends AbstractContainerMenu implements ISyncedContainer {
	private static final String SET_INHERITANCE_ACTION = "setInheritance";
	private static final ResourceLocation EMPTY_RED_DYE_SLOT_BACKGROUND = SophisticatedStorage.getRL("item/empty_red_dye_slot");
	private static final ResourceLocation EMPTY_GREEN_DYE_SLOT_BACKGROUND = SophisticatedStorage.getRL("item/empty_green_dye_slot");
	private static final ResourceLocation EMPTY_BLUE_DYE_SLOT_BACKGROUND = SophisticatedStorage.getRL("item/empty_blue_dye_slot");
	private static final ResourceLocation EMPTY_MATERIAL_SLOT_BACKGROUND = SophisticatedStorage.getRL("item/empty_material_slot");
	public static final int DECORATION_SLOT_PADDING = 12;
	private final DecorationTableBlockEntity blockEntity;

	private Slot resultSlot;

	private SlotRange decorationSlotRange;
	private SlotRange dyeSlotRange;
	private SlotRange storageSlotRange;
	private SlotRange playerSlotRange;
	@Nullable
	private Runnable slotChangedListener = null;
	private SlotItemHandler storageSlot;

	public DecorationTableMenu(int containerId, Player player, BlockPos pos) {
		super(ModBlocks.DECORATION_TABLE_CONTAINER_TYPE.get(), containerId);
		blockEntity = player.level().getBlockEntity(pos, ModBlocks.DECORATION_TABLE_BLOCK_ENTITY_TYPE.get()).orElse(null);
		if (blockEntity == null) {
			throw new IllegalStateException("No block entity found at position " + pos);
		}

		int y = addDecorationSlots();
		addStorageSlots();
		y += 14;
		addPlayerSlots(player.getInventory(), y);
	}

	public void setSlotChangedListener(@Nullable Runnable listener) {
		slotChangedListener = listener;
	}

	public Slot getStorageSlot() {
		return storageSlot;
	}

	public List<ItemStack> getDecoratedPreviewStacks() {
		return blockEntity.getDecoratedPreviewStacks();
	}

	private void addStorageSlots() {
		ItemStackHandler storageBlock = blockEntity.getStorageBlock();
		storageSlot = new SlotItemHandler(storageBlock, 0, getSlot(dyeSlotRange.firstSlot()).x, getSlot(DecorationTableBlockEntity.BOTTOM_TRIM_SLOT).y) {
			@Override
			public void setChanged() {
				super.setChanged();
				if (slotChangedListener != null) {
					slotChangedListener.run();
				}
			}
		};
		addSlot(storageSlot);

		storageSlotRange = new SlotRange(dyeSlotRange.firstSlot() + dyeSlotRange.numberOfSlots(), 1);

		resultSlot = new Slot(new SimpleContainer(1) {
			@Override
			public ItemStack getItem(int index) {
				return blockEntity.getResult();
			}

			@Override
			public ItemStack removeItem(int index, int count) {
				return index == 0 ? blockEntity.extractResult(count) : ItemStack.EMPTY;
			}
		}, 0, storageSlot.x + 18 + 18, storageSlot.y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean mayPickup(Player player) {
				return super.mayPickup(player) && getMissingDyes().isEmpty();
			}

			@Override
			public ItemStack remove(int amount) {
				return super.remove(amount);
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				super.onTake(player, stack);
				if (player.level().isClientSide()) {
					return;
				}
				blockEntity.consumeIngredientsOnCraft();
				blockEntity.getStorageBlock().extractItem(0, 1, false);
			}
		};
		addSlot(resultSlot);
	}

	private int addDecorationSlots() {
		int xOffset = 8;
		int yOffset = 17;
		ItemStackHandler decorativeBlocks = blockEntity.getDecorativeBlocks();
		int x = xOffset;
		int y = yOffset;
		int slotIndex = 0;
		y = addDecorationSlot(decorativeBlocks, slotIndex, x, y, DECORATION_SLOT_PADDING);
		y = addDecorationSlot(decorativeBlocks, 1, x, y, DECORATION_SLOT_PADDING);
		y = addDecorationSlot(decorativeBlocks, 2, x, y, DECORATION_SLOT_PADDING);
		addDecorationSlot(decorativeBlocks, 3, x, y, DECORATION_SLOT_PADDING);
		y = yOffset + 18 + DECORATION_SLOT_PADDING;
		x += 48;
		y = addDecorationSlot(decorativeBlocks, 4, x, y, DECORATION_SLOT_PADDING);
		y = addDecorationSlot(decorativeBlocks, 5, x, y, DECORATION_SLOT_PADDING);
		y = addDecorationSlot(decorativeBlocks, 6, x, y, 0);
		decorationSlotRange = new SlotRange(0, decorativeBlocks.getSlots());
		x += 44;

		ItemStackHandler dyes = blockEntity.getDyes();
		addSlot(new SlotItemHandler(dyes, 0, x, yOffset).setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_RED_DYE_SLOT_BACKGROUND));
		x += 18;
		addSlot(new SlotItemHandler(dyes, 1, x, yOffset).setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_GREEN_DYE_SLOT_BACKGROUND));
		x += 18;
		addSlot(new SlotItemHandler(dyes, 2, x, yOffset).setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_BLUE_DYE_SLOT_BACKGROUND));
		dyeSlotRange = new SlotRange(decorationSlotRange.firstSlot() + decorationSlotRange.numberOfSlots(), dyes.getSlots());

		return y;
	}

	private int addDecorationSlot(ItemStackHandler itemHandler, int slotIndex, int xOffset, int y, int yPadding) {
		addSlot(new SlotItemHandler(itemHandler, slotIndex, xOffset, y) {
			@Override
			public void setChanged() {
				super.setChanged();
				if (slotChangedListener != null) {
					slotChangedListener.run();
				}
			}
		}.setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_MATERIAL_SLOT_BACKGROUND));
		y += 18;
		y += yPadding;
		return y;
	}

	private void addPlayerSlots(Inventory playerInventory, int y) {
		int playerSlotXOffset = 45;
		int hotbarPadding = 4;

		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				addSlot(new Slot(playerInventory, col + row * 9 + 9, playerSlotXOffset + col * 18, y + row * 18));
			}
		}

		for (int col = 0; col < 9; ++col) {
			Slot slot = new Slot(playerInventory, col, playerSlotXOffset + col * 18, y + 3 * 18 + hotbarPadding);
			addSlot(slot);
		}

		playerSlotRange = new SlotRange(storageSlotRange.firstSlot() + storageSlotRange.numberOfSlots() + 1, 36);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = getSlot(slotIndex);
		ItemStack slotStack = slot.getItem();
		ItemStack slotStackCopy = slotStack.copy();
		if (isPlayerSlot(slotIndex)) {
			if (blockEntity.getDecorativeBlocks().isItemValid(decorationSlotRange.firstSlot(), slotStack)
					&& !moveItemStackTo(slotStack, decorationSlotRange, false)) {
				return ItemStack.EMPTY;
			} else if (isValidDye(slotStack) && !moveItemStackTo(slotStack, dyeSlotRange, false)) {
				return ItemStack.EMPTY;
			} else if (blockEntity.getStorageBlock().isItemValid(0, slotStack) && !moveItemStackTo(slotStack, storageSlotRange, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (!moveItemStackTo(slotStack, playerSlotRange, true)) {
				return ItemStack.EMPTY;
			}
		}

		if (slotStackCopy.getCount() == slotStack.getCount()) {
			return ItemStack.EMPTY;
		}

		blockEntity.updateResultAndSetChanged();
		slot.onTake(player, slotStackCopy);
		return slotStackCopy;
	}

	private boolean isValidDye(ItemStack stack) {
		ItemStackHandler dyes = blockEntity.getDyes();

		for (int slot = 0; slot < dyes.getSlots(); slot++) {
			if (dyes.isItemValid(slot, stack)) {
				return true;
			}
		}

		return false;
	}

	private boolean moveItemStackTo(ItemStack stack, SlotRange slotRange, boolean reverse) {
		return moveItemStackTo(stack, slotRange.firstSlot(), slotRange.firstSlot() + slotRange.numberOfSlots(), reverse);
	}

	private boolean isPlayerSlot(int slotIndex) {
		return playerSlotRange.isInRange(slotIndex);
	}

	public SlotRange getDyeSlotRange() {
		return dyeSlotRange;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.distanceToSqr((double) blockEntity.getBlockPos().getX() + 0.5D, (double) blockEntity.getBlockPos().getY() + 0.5D,
				(double) blockEntity.getBlockPos().getZ() + 0.5D) <= 64.0D;
	}

	public static DecorationTableMenu fromBuffer(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
		return new DecorationTableMenu(containerId, playerInventory.player, buffer.readBlockPos());
	}

	public void setSlotMaterialInheritance(int slot, boolean inheritance) {
		blockEntity.setSlotMaterialInheritance(slot, inheritance);
		sendToServer(tag -> {
			tag.putString("action", SET_INHERITANCE_ACTION);
			tag.putInt("slot", slot);
			tag.putBoolean("inheritance", inheritance);
		});
	}

	public void setMainColor(int color) {
		blockEntity.setMainColor(color);
		sendToServer(tag -> tag.putInt("mainColor", color));
	}

	public void setAccentColor(int color) {
		blockEntity.setAccentColor(color);
		sendToServer(tag -> tag.putInt("accentColor", color));
	}

	public boolean isSlotMaterialInherited(int slot) {
		return blockEntity.isSlotMaterialInherited(slot);
	}

	public boolean isMaterialSlotActive(int slot) {
		return blockEntity.isMaterialSlotActive(slot);
	}

	public boolean isInheritanceSlotActive(int slot) {
		return blockEntity.getMaterialLayout() == DecorationTableBlockEntity.MaterialLayout.BARREL && blockEntity.isMaterialSlotActive(slot)
				&& blockEntity.getSlotInheritedFrom(slot) != -1;
	}

	public boolean areTintsActive() {
		return blockEntity.areTintsActive();
	}

	public boolean isMainTintActive() {
		return blockEntity.isMainTintActive();
	}

	public boolean isAccentTintActive() {
		return blockEntity.isAccentTintActive();
	}

	public DecorationTableBlockEntity.MaterialLayout getMaterialLayout() {
		return blockEntity.getMaterialLayout();
	}

	public ItemStack getInheritedItem(int childSlot) {
		return blockEntity.getInheritedItem(childSlot);
	}

	public int getMainColor() {
		return blockEntity.getMainColor();
	}

	public int getAccentColor() {
		return blockEntity.getAccentColor();
	}

	public Slot getResultSlot() {
		return resultSlot;
	}

	protected void sendToServer(Consumer<CompoundTag> addData) {
		if (blockEntity.getLevel() == null || !blockEntity.getLevel().isClientSide) {
			return;
		}

		CompoundTag data = new CompoundTag();
		addData.accept(data);
		PacketHandler.INSTANCE.sendToServer(new SyncContainerClientDataMessage(data));
	}

	public Map<ResourceLocation, Integer> getPartsNeeded() {
		return blockEntity.getPartsNeeded();
	}

	public Set<ResourceLocation> getMissingDyes() {
		return blockEntity.getMissingDyes();
	}

	@Override
	public void handleMessage(CompoundTag data) {
		String action = data.getString("action");
		if (action.equals(SET_INHERITANCE_ACTION)) {
			setSlotMaterialInheritance(data.getInt("slot"), data.getBoolean("inheritance"));
		} else if (data.contains("mainColor")) {
			setMainColor(data.getInt("mainColor"));
		} else if (data.contains("accentColor")) {
			setAccentColor(data.getInt("accentColor"));
		}
	}

	public Map<ResourceLocation, Integer> getPartsStored() {
		return blockEntity.getPartsStored();
	}

	public boolean hasMaterials() {
		return blockEntity.hasMaterials();
	}
}
