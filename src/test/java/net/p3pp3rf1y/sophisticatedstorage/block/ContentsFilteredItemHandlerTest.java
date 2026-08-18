package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContentsFilteredItemHandlerTest {
	@Test
	void insertRejectsUnmemorizedSlotWhenMatchingMemorySlotIsRequired() {
		ITrackedContentsItemResourceHandler itemHandler = mock(ITrackedContentsItemResourceHandler.class);
		ISlotTracker slotTracker = mock(ISlotTracker.class);
		MemorySettingsCategory memorySettings = mock(MemorySettingsCategory.class);
		ItemResource resource = mock(ItemResource.class);
		Item item = mock(Item.class);
		when(resource.getItem()).thenReturn(item);
		when(slotTracker.getItems()).thenReturn(Set.of(item));
		when(memorySettings.isSlotSelected(1)).thenReturn(false);
		ContentsFilteredItemHandler handler = new ContentsFilteredItemHandler(() -> itemHandler, () -> slotTracker, () -> memorySettings, true);

		int inserted;
		try (Transaction tx = Transaction.openRoot()) {
			inserted = handler.insert(1, resource, 1, tx);
		}

		assertEquals(0, inserted);
		verifyNoInteractions(itemHandler);
	}

	@Test
	void insertAllowsMatchingMemorizedSlotWhenMatchingMemorySlotIsRequired() {
		ITrackedContentsItemResourceHandler itemHandler = mock(ITrackedContentsItemResourceHandler.class);
		ISlotTracker slotTracker = mock(ISlotTracker.class);
		MemorySettingsCategory memorySettings = mock(MemorySettingsCategory.class);
		ItemResource resource = mock(ItemResource.class);
		Item item = mock(Item.class);
		when(resource.getItem()).thenReturn(item);
		when(slotTracker.getItems()).thenReturn(Set.of(item));
		when(memorySettings.isSlotSelected(0)).thenReturn(true);
		when(memorySettings.matchesFilter(0, resource)).thenReturn(true);
		ContentsFilteredItemHandler handler = new ContentsFilteredItemHandler(() -> itemHandler, () -> slotTracker, () -> memorySettings, true);

		int inserted;
		try (Transaction tx = Transaction.openRoot()) {
			when(itemHandler.insert(0, resource, 1, tx)).thenReturn(1);

			inserted = handler.insert(0, resource, 1, tx);

			verify(itemHandler).insert(0, resource, 1, tx);
		}

		assertEquals(1, inserted);
	}
}
