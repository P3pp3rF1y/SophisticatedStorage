package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TankRemovalTest {
	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.GOLD_TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.STACK_UPGRADE_TIER_1.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.PUMP_UPGRADE.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.BUCKET.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 16).build());
		Items.WATER_BUCKET.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 1).build());
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2})
	void onSegmentRemovedDropsWholeGroupWhenFluidWouldExceedSplitTankCapacity(int index) {
		try (Fixture fixture = new Fixture()) {
			TankStorageWrapper storage = fixture.primary.getStorageWrapper();
			storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.PUMP_UPGRADE.get()));
			storage.getContainerInventory().set(0, ItemResource.of(Items.BUCKET), 4);
			storage.getContainerInventory().set(2, ItemResource.of(Items.WATER_BUCKET), 1);
			fixture.primary.setCustomName(Component.literal("Preserved column"));
			FluidStack fluid = new FluidStack(Fluids.WATER, storage.getCapacity());
			CompoundTag identity = new CompoundTag();
			identity.putString("identity", "component-bearing water");
			fluid.set(DataComponents.CUSTOM_DATA, CustomData.of(identity));
			fill(storage.getTankFluidHandler(), fluid);
			fixture.primary.toggleLock();
			fixture.remove(index, Blocks.AIR.defaultBlockState());
			assertTrue(fixture.segments.isEmpty());
			assertEquals(1, fixture.drops.size());
			ItemStack drop = fixture.drops.getFirst();
			assertEquals(4, drop.get(ModDataComponents.TANK_CONTAINERS).getStackInSlot(0).getCount());
			assertEquals(3, drop.get(ModDataComponents.TANK_HEIGHT));
			assertEquals(Component.literal("Preserved column"), drop.get(DataComponents.CUSTOM_NAME));
			assertTrue(drop.get(ModDataComponents.LOCKED));
			TankItemStorageWrapper restored = new TankItemStorageWrapper(drop);
			assertEquals(fluid.getAmount(), restored.getTankFluidHandler().getFluidInTank(0).getAmount());
			assertTrue(FluidStack.isSameFluidSameComponents(fluid, restored.getTankFluidHandler().getFluidInTank(0)));
			assertEquals(storage.getCapacity(), restored.getCapacity());
			assertTrue(restored.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_TIER_1));
			assertTrue(restored.getUpgradeHandler().getStackInSlot(1).is(ModItems.PUMP_UPGRADE));
			assertEquals(4, restored.getContainerInventory().getAmountAsLong(0));
			assertTrue(restored.getContainerInventory().getResource(2).toStack(1).is(Items.WATER_BUCKET));
		}
	}

	@Test
	void onSegmentRemovedKeepsSafeSplitSharesInsteadOfDroppingTheWholeGroup() {
		try (Fixture fixture = new Fixture()) {
			fill(fixture.primary.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 35000));
			fixture.remove(1, Blocks.AIR.defaultBlockState());
			assertEquals(2, fixture.segments.size());
			assertEquals(1, fixture.primary.getHeight());
			assertEquals(32000, fixture.primary.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
			TankBlockEntity upper = fixture.segments.get(BlockPos.ZERO.above(2));
			assertEquals(1, upper.getHeight());
			assertTrue(upper.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).isEmpty());
			assertEquals(1, fixture.drops.size());
			assertEquals(1, fixture.drops.getFirst().getOrDefault(ModDataComponents.TANK_HEIGHT, 1));
			assertEquals(3000, new TankItemStorageWrapper(fixture.drops.getFirst()).getTankFluidHandler().getFluidInTank(0).getAmount());
		}
	}

	@Test
	void onSegmentRemovedPreservesTheReplacementBlockDuringWholeGroupRemoval() {
		try (Fixture fixture = new Fixture()) {
			fixture.primary.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			fill(fixture.primary.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 192000));
			fixture.remove(1, Blocks.STONE.defaultBlockState());
			assertTrue(fixture.segments.isEmpty());
			assertTrue(fixture.states.get(BlockPos.ZERO.above()).is(Blocks.STONE));
			assertEquals(1, fixture.drops.size());
			assertEquals(192000, new TankItemStorageWrapper(fixture.drops.getFirst()).getTankFluidHandler().getFluidInTank(0).getAmount());
		}
	}

	private static void fill(TankFluidHandler handler, FluidStack fluid) {
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(fluid.getAmount(), handler.insert(0, FluidResource.of(fluid), fluid.getAmount(), tx, true));
			tx.commit();
		}
	}

	private static class Fixture implements AutoCloseable {
		private final ServerLevel level = mock(ServerLevel.class);
		private final Map<BlockPos, TankBlockEntity> segments = new HashMap<>();
		private final Map<BlockPos, BlockState> states = new HashMap<>();
		private final List<ItemStack> drops = new ArrayList<>();
		private final MockedStatic<RegistryHelper> registry = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		private final MockedStatic<Block> blockDrops = mockStatic(Block.class, CALLS_REAL_METHODS);
		private final TankBlockEntity primary;

		Fixture() {
			RegistryAccess registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
			registry.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(registries));
			when(level.registryAccess()).thenReturn(registries);
			when(level.hasChunkAt(any())).thenReturn(true);
			when(level.getBlockEntity(any())).thenAnswer(invocation -> segments.get(invocation.getArgument(0)));
			when(level.getBlockState(any())).thenAnswer(invocation -> states.getOrDefault(invocation.getArgument(0), Blocks.AIR.defaultBlockState()));
			when(level.setBlock(any(), any(), anyInt())).thenAnswer(invocation -> {
				BlockPos pos = invocation.getArgument(0);
				BlockState state = invocation.getArgument(1);
				states.put(pos, state);
				segments.get(pos).setBlockState(state);
				return true;
			});
			when(level.removeBlock(any(), eq(false))).thenAnswer(invocation -> {
				BlockPos pos = invocation.getArgument(0);
				remove(pos.getY(), Blocks.AIR.defaultBlockState());
				return true;
			});
			blockDrops.when(() -> Block.popResource(eq(level), any(BlockPos.class), any(ItemStack.class))).thenAnswer(invocation -> {
				drops.add(((ItemStack) invocation.getArgument(2)).copy());
				return null;
			});
			for (int index = 0; index < 3; index++) {
				BlockPos pos = BlockPos.ZERO.above(index);
				TankBlockEntity tank = new TankBlockEntity(pos, ModBlocks.GOLD_TANK.get().defaultBlockState());
				tank.setLevel(level);
				segments.put(pos, tank);
				states.put(pos, tank.getBlockState());
			}
			primary = segments.get(BlockPos.ZERO);
			primary.configureGroup(3);
		}

		void remove(int index, BlockState replacement) {
			BlockPos pos = BlockPos.ZERO.above(index);
			states.put(pos, replacement);
			TankBlockEntity segment = segments.get(pos);
			if (segment != null) {
				segment.preRemoveSideEffects(pos, replacement);
				segments.remove(pos);
			}
		}

		@Override
		public void close() {
			blockDrops.close();
			registry.close();
		}
	}
}
