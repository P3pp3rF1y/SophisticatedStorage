package net.p3pp3rf1y.sophisticatedstorage.client.render;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.p3pp3rf1y.sophisticatedcore.renderdata.DisplaySide;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BarrelBakedModelCacheKeyTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void normalBarrelNormalizationIgnoresOnlyNonRenderingProperties() {
		assertNormalizationRetainsVisualStates(BarrelBlock.FACING, BarrelBlock.OPEN, BarrelBlock.FLAT_TOP);
	}

	@Test
	void limitedBarrelNormalizationIgnoresOnlyNonRenderingProperties() {
		assertNormalizationRetainsVisualStates(LimitedBarrelBlock.HORIZONTAL_FACING, LimitedBarrelBlock.VERTICAL_FACING, BarrelBlock.FLAT_TOP);
	}

	private static void assertNormalizationRetainsVisualStates(Property<?>... visualProperties) {
		BlockState base = mock(BlockState.class);
		BlockState ticking = mock(BlockState.class);
		BlockState translucent = mock(BlockState.class);
		when(ticking.hasProperty(StorageBlockBase.TICKING)).thenReturn(true);
		when(ticking.getValue(StorageBlockBase.TICKING)).thenReturn(true);
		when(ticking.setValue(StorageBlockBase.TICKING, false)).thenReturn(base);
		when(translucent.hasProperty(BarrelBlock.OPAQUE)).thenReturn(true);
		when(translucent.getValue(BarrelBlock.OPAQUE)).thenReturn(false);
		when(translucent.setValue(BarrelBlock.OPAQUE, true)).thenReturn(base);
		assertEquals(base, BarrelBakedModelCacheKey.normalizeState(ticking));
		assertEquals(base, BarrelBakedModelCacheKey.normalizeState(translucent));
		for (Property<?> visualProperty : visualProperties) {
			BlockState visual = mock(BlockState.class);
			when(visual.hasProperty(visualProperty)).thenReturn(true);
			assertNotEquals(base, BarrelBakedModelCacheKey.normalizeState(visual));
		}
	}

	@Test
	void insertedKeySnapshotsMutableInputsAndComparesTagsAndDisplaySide() {
		ItemStack stack = new ItemStack(Items.DIAMOND);
		stack.setTag(new CompoundTag());
		stack.getTag().putString("label", "first");
		List<RenderInfo.DisplayItem> displayItems = new ArrayList<>();
		displayItems.add(new RenderInfo.DisplayItem(stack, 0, 1, DisplaySide.FRONT, 0));
		List<Integer> slots = new ArrayList<>(List.of(2));
		Map<ResourceLocation, Integer> tints = new HashMap<>();
		ResourceLocation sprite = new ResourceLocation("test", "sprite");
		tints.put(sprite, 1);
		BarrelBakedModelCacheKey stored = key(displayItems, slots, tints).immutableCopy();
		BarrelBakedModelCacheKey original = key(displayItems, slots, tints);
		assertEquals(stored, original);
		assertEquals(stored.hashCode(), original.hashCode());
		stack.getTag().putString("label", "second");
		slots.add(3);
		tints.put(sprite, 2);
		displayItems.clear();
		assertNotEquals(stored, key(displayItems, slots, tints));
		assertEquals(stored, key(List.of(new RenderInfo.DisplayItem(taggedStack("first"), 0, 1, DisplaySide.FRONT, 0)), List.of(2), Map.of(sprite, 1)));
		assertNotEquals(stored, key(List.of(new RenderInfo.DisplayItem(taggedStack("second"), 0, 1, DisplaySide.FRONT, 0)), List.of(2), Map.of(sprite, 1)));
		assertNotEquals(stored, key(List.of(new RenderInfo.DisplayItem(taggedStack("first"), 0, 1, DisplaySide.LEFT, 0)), List.of(2), Map.of(sprite, 1)));
	}

	@Test
	void cacheKeyPreservesDirectionAndDisplayOrdering() {
		RenderInfo.DisplayItem first = new RenderInfo.DisplayItem(taggedStack("first"), 0, 0, DisplaySide.FRONT, 0);
		RenderInfo.DisplayItem second = new RenderInfo.DisplayItem(taggedStack("second"), 0, 1, DisplaySide.RIGHT, 0);
		BarrelBakedModelCacheKey original = key(List.of(first, second), List.of(1, 2), Map.of());
		assertNotEquals(original, key(List.of(second, first), List.of(1, 2), Map.of()));
		assertNotEquals(original, key(List.of(first, second), List.of(2, 1), Map.of()));
		assertNotEquals(original, new BarrelBakedModelCacheKey(null, null, Direction.SOUTH, null, Items.AIR, null, false, false, false, false, true, false,
				Map.of(), Map.of(), List.of(first, second), List.of(1, 2)));
	}

	@Test
	void emptyDisplayStacksWithDifferentTagsHaveEqualKeysAndHashes() {
		ItemStack first = taggedStack("first");
		ItemStack second = taggedStack("second");
		first.setCount(0);
		second.setCount(0);
		BarrelBakedModelCacheKey firstKey = key(List.of(new RenderInfo.DisplayItem(first, 0, 1, DisplaySide.FRONT, 0)), List.of(), Map.of());
		BarrelBakedModelCacheKey secondKey = key(List.of(new RenderInfo.DisplayItem(second, 0, 1, DisplaySide.FRONT, 0)), List.of(), Map.of());
		assertEquals(firstKey, secondKey);
		assertEquals(firstKey.hashCode(), secondKey.hashCode());
		BarrelBakedModelCacheKey stored = firstKey.immutableCopy();
		assertEquals(stored, secondKey);
		assertEquals(stored.hashCode(), secondKey.hashCode());
	}

	private static ItemStack taggedStack(String label) {
		ItemStack stack = new ItemStack(Items.DIAMOND);
		CompoundTag tag = new CompoundTag();
		tag.putString("label", label);
		stack.setTag(tag);
		return stack;
	}

	private static BarrelBakedModelCacheKey key(List<RenderInfo.DisplayItem> displayItems, List<Integer> slots, Map<ResourceLocation, Integer> tints) {
		return new BarrelBakedModelCacheKey(null, null, Direction.NORTH, null, Items.AIR, null, false, false, false, false, true, false,
				Map.<BarrelMaterial, ResourceLocation>of(), tints, displayItems, slots);
	}
}
