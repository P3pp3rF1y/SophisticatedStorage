package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ControllerDepositPlayerItemsTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : Set.of(Items.DIAMOND, Items.GOLD_INGOT)) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	@Test
	void depositPlayerItemsKeepsEarlierPlayerItemsWhenLaterInsertionThrows() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND, 7), new ItemStack(Items.GOLD_INGOT, 2));
		fixture.controller.failOnGold = true;

		assertThrows(IllegalArgumentException.class, () -> fixture.controller.depositPlayerItems(fixture.player, InteractionHand.MAIN_HAND));

		assertEquals(7, fixture.items.get(0).getCount());
		assertEquals(2, fixture.items.get(1).getCount());
		assertEquals(0, fixture.controller.storage.getAmountAsLong(0));
		verify(fixture.inventory, never()).removeItem(anyInt(), anyInt());
	}

	@Test
	void depositPlayerItemsSkipsEmptySlotsAndRemovesOnlyCommittedAmounts() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND, 7), ItemStack.EMPTY);
		ItemStack original = fixture.items.getFirst();
		ItemStacksResourceHandler storage = new ItemStacksResourceHandler(1) {
			@Override
			protected int getCapacity(int index, ItemResource resource) {
				return 4;
			}
			@Override
			protected void onContentsChanged(int index, ItemStack previous) {
				assertEquals(7, original.getCount(), "Player removal must follow the storage commit");
			}
		};
		fixture.controller.storage = storage;

		assertDoesNotThrow(() -> fixture.controller.depositPlayerItems(fixture.player, InteractionHand.MAIN_HAND));

		assertEquals(3, fixture.items.get(0).getCount());
		assertEquals(4, fixture.controller.storage.getAmountAsLong(0));
		verify(fixture.inventory).removeItem(0, 4);
		verify(fixture.inventory, never()).removeItem(1, 0);
	}

	@Test
	void depositPlayerItemsCommitsBeforeRemovingTheHeldStackOnSingleClick() {
		Fixture fixture = fixture(new ItemStack(Items.DIAMOND, 7));
		when(fixture.level.getGameTime()).thenReturn(200L);
		when(fixture.player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(fixture.items.getFirst());
		Player player = fixture.player;
		ItemStacksResourceHandler storage = new ItemStacksResourceHandler(1) {
			@Override
			protected void onContentsChanged(int index, ItemStack previous) {
				verify(player, never()).setItemInHand(any(), any());
			}
		};
		fixture.controller.storage = storage;

		fixture.controller.depositPlayerItems(fixture.player, InteractionHand.MAIN_HAND);

		assertEquals(7, fixture.controller.storage.getAmountAsLong(0));
		verify(fixture.player).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	private Fixture fixture(ItemStack... stacks) {
		NonNullList<ItemStack> items = NonNullList.withSize(stacks.length, ItemStack.EMPTY);
		for (int slot = 0; slot < stacks.length; slot++) {
			items.set(slot, stacks[slot]);
		}
		Player player = mock(Player.class);
		Inventory inventory = mock(Inventory.class);
		when(player.getInventory()).thenReturn(inventory);
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(ItemStack.EMPTY);
		when(inventory.getNonEquipmentItems()).thenReturn(items);
		when(inventory.getItem(anyInt())).thenAnswer(call -> items.get(call.getArgument(0)));
		when(inventory.removeItem(anyInt(), anyInt())).thenAnswer(call -> items.get(call.getArgument(0)).split(call.getArgument(1)));
		ServerLevel level = mock(ServerLevel.class);
		when(level.getGameTime()).thenReturn(100L);
		TestController controller = mock(TestController.class, CALLS_REAL_METHODS);
		doReturn(level).when(controller).getLevel();
		controller.storage = new ItemStacksResourceHandler(2);
		controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);
		return new Fixture(controller, player, inventory, items, level);
	}

	private record Fixture(TestController controller, Player player, Inventory inventory, NonNullList<ItemStack> items, ServerLevel level) {
	}

	private static class TestController extends ControllerBlockEntity {
		private ItemStacksResourceHandler storage;
		private boolean failOnGold;

		private TestController() {
			super(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
		}

		@Override
		protected boolean hasItem(Item item) {
			return item != Items.AIR;
		}

		@Override
		protected boolean isMemorizedItem(ItemStack stack) {
			return false;
		}

		@Override
		protected boolean isFilterItem(Item item) {
			return false;
		}

		@Override
		public boolean hasMatchingFilter(ItemStack stack, TransactionContext tx) {
			return storage.insert(0, ItemResource.of(stack), 1, tx) == 1;
		}

		@Override
		protected int insertItem(ItemStack stack, TransactionContext tx, boolean insertIntoAnyEmpty) {
			if (failOnGold && stack.is(Items.GOLD_INGOT)) {
				throw new IllegalArgumentException("Injected later insertion failure");
			}
			return storage.insert(stack.is(Items.GOLD_INGOT) ? 1 : 0, ItemResource.of(stack), stack.getCount(), tx);
		}
	}
}
