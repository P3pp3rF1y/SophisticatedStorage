package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class CompressionInventoryPartOverflowTest {
	private MockedStatic<RecipeHelper> recipes;
	private MockedStatic<SophisticatedStorage> storage;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		Items.DIAMOND.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
		Items.GOLD_INGOT.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
	}

	@BeforeEach
	void setUpNonCompressibleRecipe() {
		storage = mockStatic(SophisticatedStorage.class);
		storage.when(() -> SophisticatedStorage.getIdentifier(any(String.class))).thenAnswer(invocation -> Identifier.parse(invocation.getArgument(0)));
		recipes = mockStatic(RecipeHelper.class);
		recipes.when(() -> RecipeHelper.getItemCompactingShapes(any(ItemStack.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
		recipes.when(() -> RecipeHelper.getUncompactingResult(any(ItemStack.class))).thenReturn(RecipeHelper.UncompactingResult.EMPTY);
	}

	@AfterEach
	void closeRecipeMock() {
		recipes.close();
		storage.close();
	}

	@Test
	void insertItemIntoHandlerSimulationLeavesCompressionContentsUnchanged() {
		Fixture fixture = new Fixture(true);
		fixture.insertIntoHandler(true);
		for (int slot = 0; slot < 3; slot++) {
			assertTrue(fixture.inventory.getInternalStack(slot).isEmpty());
			assertTrue(fixture.inventory.getSlotTracker().getEmptySlots().contains(slot));
		}
	}

	@Test
	void insertItemIntoHandlerSimulationDoesNotInstallCompressionSlotDefinitions() {
		Fixture fixture = new Fixture(true);
		fixture.insertIntoHandler(true);
		for (int slot = 0; slot < 3; slot++) {
			assertTrue(fixture.part.isSlotAccessible(slot));
			assertTrue(fixture.part.isValid(slot, ItemResource.of(new ItemStack(Items.GOLD_INGOT)), null, (s, resource) -> true));
		}
	}

	@Test
	void insertItemIntoHandlerSimulationReturnsSameRemainderOnRepeatedCalls() {
		Fixture fixture = new Fixture(true);
		ItemStack first = fixture.insertIntoHandler(true);
		ItemStack second = fixture.insertIntoHandler(true);
		assertTrue(first.isEmpty());
		assertTrue(second.isEmpty());
		assertEquals(2, fixture.storageOverflowCalls);
		assertTrue(fixture.inventory.getInternalStack(0).isEmpty());
		assertTrue(fixture.inventory.getSlotTracker().getEmptySlots().contains(0));
	}

	@Test
	void insertItemIntoHandlerSimulationEvaluatesOverflowAgainstTemporaryCompressionDefinitions() {
		Fixture fixture = new Fixture(true);
		fixture.insertIntoHandler(true);
		assertEquals(1, fixture.storageOverflowCalls);
		assertEquals(1, fixture.voided);
		assertTrue(fixture.inventory.getInternalStack(0).isEmpty());
		assertTrue(fixture.part.isSlotAccessible(1));
		assertTrue(fixture.part.isSlotAccessible(2));
	}

	@Test
	void insertItemIntoHandlerInitializesCompressionSlotDefinitions() {
		Fixture fixture = new Fixture(true);
		fixture.insertIntoHandler(false);
		assertTrue(fixture.part.isSlotAccessible(0));
		assertTrue(fixture.part.isValid(0, ItemResource.of(new ItemStack(Items.DIAMOND)), null, (slot, resource) -> true));
		assertFalse(fixture.part.isValid(0, ItemResource.of(new ItemStack(Items.GOLD_INGOT)), null, (slot, resource) -> true));
		assertFalse(fixture.part.isSlotAccessible(1));
		assertFalse(fixture.part.isSlotAccessible(2));
	}

	@Test
	void insertItemIntoHandlerVoidsExcessWhenStorageOverflowMatches() {
		Fixture fixture = new Fixture(true);
		ItemStack remainder = fixture.insertIntoHandler(false);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided);
		assertEquals(64, fixture.inventory.getInternalStack(0).getCount());
		assertTrue(fixture.inventory.getInternalStack(0).is(Items.DIAMOND));
		assertTrue(fixture.inventory.getInternalStack(1).isEmpty());
		assertTrue(fixture.inventory.getInternalStack(2).isEmpty());
		assertEquals(65, fixture.inventory.getInternalStack(0).getCount() + fixture.voided + remainder.getCount());
	}

	@Test
	void insertItemIntoHandlerReturnsExcessWhenStorageOverflowRejectsIt() {
		Fixture fixture = new Fixture(false);
		ItemStack remainder = fixture.insertIntoHandler(false);
		assertEquals(1, remainder.getCount());
		assertTrue(remainder.is(Items.DIAMOND));
		assertEquals(0, fixture.voided);
		assertEquals(64, fixture.inventory.getInternalStack(0).getCount());
		assertTrue(fixture.inventory.getInternalStack(0).is(Items.DIAMOND));
		assertTrue(fixture.inventory.getInternalStack(1).isEmpty());
		assertTrue(fixture.inventory.getInternalStack(2).isEmpty());
		assertEquals(65, fixture.inventory.getInternalStack(0).getCount() + remainder.getCount());
	}

	@Test
	void insertItemIntoHandlerAfterSimulationUsesActualCompressionCapacity() {
		Fixture fixture = new Fixture(true);
		fixture.insertIntoHandler(true);
		fixture.insertIntoHandler(true);
		assertTrue(fixture.inventory.getSlotTracker().getEmptySlots().contains(0), "rolled-back slot should be empty in tracker");
		int voidedDuringProposals = fixture.voided;
		ItemStack remainder = fixture.insertIntoHandler(false);
		assertTrue(remainder.isEmpty());
		assertEquals(1, fixture.voided - voidedDuringProposals);
		assertEquals(64, fixture.inventory.getInternalStack(0).getCount());
		assertTrue(fixture.inventory.getInternalStack(0).is(Items.DIAMOND));
		assertFalse(fixture.part.isSlotAccessible(1));
		assertFalse(fixture.part.isSlotAccessible(2));
		assertEquals(65, fixture.inventory.getInternalStack(0).getCount() + fixture.voided - voidedDuringProposals + remainder.getCount());
	}

	private static class Fixture {
		private final IStorageWrapper wrapper = Mockito.mock(IStorageWrapper.class);
		private final SettingsHandler settings = Mockito.mock(SettingsHandler.class);
		private final UpgradeHandler upgrades = Mockito.mock(UpgradeHandler.class);
		private final VoidUpgradeWrapper voidUpgrade = Mockito.mock(VoidUpgradeWrapper.class);
		private InventoryHandler inventory;
		private final MemorySettingsCategory memory = new MemorySettingsCategory(() -> inventory, new MemorySettingsCategoryData(), () -> {
		});
		private final CompressionInventoryPart part;
		private final boolean storageVoids;
		private int voided;
		private int storageOverflowCalls;

		private Fixture(boolean storageVoids) {
			this.storageVoids = storageVoids;
			when(wrapper.getSettingsHandler()).thenReturn(settings);
			when(wrapper.getUpgradeHandler()).thenReturn(upgrades);
			when(settings.getTypeCategory(MemorySettingsCategory.class)).thenReturn(memory);
			when(upgrades.getTypeWrappers(VoidUpgradeItem.TYPE)).thenReturn(List.of(voidUpgrade));
			IOverflowResponseUpgrade overflow = Mockito.mock(IOverflowResponseUpgrade.class);
			when(upgrades.getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)).thenReturn(List.of(overflow));
			when(overflow.onStorageOverflow(any(ItemResource.class), anyInt())).thenAnswer(invocation -> {
				storageOverflowCalls++;
				int amount = invocation.getArgument(1);
				if (!this.storageVoids) {
					return 0;
				}
				voided += amount;
				return amount;
			});
			StackUpgradeConfig config = Mockito.mock(StackUpgradeConfig.class);
			when(config.canStackItem(any(Item.class))).thenReturn(true);
			inventory = new InventoryHandler(3, wrapper, new ContainerContents(), () -> {
			}, 64, config) {
				@Override
				protected boolean isAllowed(ItemResource resource) {
					return true;
				}
			};
			part = Mockito.mock(CompressionInventoryPart.class,
					Mockito.withSettings().useConstructor(inventory, new SlotRange(0, 3), (java.util.function.Supplier<MemorySettingsCategory>) () -> memory)
							.defaultAnswer(Mockito.CALLS_REAL_METHODS));
			doReturn(Optional.empty()).when(part).getDecompressionResultFromConfig(any(Item.class));
			doReturn(Optional.empty()).when(part).getCompressionResultFromConfig(any(ItemStack.class));
			inventory.getInventoryPartitioner().addInventoryPart(0, 3, part);
		}

		private ItemStack insertIntoHandler(boolean simulate) {
			ItemStack stack = new ItemStack(Items.DIAMOND, 65);
			int inserted;
			try (Transaction tx = Transaction.openRoot()) {
				inserted = inventory.insert(ItemResource.of(stack), stack.getCount(), tx);
				if (!simulate) {
					tx.commit();
				}
			}
			return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
		}
	}
}
