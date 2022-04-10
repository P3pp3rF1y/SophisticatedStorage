package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.common.util.NonNullLazy;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ChestItemRenderer;

import java.util.function.Consumer;

public class ChestBlockItem extends WoodStorageBlockItem {
	public ChestBlockItem(Block block) {
		super(block);
	}

	@Override
	public void initializeClient(Consumer<IItemRenderProperties> consumer) {
		consumer.accept(new IItemRenderProperties() {
			private final NonNullLazy<BlockEntityWithoutLevelRenderer> ister = NonNullLazy.of(() -> new ChestItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()));

			@Override
			public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
				return ister.get();
			}
		});
	}
}
