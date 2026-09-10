package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.BakedModelWrapper;

public final class SporeBlossomDisplayModel extends BakedModelWrapper<BakedModel> {
	public static final double FACE_CLEARANCE = 1.0 / 1024.0;
	private static final float ATTACHMENT_Y = 15.9F / 16.0F;
	private final ItemTransforms transforms;

	private SporeBlossomDisplayModel(BakedModel original) {
		super(original);
		ItemTransforms source = original.getTransforms();
		Vector3f scale = source.getTransform(ItemTransforms.TransformType.FIXED).scale;
		ItemTransform fixed = new ItemTransform(new Vector3f(90, 0, 0),
				new Vector3f(0, 0, -(ATTACHMENT_Y - 0.5F) * scale.y()), scale);
		transforms = new ItemTransforms(source.thirdPersonLeftHand, source.thirdPersonRightHand,
				source.firstPersonLeftHand, source.firstPersonRightHand, source.head,
				source.gui, source.ground, fixed, source.moddedTransforms);
	}

	public static BakedModel wrap(ItemStack stack, BakedModel model) {
		return stack.is(Items.SPORE_BLOSSOM) && !(model instanceof SporeBlossomDisplayModel)
				? new SporeBlossomDisplayModel(model) : model;
	}

	@Override
	public ItemTransforms getTransforms() {
		return transforms;
	}

	@Override
	public boolean doesHandlePerspectives() {
		return true;
	}

	@Override
	public BakedModel handlePerspective(ItemTransforms.TransformType type, PoseStack poseStack) {
		if (type == ItemTransforms.TransformType.FIXED) {
			return ForgeHooksClient.handlePerspective(this, type, poseStack);
		}
		return originalModel.handlePerspective(type, poseStack);
	}
}
