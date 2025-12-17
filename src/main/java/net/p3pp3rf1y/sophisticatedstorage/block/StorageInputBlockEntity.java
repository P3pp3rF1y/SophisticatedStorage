package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jspecify.annotations.Nullable;

public class StorageInputBlockEntity extends StorageIOBlockEntity {
	@Nullable
	private ResourceHandler<ItemResource> itemResourceHandler;

	public StorageInputBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.STORAGE_INPUT_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Nullable
	@Override
	public ResourceHandler<ItemResource> getExternalItemResourceHandler(@Nullable Direction side) {
		if (getControllerPos().isEmpty()) {
			return null;
		}

		if (itemResourceHandler == null) {
			ResourceHandler<ItemResource> handler = super.getExternalItemResourceHandler(null);
			if (handler == null) {
				return null;
			}
			itemResourceHandler = new SingleSlotInputItemResourceHandlerWrapper(handler);
		}

		return itemResourceHandler;
	}

	@Override
	protected void invalidateItemHandlerCache() {
		super.invalidateItemHandlerCache();
		itemResourceHandler = null;
	}

	private static class SingleSlotInputItemResourceHandlerWrapper implements ResourceHandler<ItemResource> {
		private final ResourceHandler<ItemResource> itemResourceHandler;

		public SingleSlotInputItemResourceHandlerWrapper(ResourceHandler<ItemResource> itemResourceHandler) {
			this.itemResourceHandler = itemResourceHandler;
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public ItemResource getResource(int slot) {
			return ItemResource.EMPTY;
		}

		@Override
		public long getAmountAsLong(int slot) {
			return 0;
		}

		@Override
		public long getCapacityAsLong(int slot, ItemResource itemResource) {
			return 99;
		}

		@Override
		public boolean isValid(int slot, ItemResource itemResource) {
			return true;
		}

		@Override
		public int insert(int slot, ItemResource itemResource, int amount, TransactionContext transaction) {
			return 0;
		}

		@Override
		public int insert(ItemResource itemResource, int amount, TransactionContext transaction) {
			return itemResourceHandler.insert(itemResource, amount, transaction);
		}

		@Override
		public int extract(int slot, ItemResource itemResource, int amount, TransactionContext transaction) {
			return itemResourceHandler.insert(slot, itemResource, amount, transaction);
		}
	}
}
