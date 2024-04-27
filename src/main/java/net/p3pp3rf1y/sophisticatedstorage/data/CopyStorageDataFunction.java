package net.p3pp3rf1y.sophisticatedstorage.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.p3pp3rf1y.sophisticatedstorage.block.IAdditionalDropDataBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

public class CopyStorageDataFunction implements LootItemFunction {
	private static final CopyStorageDataFunction INSTANCE = new CopyStorageDataFunction();
	public static final Codec<CopyStorageDataFunction> CODEC = MapCodec.unit(INSTANCE).stable().codec();

	private CopyStorageDataFunction() {
	}

	@Override
	public ItemStack apply(ItemStack stack, LootContext context) {
		BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
		if (state != null && state.getBlock() instanceof IAdditionalDropDataBlock additionalDropDataBlock) {
			BlockEntity be = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
			if (be instanceof StorageBlockEntity storageBlockEntity) {
				additionalDropDataBlock.addDropData(stack, storageBlockEntity);
			}
		}

		return stack;
	}

	@Override
	public LootItemFunctionType getType() {
		return ModItems.COPY_STORAGE_DATA.get();
	}


	public static Builder builder() {
		return new Builder();
	}

	public static class Builder implements LootItemFunction.Builder {
		@Override
		public LootItemFunction build() {
			return new CopyStorageDataFunction();
		}
	}
}
