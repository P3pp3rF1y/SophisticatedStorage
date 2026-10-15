package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BarrelGeometryCacheKeyTest {
	@Test
	void bakedPartKeyRetainsDistinctTextureSlotsWithCollidingHashes() {
		UnbakedGeometry geometry = mock(UnbakedGeometry.class);
		BarrelUnbakedModelBase.ModelStateKey state = BarrelUnbakedModelBase.ModelStateKey.of(modelState(new Matrix4f()));
		TextureSlots.SlotContents slot = new TextureSlots.Data.Builder().addTexture("texture", new Material(Identifier.parse("minecraft:block/stone"))).build()
				.values().get("texture");
		BarrelUnbakedModelBase.BakedPartKey first = new BarrelUnbakedModelBase.BakedPartKey(BarrelModelPart.BASE, geometry, List.of(Map.of("Aa", slot)), state);
		BarrelUnbakedModelBase.BakedPartKey second = new BarrelUnbakedModelBase.BakedPartKey(BarrelModelPart.BASE, geometry, List.of(Map.of("BB", slot)),
				state);
		Map<BarrelUnbakedModelBase.BakedPartKey, String> cache = new HashMap<>();

		cache.put(first, "first");
		cache.put(second, "second");

		assertEquals(first.hashCode(), second.hashCode());
		assertNotEquals(first, second);
		assertEquals(2, cache.size());
		assertEquals("first", cache.get(first));
		assertEquals("second", cache.get(second));
	}

	@Test
	void modelStateKeySeparatesFaceUvTransformationsWithIdenticalModelTransform() {
		BarrelUnbakedModelBase.ModelStateKey unlocked = BarrelUnbakedModelBase.ModelStateKey.of(modelState(new Matrix4f()));
		BarrelUnbakedModelBase.ModelStateKey locked = BarrelUnbakedModelBase.ModelStateKey.of(modelState(new Matrix4f().rotateZ(0.5f)));

		assertNotEquals(unlocked, locked);
	}

	@Test
	void modelStateKeySeparatesUvLockEvenWithIdenticalMatrices() {
		ModelState unlocked = modelState(new Matrix4f());
		BlockModelRotation.WithUvLock locked = mock(BlockModelRotation.WithUvLock.class);
		Transformation transformation = unlocked.transformation();
		when(locked.transformation()).thenReturn(transformation);
		for (Direction direction : Direction.values()) {
			Matrix4f face = (Matrix4f) unlocked.faceTransformation(direction);
			Matrix4f inverse = (Matrix4f) unlocked.inverseFaceTransformation(direction);
			when(locked.faceTransformation(direction)).thenReturn(face);
			when(locked.inverseFaceTransformation(direction)).thenReturn(inverse);
		}

		BarrelUnbakedModelBase.ModelStateKey unlockedKey = BarrelUnbakedModelBase.ModelStateKey.of(unlocked);
		BarrelUnbakedModelBase.ModelStateKey lockedKey = BarrelUnbakedModelBase.ModelStateKey.of(locked);

		assertNotEquals(unlockedKey, lockedKey);
	}

	@Test
	void dynamicModelKeyRetainsDistinctMaterialsWithCollidingHashesAndCopiesInput() {
		DynamicBarrelBakingData bakingData = mock(DynamicBarrelBakingData.class);
		Material material = new Material(Identifier.parse("minecraft:block/stone"));
		Map<String, Material> mutableMaterials = new HashMap<>(Map.of("Aa", material));
		BarrelBlockStateModelBase.DynamicModelKey first = new BarrelBlockStateModelBase.DynamicModelKey(bakingData, mutableMaterials);
		BarrelBlockStateModelBase.DynamicModelKey second = new BarrelBlockStateModelBase.DynamicModelKey(bakingData, Map.of("BB", material));
		Map<BarrelBlockStateModelBase.DynamicModelKey, String> cache = new HashMap<>();

		mutableMaterials.clear();
		cache.put(first, "first");
		cache.put(second, "second");

		assertEquals(first.hashCode(), second.hashCode());
		assertNotEquals(first, second);
		assertEquals(2, cache.size());
		assertEquals("first", cache.get(first));
		assertEquals("second", cache.get(second));
		assertEquals(Map.of("Aa", material), first.materials());
		assertThrows(UnsupportedOperationException.class, () -> first.materials().put("extra", material));
		assertNotEquals(first, new BarrelBlockStateModelBase.DynamicModelKey(mock(DynamicBarrelBakingData.class), first.materials()));
	}

	private static ModelState modelState(Matrix4f faceTransformation) {
		ModelState state = mock(ModelState.class);
		when(state.transformation()).thenReturn(new Transformation(null, null, null, null));
		for (Direction direction : Direction.values()) {
			when(state.faceTransformation(direction)).thenReturn(faceTransformation);
			when(state.inverseFaceTransformation(direction)).thenReturn(new Matrix4f());
		}
		return state;
	}
}
