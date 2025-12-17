package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jspecify.annotations.Nullable;

public class StorageOutputBlockEntity extends StorageIOBlockEntity {
	@Nullable
	private ResourceHandler<ItemResource> itemHandler;

	public StorageOutputBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.STORAGE_OUTPUT_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Nullable
	@Override
	public ResourceHandler<ItemResource> getExternalItemResourceHandler(@Nullable Direction side) {
		if (getControllerPos().isEmpty()) {
			return null;
		}
		if (itemHandler == null) {
			ResourceHandler<ItemResource> handler = super.getExternalItemResourceHandler(null);
			if (handler != null) {
				itemHandler = new OutputOnlyItemHandlerWrapper(handler);
			}
		}

		return itemHandler;
	}

	@Override
	protected void invalidateItemHandlerCache() {
		super.invalidateItemHandlerCache();
		itemHandler = null;
	}

	private record OutputOnlyItemHandlerWrapper(ResourceHandler<ItemResource> itemHandler) implements ResourceHandler<ItemResource> {
		@Override
		public int size() {
			return itemHandler.size();
		}

		@Override
		public long getAmountAsLong(int i) {
			return itemHandler.getAmountAsLong(i);
		}

		@Override
		public ItemResource getResource(int i) {
			return itemHandler.getResource(i);
		}

		@Override
		public int insert(int i, ItemResource resource, int i1, TransactionContext transactionContext) {
			return 0;
		}

		@Override
		public int insert(ItemResource resource, int amount, TransactionContext transaction) {
			return 0;
		}

		@Override
		public int extract(int i, ItemResource resource, int i1, TransactionContext transactionContext) {
			return itemHandler.extract(i, resource, i1, transactionContext);
		}

		@Override
		public int extract(ItemResource resource, int amount, TransactionContext transaction) {
			return itemHandler.extract(resource, amount, transaction);
		}

		@Override
		public long getCapacityAsLong(int i, ItemResource resource) {
			return itemHandler.getCapacityAsLong(i, resource);
		}

		@Override
		public boolean isValid(int i, ItemResource resource) {
			return false;
		}
	}
}
