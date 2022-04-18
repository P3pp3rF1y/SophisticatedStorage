package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.common.util.NonNullLazy;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ShulkerBoxItemRenderer;

import java.util.function.Consumer;

public class ShulkerBoxItem extends StorageBlockItem {
	public ShulkerBoxItem(Block block) {
		super(block);
	}

	@Override
	public void initializeClient(Consumer<IItemRenderProperties> consumer) {
		consumer.accept(new IItemRenderProperties() {
			private final NonNullLazy<BlockEntityWithoutLevelRenderer> ister = NonNullLazy.of(() -> new ShulkerBoxItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()));

			@Override
			public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
				return ister.get();
			}
		});
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	@Override
	public void onDestroyed(ItemEntity pItemEntity) {
		ItemStack itemstack = pItemEntity.getItem();
		CompoundTag compoundtag = getBlockEntityData(itemstack);
		if (compoundtag == null) {
			return;
		}

		StorageWrapper storageWrapper = new StorageWrapper(() -> () -> {}, () -> {}, () -> {}) {
			@Override
			protected void onUpgradeRefresh() {
				//noop - there should be no upgrade refresh happening here
			}

			@Override
			protected int getDefaultNumberOfInventorySlots() {
				return getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfInventorySlots() : 0;
			}

			@Override
			protected boolean isAllowedInStorage(ItemStack stack) {
				//TODO add config with other things that can't go in
				//TODO add backpacks compat so that they can't go in
				return !(Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock);
			}

			@Override
			protected int getDefaultNumberOfUpgradeSlots() {
				return getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfUpgradeSlots() : 0;
			}
		};
		storageWrapper.load(compoundtag);
		Level level = pItemEntity.level;
		if (!level.isClientSide) {
			InventoryHelper.dropItems(storageWrapper.getInventoryHandler(), level, pItemEntity.getX(), pItemEntity.getY(), pItemEntity.getZ());
			InventoryHelper.dropItems(storageWrapper.getUpgradeHandler(), level, pItemEntity.getX(), pItemEntity.getY(), pItemEntity.getZ());
		}
	}
}
