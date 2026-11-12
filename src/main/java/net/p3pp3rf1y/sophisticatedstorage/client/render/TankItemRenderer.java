package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BuiltInModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.TankForm;
import net.p3pp3rf1y.sophisticatedstorage.block.TankItemContents;
import net.p3pp3rf1y.sophisticatedstorage.block.TankItemStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.joml.Matrix4f;

public class TankItemRenderer extends BlockEntityWithoutLevelRenderer {
	public TankItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@SuppressWarnings({"deprecation", "java:S1874"})
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		for (RegistryObject<Item> item : ModBlocks.ITEMS.getEntries()) {
			if (item.get() instanceof TankBlockItem) {
				event.getModels().computeIfPresent(new ModelResourceLocation(item.getId(), "inventory"),
						(location, model) -> new BuiltInModel(model.getTransforms(), model.getOverrides(), model.getParticleIcon(ModelData.EMPTY),
								model.usesBlockLight()));
			}
		}
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (!(stack.getItem() instanceof TankBlockItem item)) {
			return;
		}
		int height = TankItemContents.height(stack);
		float scale = getScaleForDisplayContext(stack, context, height);
		pose.pushPose();
		pose.translate(.5, (1 - height * scale) / 2, .5);
		pose.scale(scale, scale, scale);
		pose.translate(-.5, 0, -.5);
		boolean showTier = TankItemContents.flag(stack, "showsTier", true);
		boolean packed = TankItemContents.flag(stack, "packed", false);
		for (int index = 0; index < height; index++) {
			pose.pushPose();
			pose.translate(0, index, 0);
			Minecraft.getInstance().getBlockRenderer().renderSingleBlock(item.getBlock().defaultBlockState()
					.setValue(TankBlock.FORM, TankForm.at(index, height)).setValue(TankBlock.SHOW_TIER, showTier).setValue(TankBlock.PACKED, packed), pose,
					buffers, light, overlay);
			pose.popPose();
		}
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
		TankRenderer.renderFluid(wrapper.getTankFluidHandler().getFluidInTank(0), wrapper.getCapacity(), height, pose, buffers, light);
		pose.popPose();
	}

	@SuppressWarnings({"deprecation", "java:S1874"})
	private static float getScaleForDisplayContext(ItemStack stack, ItemDisplayContext context, int height) {
		PoseStack transformPose = new PoseStack();
		Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0).getTransforms().getTransform(context)
				.apply(context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transformPose);
		Matrix4f matrix = transformPose.last().pose();
		float projectedHeight = Math.abs(matrix.m01()) + Math.abs(matrix.m11()) + Math.abs(matrix.m21());
		return projectedHeight > 0 ? projectedHeight / (projectedHeight + (height - 1) * Math.abs(matrix.m11())) : 1F / height;
	}
}
