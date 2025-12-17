package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ISyncedContainer;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerClientDataPayload;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DecorationTableMenu extends AbstractContainerMenu implements ISyncedContainer {
	private static final String SET_INHERITANCE_ACTION = "setInheritance";
	private static final Identifier EMPTY_RED_DYE_SLOT_BACKGROUND = SophisticatedStorage.getIdentifier("container/slot/red_dye");
	private static final Identifier EMPTY_GREEN_DYE_SLOT_BACKGROUND = SophisticatedStorage.getIdentifier("container/slot/green_dye");
	private static final Identifier EMPTY_BLUE_DYE_SLOT_BACKGROUND = SophisticatedStorage.getIdentifier("container/slot/blue_dye");
	private static final Identifier EMPTY_MATERIAL_SLOT_BACKGROUND = SophisticatedStorage.getIdentifier("container/slot/material");
	public static final int DECORATION_SLOT_PADDING = 12;
	private final DecorationTableBlockEntity blockEntity;

	private Slot resultSlot;

	private SlotRange decorationSlotRange;
	private SlotRange dyeSlotRange;
	private SlotRange storageSlotRange;
	private SlotRange playerSlotRange;
	@Nullable
	private Runnable slotChangedListener = null;
	private ResourceHandlerSlot storageSlot;

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
		ItemStacksResourceHandler storageBlock = blockEntity.getStorageBlock();
		storageSlot = new ResourceHandlerSlot(storageBlock,
				(index, resource, amount) -> {
					storageBlock.set(index, resource, amount);
					if (slotChangedListener != null) {
						slotChangedListener.run();
					}
				}, 0, getSlot(dyeSlotRange.firstSlot()).x, getSlot(DecorationTableBlockEntity.BOTTOM_TRIM_SLOT).y);
		addSlot(storageSlot);

		storageSlotRange = new SlotRange(dyeSlotRange.firstSlot() + dyeSlotRange.size(), 1);

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
				InventoryHelper.extract(blockEntity.getStorageBlock(), 0, blockEntity.getStorageBlock().getResource(0), 1);
			}
		};
		addSlot(resultSlot);
	}

	private int addDecorationSlots() {
		int xOffset = 8;
		int yOffset = 17;
		ItemStacksResourceHandler decorativeBlocks = blockEntity.getDecorativeBlocks();
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
		decorationSlotRange = new SlotRange(0, decorativeBlocks.size());
		x += 44;

		ItemStacksResourceHandler dyes = blockEntity.getDyes();
		addSlot(new ResourceHandlerSlot(dyes, dyes::set, 0, x, yOffset).setBackground(EMPTY_RED_DYE_SLOT_BACKGROUND));
		x += 18;
		addSlot(new ResourceHandlerSlot(dyes, dyes::set, 1, x, yOffset).setBackground(EMPTY_GREEN_DYE_SLOT_BACKGROUND));
		x += 18;
		addSlot(new ResourceHandlerSlot(dyes, dyes::set, 2, x, yOffset).setBackground(EMPTY_BLUE_DYE_SLOT_BACKGROUND));
		dyeSlotRange = new SlotRange(decorationSlotRange.firstSlot() + decorationSlotRange.size(), dyes.size());

		return y;
	}

	private int addDecorationSlot(ItemStacksResourceHandler itemHandler, int slotIndex, int xOffset, int y, int yPadding) {
		addSlot(new ResourceHandlerSlot(itemHandler,
				(index, resource, amount) -> {
					itemHandler.set(index, resource, amount);
					if (slotChangedListener != null) {
						slotChangedListener.run();
					}
				}, slotIndex, xOffset, y).setBackground(EMPTY_MATERIAL_SLOT_BACKGROUND));
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

		playerSlotRange = new SlotRange(storageSlotRange.firstSlot() + storageSlotRange.size() + 1, 36);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = getSlot(slotIndex);
		ItemStack slotStack = slot.getItem();
		ItemStack slotStackCopy = slotStack.copy();
		if (isPlayerSlot(slotIndex)) {
			ItemResource resource = ItemResource.of(slotStack);
			if (blockEntity.getDecorativeBlocks().isValid(decorationSlotRange.firstSlot(), resource)
					&& !moveItemStackTo(slotStack, decorationSlotRange, false)) {
				return ItemStack.EMPTY;
			} else if (isValidDye(slotStack)
					&& !moveItemStackTo(slotStack, dyeSlotRange, false)) {
				return ItemStack.EMPTY;
			} else if (blockEntity.getStorageBlock().isValid(0, resource)
					&& !moveItemStackTo(slotStack, storageSlotRange, false)) {
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
		ItemStacksResourceHandler dyes = blockEntity.getDyes();

		ItemResource resource = ItemResource.of(stack);
		for (int slot = 0; slot < dyes.size(); slot++) {
			if (dyes.isValid(slot, resource)) {
				return true;
			}
		}

		return false;
	}

	private boolean moveItemStackTo(ItemStack stack, SlotRange slotRange, boolean reverse) {
		return moveItemStackTo(stack, slotRange.firstSlot(), slotRange.firstSlot() + slotRange.size(), reverse);
	}

	private boolean isPlayerSlot(int slotIndex) {
		return playerSlotRange.isInRange(slotIndex);
	}

	public SlotRange getDyeSlotRange() {
		return dyeSlotRange;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.isWithinBlockInteractionRange(blockEntity.getBlockPos(), 4);
	}

	public static DecorationTableMenu fromBuffer(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
		return new DecorationTableMenu(containerId, playerInventory.player, buffer.readBlockPos());
	}

	public void setSlotMaterialInheritance(DecorationTableBlockEntity.PartSlot slot, boolean inheritance) {
		blockEntity.setSlotMaterialInheritance(slot, inheritance);
		sendToServer(tag -> {
			tag.putString("action", SET_INHERITANCE_ACTION);
			tag.putString("slot", slot.getSerializedName());
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

	public boolean isSlotMaterialInherited(int slotIndex) {
		return isSlotMaterialInherited(DecorationTableBlockEntity.PartSlot.fromSlotIndex(slotIndex));
	}

	public boolean isSlotMaterialInherited(DecorationTableBlockEntity.PartSlot slot) {
		return blockEntity.isSlotMaterialInherited(slot);
	}

	public ItemResource getInheritedResource(int slotIndex) {
		return getInheritedResource(DecorationTableBlockEntity.PartSlot.fromSlotIndex(slotIndex));
	}

	public ItemResource getInheritedResource(DecorationTableBlockEntity.PartSlot childSlot) {
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
		if (blockEntity.getLevel() == null || !blockEntity.getLevel().isClientSide()) {
			return;
		}

		CompoundTag data = new CompoundTag();
		addData.accept(data);
		ClientPacketDistributor.sendToServer(new SyncContainerClientDataPayload(data));
	}

	public Map<Identifier, Integer> getPartsNeeded() {
		return blockEntity.getPartsNeeded();
	}

	public Set<Identifier> getMissingDyes() {
		return blockEntity.getMissingDyes();
	}

	@Override
	public void handlePacket(CompoundTag data) {
		data.getString("action").ifPresent(action -> {
			if (action.equals(SET_INHERITANCE_ACTION)) {
				data.getString("slot").ifPresent(slotName -> {
					data.getBoolean("inheritance").ifPresent(inheritance -> {
						setSlotMaterialInheritance(DecorationTableBlockEntity.PartSlot.fromName(slotName), inheritance);
					});
				});
			}
		});
		data.getInt("mainColor").ifPresent(this::setMainColor);
		data.getInt("accentColor").ifPresent(this::setAccentColor);
	}

	public Map<Identifier, Integer> getPartsStored() {
		return blockEntity.getPartsStored();
	}

	public boolean hasMaterials() {
		return blockEntity.hasMaterials();
	}
}
