package net.p3pp3rf1y.sophisticatedstorage.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.p3pp3rf1y.sophisticatedstorage.block.IAdditionalDropDataBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

public class CopyStorageDataFunction extends LootItemConditionalFunction {
	protected CopyStorageDataFunction(LootItemCondition[] conditionsIn) {
		super(conditionsIn);
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
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


	public static CopyStorageDataFunction.Builder builder() {
		return new CopyStorageDataFunction.Builder();
	}

	public static class Serializer extends LootItemConditionalFunction.Serializer<CopyStorageDataFunction> {

		@Override
		public CopyStorageDataFunction deserialize(JsonObject object, JsonDeserializationContext deserializationContext, LootItemCondition[] conditionsIn) {
			return new CopyStorageDataFunction(conditionsIn);
		}
	}

	public static class Builder extends LootItemConditionalFunction.Builder<CopyStorageDataFunction.Builder> {
		@Override
		protected CopyStorageDataFunction.Builder getThis() {
			return this;
		}

		@Override
		public LootItemFunction build() {
			return new CopyStorageDataFunction(getConditions());
		}
	}
}
