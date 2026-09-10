package net.p3pp3rf1y.sophisticatedstorage.compat.compressium;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.GameData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class CompressiumDisplayModelTest {
	private static Item compressedStone;

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		GameData.unfreezeData();
		compressedStone = new BlockItem(Blocks.STONE, new Item.Properties()).setRegistryName("compressium", "stone_1");
		ForgeRegistries.ITEMS.register(compressedStone);
	}

	@BeforeEach
	void clearCache() {
		CompressiumDisplayModel.clearCache();
	}

	@Test
	void missingModLeavesEvenMatchingItemsUntouched() {
		try (MockedStatic<ModList> mods = mockStatic(ModList.class)) {
			mods.when(ModList::get).thenReturn(mock(ModList.class));
			BakedModel original = mock(BakedModel.class);
			assertSame(original, CompressiumDisplayModel.wrap(new ItemStack(compressedStone), original));
			verifyNoInteractions(original);
		}
	}

	@Test
	void loadedModLeavesOtherNamespacesUntouched() {
		try (MockedStatic<ModList> mods = mockStatic(ModList.class)) {
			ModList list = mock(ModList.class);
			when(list.isLoaded("compressium")).thenReturn(true);
			mods.when(ModList::get).thenReturn(list);
			BakedModel original = mock(BakedModel.class);
			assertSame(original, CompressiumDisplayModel.wrap(new ItemStack(Items.STONE), original));
			verifyNoInteractions(original);
		}
	}

	@Test
	void missingOverlayFallsBackToOriginalModel() throws Exception {
		try (MockedStatic<ModList> mods = mockStatic(ModList.class); MockedStatic<Minecraft> client = mockStatic(Minecraft.class)) {
			ResourceManager resources = setupResources(mods, client);
			when(resources.getResource(any(ResourceLocation.class))).thenThrow(new FileNotFoundException());
			BakedModel original = mock(BakedModel.class);
			assertSame(original, CompressiumDisplayModel.wrap(new ItemStack(compressedStone), original));
		}
	}

	@Test
	void bakedAndDynamicRenderingUseOverlayAndReloadDropsCachedGeometry() throws Exception {
		try (MockedStatic<ModList> mods = mockStatic(ModList.class); MockedStatic<Minecraft> client = mockStatic(Minecraft.class);
				NativeImage image = new NativeImage(2, 2, true)) {
			ResourceManager resources = setupResources(mods, client);
			image.setPixelRGBA(0, 0, 0xFF000000);
			byte[] png = image.asByteArray();
			Resource resource = mock(Resource.class);
			when(resource.getInputStream()).thenAnswer(i -> new ByteArrayInputStream(png));
			when(resources.getResource(new ResourceLocation("compressium", "textures/block/layer_1.png"))).thenReturn(resource);
			BakedModel original = mock(BakedModel.class);
			when(original.getParticleIcon()).thenReturn(mock(TextureAtlasSprite.class));
			when(original.getTransforms()).thenReturn(ItemTransforms.NO_TRANSFORMS);
			ItemStack stack = new ItemStack(compressedStone);
			BakedModel model = CompressiumDisplayModel.wrap(stack, original);
			assertNotSame(original, model);
			assertFalse(model.isCustomRenderer());
			for (Direction side : Direction.values()) {
				List<BakedQuad> baked = model.getQuads(null, side, new Random());
				List<BakedQuad> dynamic = model.getQuads(null, side, new Random(), EmptyModelData.INSTANCE);
				assertEquals(4, baked.size());
				assertSame(baked, dynamic);
				assertEquals(0xFF000000, baked.get(0).getVertices()[3]);
				assertEquals(0xFFFFFFFF, baked.get(1).getVertices()[3]);
			}
			assertSame(model, CompressiumDisplayModel.wrap(stack, original));
			CompressiumDisplayModel.clearCache();
			assertNotSame(model, CompressiumDisplayModel.wrap(stack, original));
		}
	}

	private ResourceManager setupResources(MockedStatic<ModList> mods, MockedStatic<Minecraft> client) {
		ModList list = mock(ModList.class);
		when(list.isLoaded("compressium")).thenReturn(true);
		mods.when(ModList::get).thenReturn(list);
		Minecraft minecraft = mock(Minecraft.class);
		client.when(Minecraft::getInstance).thenReturn(minecraft);
		ResourceManager resources = mock(ResourceManager.class);
		when(minecraft.getResourceManager()).thenReturn(resources);
		return resources;
	}
}
