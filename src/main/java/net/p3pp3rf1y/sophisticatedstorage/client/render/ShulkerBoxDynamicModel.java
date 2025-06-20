package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ExtendedUnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class ShulkerBoxDynamicModel implements ExtendedUnbakedModel {
	private static final String BLOCK_BREAK_FOLDER = "block/break/";
	public static final ResourceLocation TINTABLE_BREAK_TEXTURE = SophisticatedStorage.getRL(BLOCK_BREAK_FOLDER + "tintable_shulker_box");
	public static final ResourceLocation MAIN_BREAK_TEXTURE = SophisticatedStorage.getRL(BLOCK_BREAK_FOLDER + "shulker_box");

	@Override
	public BakedModel bake(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState, boolean useAmbientOcclusion, boolean usesBlockLight, ItemTransforms itemTransforms, ContextMap contextMap) {
		return new ShulkerBoxBakedModel();
	}

	@Override
	public void resolveDependencies(Resolver resolver) {

	}

	private static class ShulkerBoxBakedModel implements BakedModel {
		private static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
			return Collections.emptyList();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		@Override
		public boolean isGui3d() {
			return true;
		}

		@Override
		public boolean usesBlockLight() {
			return true;
		}

		@SuppressWarnings("deprecation")
		@Override
		public TextureAtlasSprite getParticleIcon() {
			BakedModel model = Minecraft.getInstance().getModelManager().getModel(BlockModelShaper.stateToModelLocation(Blocks.OAK_PLANKS.defaultBlockState()));
			return model.getParticleIcon();
		}

		@Override
		public ItemTransforms getTransforms() {
			return ItemTransforms.NO_TRANSFORMS;
		}

		@Nonnull
		@Override
		public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
			return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class)
					.map(be -> {
						ModelData.Builder builder = ModelData.builder();
						builder.with(HAS_MAIN_COLOR, be.getStorageWrapper().getMainColor() != -1);
						return builder.build();
					}).orElse(ModelData.EMPTY);
		}

		@Override
		public TextureAtlasSprite getParticleIcon(ModelData data) {
			ResourceLocation texture = TINTABLE_BREAK_TEXTURE;
			if (Boolean.FALSE.equals(data.get(HAS_MAIN_COLOR))) {
				texture = MAIN_BREAK_TEXTURE;
			}
			return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
		}
	}

	public static final class Loader implements UnbakedModelLoader<ShulkerBoxDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public ShulkerBoxDynamicModel read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
			return new ShulkerBoxDynamicModel();
		}
	}
}
