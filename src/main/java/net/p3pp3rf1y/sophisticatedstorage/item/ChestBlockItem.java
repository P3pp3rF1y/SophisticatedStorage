package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.NonNullLazy;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ChestItemRenderer;

import java.util.function.Consumer;

public class ChestBlockItem extends WoodStorageBlockItem {
	private static final String DOUBLE_CHEST_TAG = "doubleChest";
	public ChestBlockItem(Block block) {
		this(block, new Properties());
	}
	public ChestBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private final NonNullLazy<BlockEntityWithoutLevelRenderer> ister = NonNullLazy.of(() -> new ChestItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()));

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return ister.get();
			}
		});
	}

	public static boolean isDoubleChest(ItemStack stack) {
		return NBTHelper.getBoolean(stack, DOUBLE_CHEST_TAG).orElse(false);
	}

	public static void setDoubleChest(ItemStack stack, boolean doubleChest) {
		stack.getOrCreateTag().putBoolean(DOUBLE_CHEST_TAG, doubleChest);
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext pContext, BlockState pState) {
		return super.placeBlock(pContext, pState);
	}
}
