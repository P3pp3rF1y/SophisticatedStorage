package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

public class StorageInputBlockEntity extends StorageIOBlockEntity {
	@Nullable
	private IItemHandler itemHandler;

	public StorageInputBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.STORAGE_INPUT_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Nullable
	@Override
	public IItemHandler getExternalItemHandler(@Nullable Direction side) {
		if (getControllerPos().isEmpty()) {
			return null;
		}

		if (itemHandler == null) {
			itemHandler = super.getExternalItemHandler(null);
			if (itemHandler instanceof IItemHandlerSimpleInserter simpleInserter) {
				itemHandler = new SingleSlotInputItemHandlerWrapper(simpleInserter);
			}
		}

		return itemHandler;
	}

	@Override
	protected void invalidateItemHandlerCache() {
		super.invalidateItemHandlerCache();
		itemHandler = null;
	}

	private static class SingleSlotInputItemHandlerWrapper implements IItemHandler {
		private final IItemHandlerSimpleInserter itemHandler;

		public SingleSlotInputItemHandlerWrapper(IItemHandlerSimpleInserter itemHandler) {
			this.itemHandler = itemHandler;
		}

		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return itemHandler.insertItem(stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 99;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return true;
		}
	}
}
