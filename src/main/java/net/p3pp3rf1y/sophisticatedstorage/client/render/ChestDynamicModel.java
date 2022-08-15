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
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class ChestDynamicModel implements IModelGeometry<ChestDynamicModel> {
	private static final String BLOCK_BREAK_FOLDER = "block/break/";
	private static final Map<String, ResourceLocation> WOOD_BREAK_TEXTURES = new HashMap<>();
	public static final ResourceLocation TINTABLE_BREAK_TEXTURE = SophisticatedStorage.getRL(BLOCK_BREAK_FOLDER + "tintable_chest");

	static {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> WOOD_BREAK_TEXTURES.put(woodType.name(), SophisticatedStorage.getRL(BLOCK_BREAK_FOLDER + woodType.name() + "_chest")));
	}

	public static Collection<ResourceLocation> getWoodBreakTextures() {
		return WOOD_BREAK_TEXTURES.values();
	}

	@Override
	public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
		return new ChestBakedModel();
	}

	@Override
	public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
		return Collections.emptySet();
	}

	private static class ChestBakedModel implements BakedModel {
		private static final ModelProperty<String> WOOD_NAME = new ModelProperty<>();
		private static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
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

		@SuppressWarnings("java:S1874")
		@Override
		public TextureAtlasSprite getParticleIcon() {
			BakedModel model = Minecraft.getInstance().getModelManager().getModel(BlockModelShaper.stateToModelLocation(Blocks.OAK_PLANKS.defaultBlockState()));
			//noinspection deprecation
			return model.getParticleIcon();
		}

		@Nonnull
		@Override
		public IModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, IModelData modelData) {
			return WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class)
					.map(be -> {
						ModelDataMap.Builder builder = new ModelDataMap.Builder();
						builder.withInitial(HAS_MAIN_COLOR, be.getStorageWrapper().getMainColor() > -1);
						be.getWoodType().ifPresent(n -> builder.withInitial(WOOD_NAME, n.name()));
						return (IModelData) builder.build();
					}).orElse(EmptyModelData.INSTANCE);
		}

		@Override
		public TextureAtlasSprite getParticleIcon(IModelData data) {
			ResourceLocation texture = TINTABLE_BREAK_TEXTURE;
			if (Boolean.FALSE.equals(data.getData(HAS_MAIN_COLOR)) && data.hasProperty(WOOD_NAME) && WOOD_BREAK_TEXTURES.containsKey(data.getData(WOOD_NAME))) {
				texture = WOOD_BREAK_TEXTURES.get(data.getData(WOOD_NAME));
			}
			return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
		}

		@Override
		public ItemOverrides getOverrides() {
			return new ItemOverrides() {};
		}
	}

	public static final class Loader implements IModelLoader<ChestDynamicModel> {
		public static final Loader INSTANCE = new Loader();

		@Override
		public ChestDynamicModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {
			return new ChestDynamicModel();
		}

		@Override
		public void onResourceManagerReload(ResourceManager resourceManager) {
			//noop
		}
	}
}
