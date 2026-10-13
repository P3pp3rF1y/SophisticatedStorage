package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ControllerDepositPlayerItemsTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		// Forge normally transforms CapabilityToken; supply capabilities for this plain JVM test.
		try (MockedStatic<CapabilityManager> manager = mockStatic(CapabilityManager.class)) {
			manager.when(() -> CapabilityManager.get(any(CapabilityToken.class))).thenAnswer(ignored -> mock(Capability.class));
			assertTrue(ForgeCapabilities.ITEM_HANDLER != null);
		}
	}

	@Test
	void depositPlayerItemsSkipsEmptySlotsBeforeConsultingEligibilityPredicates() {
		Player player = mock(Player.class);
		ItemStackHandler inventory = new ItemStackHandler(2);
		when(player.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)).thenReturn(LazyOptional.of(() -> inventory));
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(ItemStack.EMPTY);
		TestController controller = controller();

		controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);
		controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);

		verify(controller, never()).hasItem(any());
		verify(controller, never()).isMemorizedItem(any());
		verify(controller, never()).isFilterItem(any());
		verify(controller, never()).hasMatchingFilter(any());
		verify(controller, never()).insertItem(any(), anyBoolean(), anyBoolean());
	}

	@Test
	void depositPlayerItemsStillDepositsEligibleNonemptyStacks() {
		Player player = mock(Player.class);
		ItemStackHandler inventory = new ItemStackHandler(2);
		inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
		when(player.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)).thenReturn(LazyOptional.of(() -> inventory));
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(ItemStack.EMPTY);
		TestController controller = controller();

		controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);
		controller.depositPlayerItems(player, InteractionHand.MAIN_HAND);

		assertTrue(inventory.getStackInSlot(0).isEmpty());
		verify(controller).hasItem(Items.DIAMOND);
		verify(controller).insertItem(any(), eq(true), eq(false));
		verify(controller).insertItem(any(), eq(false), eq(false));
	}

	private TestController controller() {
		TestController controller = mock(TestController.class, CALLS_REAL_METHODS);
		ServerLevel level = mock(ServerLevel.class);
		when(level.getGameTime()).thenReturn(100L);
		doReturn(level).when(controller).getLevel();
		return controller;
	}

	private static class TestController extends ControllerBlockEntity {
		private TestController() {
			super(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
		}

		@Override
		protected boolean hasItem(Item item) {
			return true;
		}

		@Override
		protected boolean isMemorizedItem(ItemStack stack) {
			return true;
		}

		@Override
		protected boolean isFilterItem(Item item) {
			return true;
		}

		@Override
		public boolean hasMatchingFilter(ItemStack stack) {
			return true;
		}

		@Override
		protected ItemStack insertItem(ItemStack stack, boolean simulate, boolean insertIntoAnyEmpty) {
			return ItemStack.EMPTY;
		}
	}
}
