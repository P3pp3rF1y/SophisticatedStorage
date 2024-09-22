package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StorageInputBlockEntity extends StorageIOBlockEntity {

	public StorageInputBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.STORAGE_INPUT_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Override
	protected <T> LazyOptional<T> getControllerCapability(Capability<T> cap, @Nullable Direction side, ControllerBlockEntity c) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
				return c.getCapability(ForgeCapabilities.ITEM_HANDLER, null) //passing null side to not get the cache failed handler
						.map(itemHandler -> LazyOptional.of(() -> itemHandler instanceof IItemHandlerSimpleInserter simpleInserter ? new SingleSlotInputItemHandlerWrapper(simpleInserter) : itemHandler))
						.orElseGet(LazyOptional::empty).cast();
		}

		return super.getControllerCapability(cap, side, c);
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
		public @NotNull ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
			return itemHandler.insertItem(stack, simulate);
		}

		@Override
		public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return true;
		}
	}
}
