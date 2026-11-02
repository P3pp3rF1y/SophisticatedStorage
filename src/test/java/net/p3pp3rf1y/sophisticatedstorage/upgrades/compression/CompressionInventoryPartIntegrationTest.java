package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Cross-component coverage of the real inventory, partitioner, tracker and compression part. */
class CompressionInventoryPartIntegrationTest extends CompressionInventoryPartTestSupport {
	@ParameterizedTest
	@MethodSource("insertItemOverflowParams")
	void insertItemOverflowAgreesWithSimulation(InsertItemOverflowParams params) {
		Fixture fixture = new Fixture(64, 63, 8);
		assertEquals(1, fixture.insert(2, Items.IRON_NUGGET, 1, false));
		assertEquals(0, fixture.inventory.getInternalStack(2).getCount());
		assertEquals(5760, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
		fixture.insert(2, Items.IRON_NUGGET, 64, false);
		fixture.installVoid(params.type(), params.matches());
		List<Integer> notifications = new ArrayList<>();
		fixture.inventory.addListener(notifications::add);
		for (int attempt = 0; attempt < 3; attempt++) {
			assertEquals(params.matches() ? 1 : 0, fixture.insertOverflow(params.slotSpecific(), true));
		}
		assertTrue(notifications.isEmpty());
		assertEquals(5824, fixture.inventory.getStackInSlot(2).getCount());
		assertEquals(params.matches() ? 1 : 0, fixture.insertOverflow(params.slotSpecific(), false));
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
		assertEquals(1, fixture.insert(2, Items.IRON_NUGGET, 1, false));
		assertEquals(0, fixture.inventory.getInternalStack(2).getCount());
		assertEquals(5760, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
		assertTrue(fixture.inventory.getSlotTracker().getFullStacks().contains(ItemStackKey.of(new ItemStack(Items.IRON_INGOT))));
		assertTrue(fixture.inventory.getSlotTracker().getPartialStacks().contains(ItemStackKey.of(new ItemStack(Items.IRON_NUGGET))));
	}

	@Test
	void extractItemAndReinsertRefreshUnchangedRawSibling() {
		Fixture fixture = new Fixture(64, 64, 64);
		assertEquals(1, fixture.extract(1, Items.IRON_INGOT, 1, false));
		assertEquals(64, fixture.inventory.getInternalStack(1).getCount());
		assertEquals(55, fixture.inventory.getInternalStack(2).getCount());
		fixture.assertTracking();
		assertEquals(9, fixture.insert(2, Items.IRON_NUGGET, 9, false));
		assertEquals(5824, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
	}

	@Test
	void extractItemByResourceUsesCalculatedContentsAndCompacts() {
		Fixture fixture = new Fixture(1, 0, 0);
		assertEquals(9, fixture.extract(2, Items.IRON_NUGGET, 9, true));
		assertEquals(81, fixture.inventory.getStackInSlot(2).getCount());
		assertEquals(9, fixture.extract(2, Items.IRON_NUGGET, 9, false));
		assertEquals(0, fixture.inventory.getInternalStack(0).getCount());
		assertEquals(8, fixture.inventory.getInternalStack(1).getCount());
		assertEquals(72, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
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
			assertEquals(64, fixture.inventory.getInternalStack(1).getCount());
			assertEquals(0, fixture.inventory.getInternalStack(2).getCount());
			assertEquals(640, fixture.inventory.getStackInSlot(1).getCount());
			fixture.assertTracking();
			if (reentered.compareAndSet(false, true)) {
				assertEquals(5760, fixture.inventory.getStackInSlot(2).getCount());
			}
		});
		fixture.insert(2, Items.IRON_NUGGET, 1, false);
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
			notified.set(true);
		}, key -> {
		}, () -> {
		}, () -> {
		});
		fixture.insert(0, Items.IRON_BLOCK, 1, false);
		assertTrue(notified.get());
		fixture.assertTracking();
	}

