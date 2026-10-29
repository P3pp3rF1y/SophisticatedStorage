package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandlerSlotTracker;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedcore.util.SlotValueMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class CompressionInventoryPartOverflowTest {
	private MockedStatic<RecipeHelper> recipes;

	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void setUpNonCompressibleRecipe() {
		recipes = mockStatic(RecipeHelper.class);
		recipes.when(() -> RecipeHelper.getItemCompactingShapes(any(ItemStack.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
		recipes.when(() -> RecipeHelper.getUncompactingResult(any(ItemStack.class))).thenReturn(RecipeHelper.UncompactingResult.EMPTY);
	}

	@AfterEach
	void closeRecipeMock() {
		recipes.close();
	}

	@Test
	void insertItemIntoHandlerSimulationLeavesCompressionContentsUnchanged() {
		Fixture fixture = new Fixture(true);

		fixture.insertIntoHandler(true);

		for (int slot = 0; slot < 3; slot++) {
			assertTrue(fixture.contents.getStackInSlot(slot).isEmpty());
		}
	}

	@Test
	void insertItemIntoHandlerSimulationDoesNotInstallCompressionSlotDefinitions() {
		Fixture fixture = new Fixture(true);

		fixture.insertIntoHandler(true);

		for (int slot = 0; slot < 3; slot++) {
			assertTrue(fixture.part.isSlotAccessible(slot));
			assertTrue(fixture.part.isItemValid(slot, new ItemStack(Items.GOLD_INGOT), null, (s, stack) -> true));
		}
	}

	@Test
	void insertItemIntoHandlerSimulationReturnsSameRemainderOnRepeatedCalls() {
		Fixture fixture = new Fixture(true);

		ItemStack first = fixture.insertIntoHandler(true);
		ItemStack second = fixture.insertIntoHandler(true);

		assertEquals(1, first.getCount());
		assertEquals(1, second.getCount());
		assertTrue(first.is(Items.DIAMOND));
		assertTrue(ItemStack.isSameItemSameTags(first, second));
	}

	@Test
	void insertItemIntoHandlerSimulationDoesNotInvokeStorageOverflowForUndefinedAlternativeSlots() {
		Fixture fixture = new Fixture(true);

		fixture.insertIntoHandler(true);

		assertEquals(0, fixture.storageOverflowCalls);
		assertEquals(0, fixture.voided);
	}

	@Test
	void insertItemIntoHandlerInitializesCompressionSlotDefinitions() {
		Fixture fixture = new Fixture(true);

		fixture.insertIntoHandler(false);

		assertTrue(fixture.part.isSlotAccessible(0));
		assertTrue(fixture.part.isItemValid(0, new ItemStack(Items.DIAMOND), null, (slot, stack) -> true));
		assertFalse(fixture.part.isItemValid(0, new ItemStack(Items.GOLD_INGOT), null, (slot, stack) -> true));
		assertFalse(fixture.part.isSlotAccessible(1));
		assertFalse(fixture.part.isSlotAccessible(2));
	}

	@Test
	void insertItemIntoHandlerVoidsExcessWhenStorageOverflowMatches() {
		Fixture fixture = new Fixture(true);

		ItemStack remainder = fixture.insertIntoHandler(false);

		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.contents.getStackInSlot(0).getCount());
		assertTrue(fixture.contents.getStackInSlot(0).is(Items.DIAMOND));
		assertTrue(fixture.contents.getStackInSlot(1).isEmpty());
		assertTrue(fixture.contents.getStackInSlot(2).isEmpty());
		assertEquals(65, fixture.contents.getStackInSlot(0).getCount() + fixture.voided + remainder.getCount());
	}

	@Test
	void insertItemIntoHandlerReturnsExcessWhenStorageOverflowRejectsIt() {
		Fixture fixture = new Fixture(false);

		ItemStack remainder = fixture.insertIntoHandler(false);

		assertEquals(1, remainder.getCount());
		assertTrue(remainder.is(Items.DIAMOND));
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.contents.getStackInSlot(0).getCount());
		assertTrue(fixture.contents.getStackInSlot(0).is(Items.DIAMOND));
		assertTrue(fixture.contents.getStackInSlot(1).isEmpty());
		assertTrue(fixture.contents.getStackInSlot(2).isEmpty());
		assertEquals(65, fixture.contents.getStackInSlot(0).getCount() + remainder.getCount());
	}

	@Test
	void insertItemIntoHandlerAfterSimulationUsesActualCompressionCapacity() {
		Fixture fixture = new Fixture(true);
		fixture.insertIntoHandler(true);
		fixture.insertIntoHandler(true);

		ItemStack remainder = fixture.insertIntoHandler(false);

		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.contents.getStackInSlot(0).getCount());
		assertTrue(fixture.contents.getStackInSlot(0).is(Items.DIAMOND));
		assertFalse(fixture.part.isSlotAccessible(1));
		assertFalse(fixture.part.isSlotAccessible(2));
		assertEquals(65, fixture.contents.getStackInSlot(0).getCount() + fixture.voided + remainder.getCount());
	}

	private static class Fixture {
		private final InventoryHandler inventory = mock(InventoryHandler.class);
		private final ItemStackHandler contents = new ItemStackHandler(3);
		private final MemorySettingsCategory memory = new MemorySettingsCategory(() -> inventory, new CompoundTag(), tag -> {
		});
		private final InventoryHandlerSlotTracker tracker = new InventoryHandlerSlotTracker(memory, new SlotValueMap<>());
		private final CompressionInventoryPart part;
		private final boolean storageVoids;
		private int voided;
		private int storageOverflowCalls;

		private Fixture(boolean storageVoids) {
			this.storageVoids = storageVoids;
			part = spy(new CompressionInventoryPart(inventory, new SlotRange(0, 3), () -> memory));
			doReturn(Optional.empty()).when(part).getDecompressionResultFromConfig(any(Item.class));
			doReturn(Optional.empty()).when(part).getCompressionResultFromConfig(any(ItemStack.class));
			when(inventory.getSlots()).thenReturn(3);
			when(inventory.getBaseSlotLimit()).thenReturn(64);
			when(inventory.getBaseStackLimit(any(ItemStack.class))).thenReturn(64);
			when(inventory.getSlotStack(anyInt())).thenAnswer(invocation -> contents.getStackInSlot(invocation.getArgument(0)));
			doAnswer(invocation -> {
				contents.setStackInSlot(invocation.getArgument(0), invocation.getArgument(1));
				return null;
			}).when(inventory).setSlotStack(anyInt(), any(ItemStack.class));
			when(inventory.getStackInSlot(anyInt())).thenAnswer(invocation -> part.getStackInSlot(invocation.getArgument(0), contents::getStackInSlot));
			when(inventory.getStackLimit(anyInt(), any(ItemStack.class)))
					.thenAnswer(invocation -> part.getStackLimit(invocation.getArgument(0), invocation.getArgument(1)));
			when(inventory.isSlotAccessible(anyInt())).thenAnswer(invocation -> part.isSlotAccessible(invocation.getArgument(0)));
			when(inventory.isItemValid(anyInt(), any(ItemStack.class)))
					.thenAnswer(invocation -> part.isItemValid(invocation.getArgument(0), invocation.getArgument(1), null, (slot, stack) -> true));
			part.onInit();
			tracker.refreshSlotIndexesFrom(inventory);
		}

		private ItemStack insertIntoHandler(boolean simulate) {
			return tracker.insertItemIntoHandler(inventory, (stack, sim) -> stack, this::insert, stack -> stack, this::overflow, 0,
					new ItemStack(Items.DIAMOND, 65), simulate);
		}

		private ItemStack insert(int slot, ItemStack stack, boolean simulate) {
			ItemStack remainder = part.insertItem(slot, stack, simulate, contents::insertItem);
			if (!simulate) {
				tracker.refreshSlotIndexesFrom(inventory);
			}
			return remainder;
		}

		private ItemStack overflow(ItemStack stack) {
			storageOverflowCalls++;
			if (!storageVoids) {
				return stack;
			}
			voided += stack.getCount();
			return ItemStack.EMPTY;
		}
	}
}
