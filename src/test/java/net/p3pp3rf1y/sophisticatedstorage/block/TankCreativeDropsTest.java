package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupManager;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TankCreativeDropsTest {
	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.GOLD_TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.STACK_UPGRADE_TIER_1.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.PUMP_UPGRADE.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void createPackedStackMarksOnlyPackedTanksAndPreservesTheirFluid(boolean packed) {
		try (Fixture fixture = new Fixture()) {
			fill(fixture.tank, 12000);
			if (packed) {
				fixture.tank.pack();
			}
			ItemStack stack = fixture.tank.createPackedStack();
			assertEquals(packed, stack.getOrDefault(ModDataComponents.PACKED, false));
			assertEquals(12000, fixture.tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
			assertEquals(12000, new TankItemStorageWrapper(stack).getTankFluidHandler().getFluidInTank(0).getAmount());
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1000, 40000})
	void onDestroyedByPlayerDropsOnlyFluidBearingTanksAndRetainsOnlyRequiredStackUpgrades(int amount) {
		try (Fixture fixture = new Fixture()) {
			fixture.tank.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			fixture.tank.getStorageWrapper().getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.PUMP_UPGRADE.get()));
			if (amount > 0) {
				fill(fixture.tank, amount);
			}
			boolean removed = fixture.breakByPlayer(true);
			assertTrue(removed);
			List<ItemStack> tanks = fixture.tankDrops();
			assertEquals(amount == 0 ? 0 : 1, tanks.size());
			assertEquals(1, fixture.countDrops(ModItems.PUMP_UPGRADE.get()));
			assertEquals(amount > 32000 ? 0 : 1, fixture.countDrops(ModItems.STACK_UPGRADE_TIER_1.get()));
			if (amount > 0) {
				TankItemStorageWrapper dropped = new TankItemStorageWrapper(tanks.getFirst());
				assertEquals(amount, dropped.getTankFluidHandler().getFluidInTank(0).getAmount());
				assertEquals(amount > 32000, !dropped.getUpgradeHandler().getStackInSlot(0).isEmpty());
				assertTrue(dropped.getUpgradeHandler().getStackInSlot(1).isEmpty());
			}
		}
	}

	@Test
	void playerWillDestroyReturnsTheOriginalStateAndEmitsTheNormalBreakEffect() {
		try (Fixture fixture = new Fixture()) {
			ServerPlayer player = mock(ServerPlayer.class);
			BlockState result = ModBlocks.GOLD_TANK.get().playerWillDestroy(fixture.level, BlockPos.ZERO, fixture.tank.getBlockState(), player);
			assertSame(fixture.tank.getBlockState(), result);
			verify(fixture.level).levelEvent(player, 2001, BlockPos.ZERO, Block.getId(fixture.tank.getBlockState()));
		}
	}

	@Test
	void onDestroyedByPlayerDoesNotDropAnEmptyUpperSegmentAndKeepsTheLowerGroupsContents() {
		try (Fixture fixture = new Fixture()) {
			BlockPos upperPos = BlockPos.ZERO.above();
			TankBlockEntity upper = new TankBlockEntity(upperPos, ModBlocks.GOLD_TANK.get().defaultBlockState());
			upper.setLevel(fixture.level);
			when(fixture.level.getBlockEntity(upperPos)).thenReturn(upper);
			fixture.tank.configureGroup(2);
			fixture.tank.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.PUMP_UPGRADE.get()));
			fill(fixture.tank, 1000);
			when(fixture.level.removeBlock(upperPos, false)).thenAnswer(invocation -> {
				upper.preRemoveSideEffects(upperPos, Blocks.AIR.defaultBlockState());
				return true;
			});
			ServerPlayer player = mock(ServerPlayer.class);
			when(player.isCreative()).thenReturn(true);
			ModBlocks.GOLD_TANK.get().playerWillDestroy(fixture.level, upperPos, upper.getBlockState(), player);
			boolean removed = fixture.level.removeBlock(upperPos, false);
			assertTrue(removed);
			assertTrue(fixture.drops.isEmpty());
			assertEquals(1, fixture.tank.getHeight());
			assertEquals(1000, fixture.tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
			assertTrue(fixture.tank.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.PUMP_UPGRADE));
		}
	}

	@Test
	void onDestroyedByPlayerStillDropsEmptyTanksInSurvival() {
		try (Fixture fixture = new Fixture()) {
			boolean removed = fixture.breakByPlayer(false);
			assertTrue(removed);
			assertEquals(1, fixture.tankDrops().size());
		}
	}

	@Test
	void onDestroyedByPlayerDoesNotLeaveCreativeSuppressionAfterRejectedRemoval() {
		try (Fixture fixture = new Fixture()) {
			doReturn(false).when(fixture.level).removeBlock(BlockPos.ZERO, false);
			boolean rejectedRemoval = fixture.breakByPlayer(true);
			ModBlocks.GOLD_TANK.get().playerWillDestroy(fixture.level, BlockPos.ZERO, fixture.tank.getBlockState(), mock(ServerPlayer.class));
			fixture.tank.preRemoveSideEffects(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
			assertFalse(rejectedRemoval);
			assertEquals(1, fixture.tankDrops().size());
		}
	}

	@Test
	void onDestroyedByPlayerEjectsUpgradesFromAnEmptyPackedTankWithoutDroppingTheTank() {
		try (Fixture fixture = new Fixture()) {
			fixture.tank.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			fixture.tank.getStorageWrapper().getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.PUMP_UPGRADE.get()));
			fixture.tank.pack();
			boolean removed = fixture.breakByPlayer(true);
			assertTrue(removed);
			assertTrue(fixture.tankDrops().isEmpty());
			assertEquals(1, fixture.countDrops(ModItems.STACK_UPGRADE_TIER_1.get()));
			assertEquals(1, fixture.countDrops(ModItems.PUMP_UPGRADE.get()));
		}
	}

	@Test
	void onDestroyedByPlayerDetachesAnEmptyLinkedPrimaryAndEjectsSharedUpgradesOnce() {
		try (Fixture fixture = new Fixture(); MockedStatic<LinkedStorageGroupsSavedData> groups = mockStatic(LinkedStorageGroupsSavedData.class)) {
			var savedData = mock(LinkedStorageGroupsSavedData.class);
			var manager = mock(LinkedStorageGroupManager.class);
			groups.when(() -> LinkedStorageGroupsSavedData.get(fixture.level)).thenReturn(savedData);
			when(savedData.manager()).thenReturn(manager);
			LinkedStorageEndpointData endpoint = new LinkedStorageEndpointData(UUID.randomUUID(), UUID.randomUUID());
			fixture.endpoint = endpoint;
			fixture.tank.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.PUMP_UPGRADE.get()));
			boolean removed = fixture.breakByPlayer(true);
			assertTrue(removed);
			assertTrue(fixture.tankDrops().isEmpty());
			assertEquals(1, fixture.countDrops(ModItems.PUMP_UPGRADE.get()));
			assertTrue(fixture.tank.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).isEmpty());
			verify(manager).discardEndpoint(endpoint.groupId(), endpoint.endpointId());
		}
	}

	private static void fill(TankBlockEntity tank, int amount) {
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(amount,
					tank.getStorageWrapper().getTankFluidHandler().insert(0, FluidResource.of(new FluidStack(Fluids.WATER, amount)), amount, tx, true));
			tx.commit();
		}
	}

	private static class Fixture implements AutoCloseable {
		private final ServerLevel level = mock(ServerLevel.class);
		private final TankBlockEntity tank;
		@Nullable
		private LinkedStorageEndpointData endpoint;
		private final List<ItemStack> drops = new ArrayList<>();
		private final MockedStatic<RegistryHelper> registry = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		private final MockedStatic<Block> blockDrops = mockStatic(Block.class, CALLS_REAL_METHODS);
		Fixture() {
			RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
			registry.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(registries));
			when(level.registryAccess()).thenReturn(registries);
			when(level.hasChunkAt(any())).thenReturn(true);
			when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
			tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.GOLD_TANK.get().defaultBlockState()) {
				@Override
				public @Nullable LinkedStorageEndpointData getLinkedStorageEndpointData() {
					return endpoint;
				}
				@Override
				public boolean isSecondaryLinkedStorageEndpoint(Level tankLevel) {
					return false;
				}
			};
			tank.setLevel(level);
			when(level.getBlockEntity(BlockPos.ZERO)).thenReturn(tank);
			when(level.removeBlock(BlockPos.ZERO, false)).thenAnswer(invocation -> {
				tank.preRemoveSideEffects(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
				return true;
			});
			blockDrops.when(() -> Block.popResource(eq(level), any(BlockPos.class), any(ItemStack.class))).thenAnswer(invocation -> {
				drops.add(((ItemStack) invocation.getArgument(2)).copy());
				return null;
			});
		}
		boolean breakByPlayer(boolean creative) {
			ServerPlayer player = mock(ServerPlayer.class);
			when(player.isCreative()).thenReturn(creative);
			ModBlocks.GOLD_TANK.get().playerWillDestroy(level, BlockPos.ZERO, tank.getBlockState(), player);
			return level.removeBlock(BlockPos.ZERO, false);
		}
		List<ItemStack> tankDrops() {
			return drops.stream().filter(stack -> stack.getItem() instanceof TankBlockItem).toList();
		}
		int countDrops(Item item) {
			return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
		}
		@Override
		public void close() {
			blockDrops.close();
			registry.close();
		}
	}
}