	@Test
	void setStackInSlotNotifiesTrackerKeysAfterAllEmptyIndexesAreReady() {
		Fixture fixture = new Fixture(1, 0, 0);
		AtomicBoolean notified = new AtomicBoolean();
		fixture.inventory.registerTrackingListeners(key -> {
		}, key -> {
			for (int slot = 0; slot < 3; slot++) {
				assertTrue(fixture.inventory.getInternalStack(slot).isEmpty());
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
		assertEquals(0, fixture.insert(2, Items.IRON_NUGGET, 1, false));
		assertEquals(128, fixture.inventory.getInternalStack(0).getCount());
		assertEquals(8, fixture.inventory.getInternalStack(1).getCount());
		assertEquals(8, fixture.inventory.getInternalStack(2).getCount());
		assertEquals(10448, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
	}

	@Test
	void onInitAndCapacityChangesPreserveContentsAndIndexes() {
		Fixture fixture = new Fixture(64, 64, 64);
		fixture.inventory.setBaseSlotLimit(256);
		fixture.part.onInit();
		assertEquals(71, fixture.inventory.getInternalStack(0).getCount());
		assertEquals(8, fixture.inventory.getInternalStack(1).getCount());
		assertEquals(1, fixture.inventory.getInternalStack(2).getCount());
		assertEquals(5824, fixture.inventory.getStackInSlot(2).getCount());
		fixture.assertTracking();
	}

	@Test
	void insertItemAbortDoesNotPersistOrNotifyProposedCompression() {
		Fixture fixture = new Fixture(64, 63, 8);
		fixture.inventory.setPersistent(true);
		List<Integer> notifications = new ArrayList<>();
		fixture.inventory.addListener(notifications::add);
		AtomicBoolean trackingNotification = new AtomicBoolean();
		fixture.inventory.registerTrackingListeners(key -> trackingNotification.set(true), key -> trackingNotification.set(true),
				() -> trackingNotification.set(true), () -> trackingNotification.set(true));
		int savesBefore = fixture.saves;
		List<ItemStack> persisted = fixture.contents.inventory().stacks().stream().map(ItemStack::copy).toList();
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(1, fixture.inventory.insert(2, ItemResource.of(Items.IRON_NUGGET), 1, tx));
			assertEquals(5760, fixture.inventory.getStackInSlot(2).getCount());
			fixture.assertTracking();
			assertEquals(savesBefore, fixture.saves);
			assertTrue(notifications.isEmpty());
			assertFalse(trackingNotification.get());
		}
		assertEquals(63, fixture.inventory.getInternalStack(1).getCount());
		assertEquals(8, fixture.inventory.getInternalStack(2).getCount());
		assertEquals(savesBefore, fixture.saves);
		assertTrue(notifications.isEmpty());
		assertFalse(trackingNotification.get());
		for (int slot = 0; slot < 3; slot++) {
			assertEquals(persisted.get(slot).getCount(), fixture.contents.inventory().stacks().get(slot).getCount());
		}
		fixture.assertTracking();
	}

	@Test
	void insertItemNestedAbortPublishesOnlyCommittedCompressionOnce() {
		Fixture fixture = new Fixture(64, 63, 8);
		fixture.inventory.setPersistent(true);
		List<Integer> notifications = new ArrayList<>();
		fixture.inventory.addListener(slot -> {
			assertEquals(5760, fixture.inventory.getStackInSlot(2).getCount());
			assertEquals(64, fixture.contents.inventory().stacks().get(1).getCount());
			notifications.add(slot);
		});
		int savesBefore = fixture.saves;
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(1, fixture.inventory.insert(2, ItemResource.of(Items.IRON_NUGGET), 1, tx));
			try (Transaction nested = Transaction.open(tx)) {
				assertEquals(1, fixture.inventory.extract(2, ItemResource.of(Items.IRON_NUGGET), 1, nested));
			}
			assertEquals(5760, fixture.inventory.getStackInSlot(2).getCount());
			assertEquals(savesBefore, fixture.saves);
			assertTrue(notifications.isEmpty());
			tx.commit();
		}
		assertEquals(savesBefore + 1, fixture.saves);
		assertEquals(Set.of(1, 2), Set.copyOf(notifications));
		assertEquals(2, notifications.size());
		fixture.assertTracking();
	}

	@Test
	void extractItemRootAbortRestoresKeysAndEmptySlotsSilently() {
		Fixture fixture = new Fixture(1, 0, 0);
		List<String> events = fixture.observeTracking();
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(81, fixture.inventory.extract(2, ItemResource.of(Items.IRON_NUGGET), 81, tx));
			assertEquals(Set.of(0, 1, 2), fixture.inventory.getSlotTracker().getEmptySlots());
			assertTrue(fixture.inventory.getSlotTracker().getPartialStacks().isEmpty());
			assertTrue(events.isEmpty());
		}
		assertEquals(81, fixture.inventory.getStackInSlot(2).getCount());
		assertTrue(fixture.inventory.getSlotTracker().getEmptySlots().isEmpty());
		fixture.assertTracking();
		assertTrue(events.isEmpty());
	}

	@Test
	void insertItemRootAbortRestoresInitiallyEmptyTopologySilently() {
		Fixture fixture = new Fixture(0, 0, 0);
		List<String> events = fixture.observeTracking();
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(1, fixture.inventory.insert(0, ItemResource.of(Items.IRON_BLOCK), 1, tx));
			assertTrue(fixture.inventory.getSlotTracker().getEmptySlots().isEmpty());
			fixture.assertTracking();
			assertTrue(events.isEmpty());
		}
		assertEquals(Set.of(0, 1, 2), fixture.inventory.getSlotTracker().getEmptySlots());
		assertTrue(fixture.inventory.getSlotTracker().getPartialStacks().isEmpty());
		assertTrue(events.isEmpty());
	}

