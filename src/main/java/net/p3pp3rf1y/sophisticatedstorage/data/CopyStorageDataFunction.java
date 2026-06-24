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
import net.p3pp3rf1y.sophisticatedstorage.block.ISimpleMaterialHolder;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.SimpleMaterialBlockItem;

public class CopyStorageDataFunction extends LootItemConditionalFunction {
	protected CopyStorageDataFunction(LootItemCondition[] conditionsIn) {
		super(conditionsIn);
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
		BlockEntity be = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
		if (state != null && state.getBlock() instanceof IAdditionalDropDataBlock additionalDropDataBlock) {
			if (be instanceof StorageBlockEntity storageBlockEntity) {
				additionalDropDataBlock.addDropData(stack, storageBlockEntity);
			}
		}
		if (be instanceof ISimpleMaterialHolder simpleMaterialHolder && stack.getItem() instanceof SimpleMaterialBlockItem) {
			simpleMaterialHolder.getMaterial().ifPresentOrElse(material -> SimpleMaterialBlockItem.setMaterial(stack, material),
					() -> SimpleMaterialBlockItem.removeMaterial(stack));
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
