package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompressionUpgradeItemTest {
	private static MockedStatic<RecipeHelper> recipeHelperMock;

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		recipeHelperMock = Mockito.mockStatic(RecipeHelper.class);
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.IRON_NUGGET), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)))
				.thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.IRON_INGOT), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.IRON_INGOT), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)))
				.thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.IRON_BLOCK), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(any(ItemStack.class))).thenReturn(RecipeHelper.UncompactingResult.EMPTY);
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.IRON_BLOCK)))
				.thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.IRON_INGOT), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.IRON_INGOT)))
				.thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.IRON_NUGGET), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));

		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(any(ItemStack.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.IRON_NUGGET)))
				.thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.IRON_INGOT)))
				.thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
	}

	private static ItemStack stackOf(Item item) {
		return org.mockito.ArgumentMatchers.argThat(stack -> stack.getItem() == item);
	}

	@AfterAll
	static void tearDown() {
		recipeHelperMock.close();
	}

	@Test
	void canUseForCompressionAllowsCompressionChainWithEmptySlotsBetweenItems() {
		Map<Integer, ItemStack> slotStacks = Map.of(0, new ItemStack(Items.IRON_BLOCK), 2, new ItemStack(Items.IRON_NUGGET));

		UpgradeSlotChangeResult result = validateCompressionSlots(slotStacks);

		assertTrue(result.successful());
	}

	@Test
	void canUseForCompressionRejectsItemOneSlotTooFarFromPreviousCompressionLevel() {
		Map<Integer, ItemStack> slotStacks = Map.of(0, new ItemStack(Items.IRON_BLOCK), 1, new ItemStack(Items.IRON_INGOT), 3,
				new ItemStack(Items.IRON_NUGGET));

		UpgradeSlotChangeResult result = validateCompressionSlots(slotStacks);

		assertFalse(result.successful());
		assertEquals(Set.of(3), result.errorInventorySlots());
	}

	@Test
	void canUseForCompressionRejectsItemTwoSlotsTooFarFromPreviousCompressionLevel() {
		Map<Integer, ItemStack> slotStacks = Map.of(0, new ItemStack(Items.IRON_BLOCK), 1, new ItemStack(Items.IRON_INGOT), 4,
				new ItemStack(Items.IRON_NUGGET));

		UpgradeSlotChangeResult result = validateCompressionSlots(slotStacks);

		assertFalse(result.successful());
		assertEquals(Set.of(4), result.errorInventorySlots());
	}

	private UpgradeSlotChangeResult validateCompressionSlots(Map<Integer, ItemStack> slotStacks) {
		return getCompressionUpgradeItem().canUseForCompression(getStorageWrapper(slotStacks), new SlotRange(0, 5));
	}

	private IStorageWrapper getStorageWrapper(Map<Integer, ItemStack> slotStacks) {
		InventoryHandler inventoryHandler = mock(InventoryHandler.class);
		when(inventoryHandler.getInternalStack(anyInt())).thenAnswer(invocation -> slotStacks.getOrDefault(invocation.getArgument(0), ItemStack.EMPTY));

		MemorySettingsCategory memorySettingsCategory = mock(MemorySettingsCategory.class);
		when(memorySettingsCategory.getSlotFilterStack(anyInt(), anyBoolean())).thenReturn(Optional.empty());

		SettingsHandler settingsHandler = mock(SettingsHandler.class);
		when(settingsHandler.getTypeCategory(MemorySettingsCategory.class)).thenReturn(memorySettingsCategory);

		IStorageWrapper storageWrapper = mock(IStorageWrapper.class);
		when(storageWrapper.getInventoryHandler()).thenReturn(inventoryHandler);
		when(storageWrapper.getSettingsHandler()).thenReturn(settingsHandler);
		return storageWrapper;
	}

	private CompressionUpgradeItem getCompressionUpgradeItem() {
		return mock(CompressionUpgradeItem.class, Mockito.CALLS_REAL_METHODS);
	}
}
