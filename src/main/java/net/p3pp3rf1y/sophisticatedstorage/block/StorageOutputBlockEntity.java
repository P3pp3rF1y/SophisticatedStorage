package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;

public class StorageOutputBlockEntity extends StorageIOBlockEntity {
	@Nullable
	private IItemHandler itemHandler;

	public StorageOutputBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.STORAGE_OUTPUT_BLOCK_ENTITY_TYPE.get(), pos, state);
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
				itemHandler = new OutputOnlyItemHandlerWrapper(simpleInserter);
			}
		}

		return itemHandler;
	}

	@Override
	protected void invalidateItemHandlerCache() {
		super.invalidateItemHandlerCache();
		itemHandler = null;
	}

	private static class OutputOnlyItemHandlerWrapper implements IItemHandler {
		private final IItemHandlerSimpleInserter itemHandler;

		public OutputOnlyItemHandlerWrapper(IItemHandlerSimpleInserter itemHandler) {
			this.itemHandler = itemHandler;
		}

		@Override
		public int getSlots() {
			return itemHandler.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return itemHandler.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return itemHandler.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return itemHandler.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	}
}
