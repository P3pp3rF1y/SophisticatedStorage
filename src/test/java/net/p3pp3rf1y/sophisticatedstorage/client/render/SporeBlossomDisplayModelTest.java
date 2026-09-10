package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.model.TransformationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SporeBlossomDisplayModelTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void vanillaAttachmentIsFlushAndAllPetalsProjectOutward() throws Exception {
		BakedModel model = SporeBlossomDisplayModel.wrap(new ItemStack(Items.SPORE_BLOSSOM), getOriginalModel());
		Matrix4f transform = TransformationHelper.toTransformation(model.getTransforms().fixed).getMatrix();
		try (InputStream stream = getClass().getResourceAsStream("/assets/minecraft/models/block/spore_blossom.json")) {
			assertNotNull(stream, "Validate against the actual Minecraft model geometry");
			JsonArray elements = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
					.getAsJsonObject().getAsJsonArray("elements");
			for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
				JsonObject element = elements.get(elementIndex).getAsJsonObject();
				JsonArray from = element.getAsJsonArray("from");
				JsonArray to = element.getAsJsonArray("to");
				for (int corner = 0; corner < 8; corner++) {
					Vector3f vertex = new Vector3f(
							((corner & 1) == 0 ? from : to).get(0).getAsFloat(),
							((corner & 2) == 0 ? from : to).get(1).getAsFloat(),
							((corner & 4) == 0 ? from : to).get(2).getAsFloat());
					if (element.has("rotation")) {
						JsonObject rotation = element.getAsJsonObject("rotation");
						JsonArray xyz = rotation.getAsJsonArray("origin");
						Vector3f origin = new Vector3f(xyz.get(0).getAsFloat(), xyz.get(1).getAsFloat(), xyz.get(2).getAsFloat());
						Vector3f axis = rotation.get("axis").getAsString().equals("x") ? Vector3f.XP : Vector3f.ZP;
						vertex.sub(origin);
						vertex.transform(axis.rotationDegrees(rotation.get("angle").getAsFloat()));
						vertex.add(origin);
					}
					Vector4f displayed = new Vector4f(vertex.x() / 16 - 0.5F,
							vertex.y() / 16 - 0.5F, vertex.z() / 16 - 0.5F, 1);
					displayed.transform(transform);
					for (float slotScale : new float[] {1.0F, 0.5F}) {
						double distanceOutsidePanel = SporeBlossomDisplayModel.FACE_CLEARANCE - displayed.z() * slotScale;
						if (elementIndex == 0) {
							assertEquals(SporeBlossomDisplayModel.FACE_CLEARANCE, distanceOutsidePanel, 1e-6,
									"The attachment plane must sit flush against the front");
						} else {
							assertTrue(distanceOutsidePanel > SporeBlossomDisplayModel.FACE_CLEARANCE,
									"Petals must extend away from the panel, not into the barrel");
						}
					}
				}
			}
		}
	}

	@Test
	void forgeDynamicRenderingUsesTheSameTransformAsBakedQuads() {
		BakedModel model = SporeBlossomDisplayModel.wrap(new ItemStack(Items.SPORE_BLOSSOM), getOriginalModel());
		PoseStack dynamic = new PoseStack();
		assertSame(model, ForgeHooksClient.handleCameraTransforms(dynamic, model, ItemTransforms.TransformType.FIXED, false));
		Matrix4f baked = TransformationHelper.toTransformation(model.getTransforms().fixed).getMatrix();
		for (Vector4f point : new Vector4f[] {new Vector4f(0, 0, 0, 1), new Vector4f(0.3F, 0.49F, -0.4F, 1)}) {
			Vector4f expected = new Vector4f(point.x(), point.y(), point.z(), point.w());
			expected.transform(baked);
			point.transform(dynamic.last().pose());
			assertEquals(expected.x(), point.x(), 1e-6);
			assertEquals(expected.y(), point.y(), 1e-6);
			assertEquals(expected.z(), point.z(), 1e-6);
		}
	}

	@Test
	void leavesOtherItemsAndTheSharedModelUnchanged() {
		BakedModel original = getOriginalModel();
		ItemTransform fixed = original.getTransforms().fixed;
		assertSame(original, SporeBlossomDisplayModel.wrap(new ItemStack(Items.STONE), original));
		BakedModel blossom = SporeBlossomDisplayModel.wrap(new ItemStack(Items.SPORE_BLOSSOM), original);
		assertNotSame(original, blossom);
		assertSame(fixed, original.getTransforms().fixed);
		assertEquals(0, fixed.rotation.x());
		assertSame(original.getTransforms().gui, blossom.getTransforms().gui);
		assertSame(blossom, SporeBlossomDisplayModel.wrap(new ItemStack(Items.SPORE_BLOSSOM), blossom));
	}

	private BakedModel getOriginalModel() {
		BakedModel model = mock(BakedModel.class);
		ItemTransform none = ItemTransform.NO_TRANSFORM;
		ItemTransform fixed = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(0.5F, 0.5F, 0.5F));
		when(model.getTransforms()).thenReturn(new ItemTransforms(none, none, none, none, none, none, none, fixed));
		return model;
	}
}