	@Test
	void extractItemNestedAbortDiscardsEmptyBoundaryNotificationsBeforeOuterCommit() {
		Fixture fixture = new Fixture(1, 0, 0);
		List<String> events = fixture.observeTracking();
		try (Transaction outer = Transaction.openRoot()) {
			assertEquals(1, fixture.inventory.insert(0, ItemResource.of(Items.IRON_BLOCK), 1, outer));
			try (Transaction nested = Transaction.open(outer)) {
				assertEquals(162, fixture.inventory.extract(2, ItemResource.of(Items.IRON_NUGGET), 162, nested));
				assertEquals(Set.of(0, 1, 2), fixture.inventory.getSlotTracker().getEmptySlots());
			}
			assertEquals(162, fixture.inventory.getStackInSlot(2).getCount());
			assertTrue(fixture.inventory.getSlotTracker().getEmptySlots().isEmpty());
			fixture.assertTracking();
			assertTrue(events.isEmpty());
			outer.commit();
		}
		assertTrue(events.isEmpty());
	}

	@Test
	void insertItemFollowUpUsesProvisionalAccessibleSlotsAfterTopologyChanges() {
		Fixture fixture = new Fixture(0, 0, 0);
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(1, fixture.inventory.insert(0, ItemResource.of(Items.CLAY_BALL), 1, tx));
			assertTrue(fixture.inventory.getSlotTracker().getEmptySlots().isEmpty());
			assertEquals(0, fixture.inventory.insert(ItemResource.of(Items.IRON_NUGGET), 1, tx));
			assertEquals(1, fixture.inventory.extract(0, ItemResource.of(Items.CLAY_BALL), 1, tx));
			assertEquals(Set.of(0, 1, 2), fixture.inventory.getSlotTracker().getEmptySlots());
			assertEquals(1, fixture.inventory.insert(2, ItemResource.of(Items.IRON_NUGGET), 1, tx));
			assertEquals(Set.of(0, 1), fixture.inventory.getSlotTracker().getEmptySlots());
			assertEquals(1, fixture.inventory.insert(ItemResource.of(Items.IRON_BLOCK), 1, tx));
			assertEquals(82, fixture.inventory.getStackInSlot(2).getCount());
			assertTrue(fixture.inventory.getSlotTracker().getEmptySlots().isEmpty());
			tx.commit();
		}
		fixture.assertTracking();
	}

	private static class Fixture {
		private final ContainerContents contents = new ContainerContents();
		private int saves;
		private final IStorageWrapper wrapper = mock(IStorageWrapper.class);
		private final UpgradeHandler upgrades = mock(UpgradeHandler.class);
		private final InventoryHandler inventory;
		private final CompressionInventoryPart part;

		private Fixture(int blocks, int ingots, int nuggets) {
			SettingsHandler settings = mock(SettingsHandler.class);
			MemorySettingsCategory memory = new MemorySettingsCategory(wrapper::getInventoryHandler, new MemorySettingsCategoryData(), () -> {
			});
			when(wrapper.getSettingsHandler()).thenReturn(settings);
			when(wrapper.getUpgradeHandler()).thenReturn(upgrades);
			when(settings.getTypeCategory(MemorySettingsCategory.class)).thenReturn(memory);
			StackUpgradeConfig config = mock(StackUpgradeConfig.class);
			when(config.canStackItem(any(Item.class))).thenReturn(true);
			inventory = new InventoryHandler(3, wrapper, contents, () -> saves++, 64, config) {
				@Override
				protected boolean isAllowed(ItemResource resource) {
					return true;
				}
			};
			inventory.setPersistent(false);
			when(wrapper.getInventoryHandler()).thenReturn(inventory);
			inventory.setStackInSlotInternal(0, new ItemStack(Items.IRON_BLOCK, blocks));
			inventory.setStackInSlotInternal(1, new ItemStack(Items.IRON_INGOT, ingots));
			inventory.setStackInSlotInternal(2, new ItemStack(Items.IRON_NUGGET, nuggets));
			part = new CompressionInventoryPart(inventory, new SlotRange(0, 3), () -> memory) {
				@Override
				Optional<CompressionUpgradeConfig.CompressionResult> getCompressionResultFromConfig(ItemStack stack) {
					return stack.is(Items.IRON_INGOT)
							? Optional.of(new CompressionUpgradeConfig.CompressionResult(new ItemStack(Items.IRON_BLOCK), 9))
							: stack.is(Items.IRON_NUGGET)
									? Optional.of(new CompressionUpgradeConfig.CompressionResult(new ItemStack(Items.IRON_INGOT), 9))
									: Optional.empty();
				}

				@Override
				Optional<CompressionUpgradeConfig.DecompressionResult> getDecompressionResultFromConfig(Item item) {
					return item == Items.IRON_BLOCK
							? Optional.of(new CompressionUpgradeConfig.DecompressionResult(new ItemStack(Items.IRON_INGOT), 9))
							: item == Items.IRON_INGOT
									? Optional.of(new CompressionUpgradeConfig.DecompressionResult(new ItemStack(Items.IRON_NUGGET), 9))
									: Optional.empty();
				}
			};
			inventory.getInventoryPartitioner().addInventoryPart(0, 3, part);
		}

		private List<String> observeTracking() {
			List<String> events = new ArrayList<>();
			inventory.registerTrackingListeners(key -> events.add("add"), key -> events.add("remove"), () -> events.add("first-empty"),
					() -> events.add("last-empty"));
			return events;
		}

		private int insert(int slot, Item item, int amount, boolean simulate) {
			try (Transaction tx = Transaction.openRoot()) {
				int inserted = inventory.insert(slot, ItemResource.of(new ItemStack(item)), amount, tx);
				if (!simulate) {
					tx.commit();
				}
				return inserted;
			}
		}

		private int extract(int slot, Item item, int amount, boolean simulate) {
			try (Transaction tx = Transaction.openRoot()) {
				int extracted = inventory.extract(slot, ItemResource.of(new ItemStack(item)), amount, tx);
				if (!simulate) {
					tx.commit();
				}
				return extracted;
			}
		}

		private void assertTracking() {
			for (int slot = 0; slot < 3; slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (stack.isEmpty()) {
					continue;
				}
				ItemStackKey key = ItemStackKey.of(stack);
				boolean full = stack.getCount() >= part.getSlotLimit(slot);
				assertEquals(full, inventory.getSlotTracker().getFullStacks().contains(key), "Full slot " + slot);
				assertEquals(!full, inventory.getSlotTracker().getPartialStacks().contains(key), "Partial slot " + slot);
			}
		}

		private int insertOverflow(boolean slotSpecific, boolean simulate) {
			return slotSpecific ? insert(2, Items.IRON_NUGGET, 1, simulate) : insertAcrossSlots(Items.IRON_NUGGET, 1, simulate);
		}

		private int insertAcrossSlots(Item item, int amount, boolean simulate) {
			try (Transaction tx = Transaction.openRoot()) {
				int inserted = inventory.insert(ItemResource.of(new ItemStack(item)), amount, tx);
				if (!simulate) {
					tx.commit();
				}
				return inserted;
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
			inventory.setBaseSlotLimit(64); // upgrade refresh invalidates the 26.1 cached void-upgrade lookup
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
