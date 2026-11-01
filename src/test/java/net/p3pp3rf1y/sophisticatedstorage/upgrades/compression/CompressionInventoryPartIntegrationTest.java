package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Cross-component tests: real inventory, partitioner, tracker and void upgrade; recipe/world services are isolated. */
class CompressionInventoryPartIntegrationTest {
	private MockedStatic<RecipeHelper> recipes;

	@BeforeEach
	void setUpRecipes() {
		recipes = mockStatic(RecipeHelper.class);
		recipes.when(() -> RecipeHelper.getItemCompactingShapes(any(ItemStack.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
		recipes.when(() -> RecipeHelper.getUncompactingResult(any(ItemStack.class))).thenReturn(RecipeHelper.UncompactingResult.EMPTY);
	}

	@AfterEach
	void closeRecipes() {
		recipes.close();
	}

	@ParameterizedTest
	@MethodSource("insertItemOverflowParams")
	void insertItemOverflowAgreesWithSimulation(InsertItemOverflowParams params) {
		Fixture fixture = new Fixture(64, 63, 8);
		assertTrue(fixture.inventory.insertItem(2, new ItemStack(Items.IRON_NUGGET), false).isEmpty());
		assertEquals(0, fixture.inventory.getSlotStack(2).getCount());
		assertEquals(5760, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
		fixture.inventory.insertItem(2, new ItemStack(Items.IRON_NUGGET, 64), false);
		fixture.installVoid(params.type(), params.matches());
		List<Integer> notifications = new ArrayList<>();
		fixture.inventory.addListener(notifications::add);

		for (int attempt = 0; attempt < 3; attempt++) {
			assertEquals(params.matches() ? 0 : 1, fixture.insertOverflow(params.slotSpecific(), true).getCount());
		}
		assertTrue(notifications.isEmpty());
		assertEquals(5824, fixture.inventory.getStackInSlot(2).getCount());
		assertEquals(params.matches() ? 0 : 1, fixture.insertOverflow(params.slotSpecific(), false).getCount());
		fixture.assertTracking();
		assertEquals(5824, fixture.inventory.getStackInSlot(2).getCount());
	}

	private record InsertItemOverflowParams(VoidType type, boolean matches, boolean slotSpecific) {
	}

	private static List<InsertItemOverflowParams> insertItemOverflowParams() {
		List<InsertItemOverflowParams> params = new ArrayList<>();
		for (VoidType type : VoidType.values()) {
			for (boolean matches : new boolean[]{false, true}) {
				for (boolean slotSpecific : new boolean[]{false, true}) {
					params.add(new InsertItemOverflowParams(type, matches, slotSpecific));
				}
			}
		}
		return params;
	}

	@Test
	void insertItemUpdatesFullSiblingAndNonemptyCalculatedEmptyRawSlot() {
		Fixture fixture = new Fixture(64, 63, 8);

		fixture.inventory.insertItem(2, new ItemStack(Items.IRON_NUGGET), false);

		fixture.assertTracking();
		assertTrue(fixture.inventory.getSlotTracker().getFullStacks().contains(ItemStackKey.of(new ItemStack(Items.IRON_INGOT))));
		assertTrue(fixture.inventory.getSlotTracker().getPartialStacks().contains(ItemStackKey.of(new ItemStack(Items.IRON_NUGGET))));
		fixture.installVoid(VoidType.SLOT_OVERFLOW, true);
		assertTrue(fixture.inventory.insertItem(new ItemStack(Items.IRON_INGOT), true).isEmpty());
	}

	@Test
	void extractItemAndReinsertRefreshUnchangedRawSibling() {
		Fixture fixture = new Fixture(64, 64, 64);

		fixture.inventory.extractItem(1, 1, false);
		assertEquals(64, fixture.inventory.getSlotStack(1).getCount());
		assertEquals(55, fixture.inventory.getSlotStack(2).getCount());
		fixture.assertTracking();
		fixture.inventory.insertItem(2, new ItemStack(Items.IRON_NUGGET, 9), false);

		assertEquals(5824, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
	}

	@Test
	void extractItemByStackUsesCalculatedContentsAndCompacts() {
		Fixture fixture = new Fixture(1, 0, 0);

		assertEquals(9, fixture.inventory.extractItem(new ItemStack(Items.IRON_NUGGET, 9), true).getCount());
		assertEquals(81, fixture.inventory.getStackInSlot(2).getCount());
		assertEquals(9, fixture.inventory.extractItem(new ItemStack(Items.IRON_NUGGET, 9), false).getCount());

		assertEquals(0, fixture.inventory.getSlotStack(0).getCount());
		assertEquals(8, fixture.inventory.getSlotStack(1).getCount());
		assertEquals(72, fixture.inventory.getStackInSlot(2).getCount());
		assertTrue(fixture.inventory.getSlotTracker().getPartialStacks().contains(ItemStackKey.of(new ItemStack(Items.IRON_NUGGET))));
	}

	@Test
	void setStackInSlotTracksActualCountAfterFollowUpRefillsSelectedSlot() {
		Fixture fixture = new Fixture(64, 64, 64);

		fixture.inventory.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 639));

		assertEquals(640, fixture.inventory.getStackInSlot(1).getCount());
		fixture.assertTracking();
	}

	@Test
	void insertItemNotifiesOnlyAfterRawAndCalculatedContentsAndIndexesAgree() {
		Fixture fixture = new Fixture(64, 63, 8);
		AtomicBoolean reentered = new AtomicBoolean();
		List<Integer> notifications = new ArrayList<>();
		fixture.inventory.addListener(slot -> {
			notifications.add(slot);
			assertEquals(64, fixture.inventory.getSlotStack(1).getCount());
			assertEquals(0, fixture.inventory.getSlotStack(2).getCount());
			assertEquals(640, fixture.inventory.getStackInSlot(1).getCount());
			fixture.assertTracking();
			if (reentered.compareAndSet(false, true)) {
				fixture.inventory.insertItem(new ItemStack(Items.IRON_NUGGET), true);
			}
		});

		fixture.inventory.insertItem(2, new ItemStack(Items.IRON_NUGGET), false);

		assertEquals(Set.of(1, 2), Set.copyOf(notifications));
		assertEquals(2, notifications.size());
		assertTrue(reentered.get());
	}

	@Test
	void insertItemNotifiesTrackerKeysAfterAllCalculatedIndexesAreReady() {
		Fixture fixture = new Fixture(0, 0, 1);
		AtomicBoolean notified = new AtomicBoolean();
		fixture.inventory.registerTrackingListeners(key -> {
			assertEquals(82, fixture.inventory.getStackInSlot(2).getCount());
			fixture.assertTracking();
			assertEquals(1, fixture.inventory.extractItem(2, 1, true).getCount());
			notified.set(true);
		}, key -> {
		}, () -> {
		}, () -> {
		});

		fixture.inventory.insertItem(0, new ItemStack(Items.IRON_BLOCK), false);

		assertTrue(notified.get());
	}

	@Test
	void setStackInSlotNotifiesTrackerKeysAfterAllEmptyIndexesAreReady() {
		Fixture fixture = new Fixture(1, 0, 0);
		AtomicBoolean notified = new AtomicBoolean();
		fixture.inventory.registerTrackingListeners(key -> {
		}, key -> {
			for (int slot = 0; slot < 3; slot++) {
				assertTrue(fixture.inventory.getSlotStack(slot).isEmpty());
				assertTrue(fixture.inventory.getStackInSlot(slot).isEmpty());
			}
			assertTrue(fixture.inventory.getSlotTracker().getFullStacks().isEmpty());
			assertTrue(fixture.inventory.getSlotTracker().getPartialStacks().isEmpty());
			notified.set(true);
		}, () -> {
		}, () -> {
		});

		fixture.inventory.setStackInSlot(2, ItemStack.EMPTY);

		assertTrue(notified.get());
	}

	@Test
	void insertItemRejectsAdditionalContentsAndPreservesOversizedStacksAfterCapacityDecrease() {
		Fixture fixture = new Fixture(128, 8, 8);
		fixture.inventory.setBaseSlotLimit(128);
		fixture.inventory.setBaseSlotLimit(64);

		assertEquals(1, fixture.inventory.insertItem(2, new ItemStack(Items.IRON_NUGGET), false).getCount());

		assertEquals(128, fixture.inventory.getSlotStack(0).getCount());
		assertEquals(8, fixture.inventory.getSlotStack(1).getCount());
		assertEquals(8, fixture.inventory.getSlotStack(2).getCount());
		assertEquals(10448, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
	}

	@Test
	void onInitAndCapacityChangesPreserveContentsAndIndexes() {
		Fixture fixture = new Fixture(64, 64, 64);

		fixture.inventory.setBaseSlotLimit(256);
		fixture.part.onInit();

		assertEquals(71, fixture.inventory.getSlotStack(0).getCount());
		assertEquals(8, fixture.inventory.getSlotStack(1).getCount());
		assertEquals(1, fixture.inventory.getSlotStack(2).getCount());
		assertEquals(5824, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
	}

	private static class Fixture {
		private final InventoryHandler inventory;
		private final CompressionInventoryPart part;
		private final IStorageWrapper wrapper = mock(IStorageWrapper.class);
		private final UpgradeHandler upgrades = mock(UpgradeHandler.class);

		private Fixture(int blocks, int ingots, int nuggets) {
			SettingsHandler settings = mock(SettingsHandler.class);
			MemorySettingsCategory memory = new MemorySettingsCategory(() -> wrapper.getInventoryHandler(), new CompoundTag(), tag -> {
			});
			when(wrapper.getSettingsHandler()).thenReturn(settings);
			when(wrapper.getUpgradeHandler()).thenReturn(upgrades);
			when(settings.getTypeCategory(MemorySettingsCategory.class)).thenReturn(memory);
			StackUpgradeConfig config = mock(StackUpgradeConfig.class);
			when(config.canStackItem(any(Item.class))).thenReturn(true);
			inventory = new InventoryHandler(3, wrapper, new CompoundTag(), () -> {
			}, 64, config) {
				@Override
				protected boolean isAllowed(ItemStack stack) {
					return true;
				}
			};
			inventory.setPersistent(false);
			when(wrapper.getInventoryHandler()).thenReturn(inventory);
			inventory.setSlotStack(0, new ItemStack(Items.IRON_BLOCK, blocks));
			inventory.setSlotStack(1, new ItemStack(Items.IRON_INGOT, ingots));
			inventory.setSlotStack(2, new ItemStack(Items.IRON_NUGGET, nuggets));
			part = spy(new CompressionInventoryPart(inventory, new SlotRange(0, 3), () -> memory));
			doReturn(Optional.empty()).when(part).getCompressionResultFromConfig(any(ItemStack.class));
			doReturn(Optional.empty()).when(part).getDecompressionResultFromConfig(any(Item.class));
			doReturn(Optional.of(new CompressionUpgradeConfig.CompressionResult(new ItemStack(Items.IRON_BLOCK), 9))).when(part)
					.getCompressionResultFromConfig(argThat(stack -> stack.is(Items.IRON_INGOT)));
			doReturn(Optional.of(new CompressionUpgradeConfig.CompressionResult(new ItemStack(Items.IRON_INGOT), 9))).when(part)
					.getCompressionResultFromConfig(argThat(stack -> stack.is(Items.IRON_NUGGET)));
			doReturn(Optional.of(new CompressionUpgradeConfig.DecompressionResult(new ItemStack(Items.IRON_INGOT), 9))).when(part)
					.getDecompressionResultFromConfig(Items.IRON_BLOCK);
			doReturn(Optional.of(new CompressionUpgradeConfig.DecompressionResult(new ItemStack(Items.IRON_NUGGET), 9))).when(part)
					.getDecompressionResultFromConfig(Items.IRON_INGOT);
			inventory.getInventoryPartitioner().addInventoryPart(0, 3, part);
		}

		private ItemStack insertOverflow(boolean slotSpecific, boolean simulate) {
			ItemStack offered = new ItemStack(Items.IRON_NUGGET);
			return slotSpecific ? inventory.insertItem(2, offered, simulate) : inventory.insertItem(offered, simulate);
		}

		private void assertTracking() {
			for (int slot = 0; slot < 3; slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				ItemStackKey key = ItemStackKey.of(stack);
				boolean full = stack.getCount() >= inventory.getStackLimit(slot, stack);
				assertEquals(full, inventory.getSlotTracker().getFullStacks().contains(key), "Full slot " + slot);
				assertEquals(!full, inventory.getSlotTracker().getPartialStacks().contains(key), "Partial slot " + slot);
			}
		}

		private void installVoid(VoidType type, boolean matches) {
			VoidUpgradeItem item = mock(VoidUpgradeItem.class);
			when(item.getFilterSlotCount()).thenReturn(1);
			when(item.isVoidAlwaysEnabled()).thenReturn(true);
			ItemStack stack = mock(ItemStack.class);
			Map<Object, Object> components = new HashMap<>();
			when(stack.getItem()).thenReturn(item);
			when(stack.getOrDefault(anyDataComponentSupplier(), any()))
					.thenAnswer(invocation -> components.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
			doAnswer(invocation -> components.put(invocation.getArgument(0), invocation.getArgument(1))).when(stack).set(anySetDataComponentSupplier(), any());
			VoidUpgradeWrapper voidUpgrade = new VoidUpgradeWrapper(wrapper, stack, ignored -> {
			});
			voidUpgrade.setVoidType(type);
			voidUpgrade.getFilterLogic().setAllowList(!matches);
			when(upgrades.getTypeWrappers(VoidUpgradeItem.TYPE)).thenReturn(List.of(voidUpgrade));
			when(upgrades.getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class)).thenReturn(List.of(voidUpgrade));
			when(upgrades.getWrappersThatImplementFromMainStorage(IOverflowResponseUpgrade.class)).thenReturn(List.of(voidUpgrade));
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Supplier<? extends DataComponentType<? extends T>> anyDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<? extends T>>) any(Supplier.class);
	}

	@SuppressWarnings("unchecked")
	private static Supplier<? extends DataComponentType<Object>> anySetDataComponentSupplier() {
		return (Supplier<? extends DataComponentType<Object>>) any(Supplier.class);
	}
}
