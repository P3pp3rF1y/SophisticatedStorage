package net.p3pp3rf1y.sophisticatedstorage.data;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.p3pp3rf1y.sophisticatedstorage.block.IAdditionalDropDataBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;

public class CopyStorageDataFunction implements LootItemFunction {
	private static final CopyStorageDataFunction INSTANCE = new CopyStorageDataFunction();
	public static final MapCodec<CopyStorageDataFunction> CODEC = MapCodec.unit(INSTANCE).stable();

	private CopyStorageDataFunction() {
	}

	@Override
	public ItemStack apply(ItemStack stack, LootContext context) {
		BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
		BlockEntity be = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (state != null && state.getBlock() instanceof IAdditionalDropDataBlock additionalDropDataBlock) {
			if (be instanceof StorageBlockEntity storageBlockEntity) {
				additionalDropDataBlock.addDropData(stack, storageBlockEntity);
			}
		}
		if (be instanceof ISimpleMaterialHolder simpleMaterialHolder && stack.getItem() instanceof SimpleMaterialBlockItem) {
			simpleMaterialHolder.getMaterial().ifPresentOrElse(material -> SimpleMaterialBlockItem.setMaterial(stack, material), () -> SimpleMaterialBlockItem.removeMaterial(stack));
		}

		return stack;
	}

	@Override
	public LootItemFunctionType<? extends LootItemFunction> getType() {
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
