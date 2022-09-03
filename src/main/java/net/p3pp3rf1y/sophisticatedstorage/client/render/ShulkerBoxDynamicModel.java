package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ShulkerBoxDynamicModel implements IUnbakedGeometry<ShulkerBoxDynamicModel> {
	private static final String BLOCK_BREAK_FOLDER = "block/break/";
	public static final ResourceLocation TINTABLE_BREAK_TEXTURE = SophisticatedStorage.getRL(BLOCK_BREAK_FOLDER + "tintable_shulker_box");
	public static final ResourceLocation MAIN_BREAK_TEXTURE = SophisticatedStorage.getRL(BLOCK_BREAK_FOLDER + "shulker_box");

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
		return new ShulkerBoxBakedModel();
	}

	@Override
	public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		return Collections.emptySet();
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

		@Override
		public boolean isCustomRenderer() {
			return true;
		}

		@SuppressWarnings("deprecation")
		@Override
		public TextureAtlasSprite getParticleIcon() {
			BakedModel model = Minecraft.getInstance().getModelManager().getModel(BlockModelShaper.stateToModelLocation(Blocks.OAK_PLANKS.defaultBlockState()));
			return model.getParticleIcon();
		}

		@Nonnull
		@Override
		public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
			return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class)
					.map(be -> {
						ModelData.Builder builder = ModelData.builder();
						builder.with(HAS_MAIN_COLOR, be.getStorageWrapper().getMainColor() > -1);
						return builder.build();
					}).orElse(ModelData.EMPTY);
		}

		@Override
		public TextureAtlasSprite getParticleIcon(ModelData data) {
			ResourceLocation texture = TINTABLE_BREAK_TEXTURE;
			if (Boolean.FALSE.equals(data.get(HAS_MAIN_COLOR))) {
				texture = MAIN_BREAK_TEXTURE;
			}
			return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
		}

		@Override
		public ItemOverrides getOverrides() {
			return new ItemOverrides() {};
		}
	}

	public static final class Loader implements IGeometryLoader<ShulkerBoxDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public ShulkerBoxDynamicModel read(JsonObject modelContents, JsonDeserializationContext deserializationContext) {
			return new ShulkerBoxDynamicModel();
		}
	}
}
