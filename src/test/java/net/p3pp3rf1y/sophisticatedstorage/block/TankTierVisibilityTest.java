package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankTierVisibilityTest {
	private MockedStatic<RegistryHelper> registryMock;

	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.GOLD_TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}

	@BeforeEach
	void mockRegistryAccess() {
		registryMock = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		registryMock.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
	}

	@AfterEach
	void closeRegistryMock() {
		registryMock.close();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2})
	void toggleTierVisiblityUpdatesTheWholeColumnFromAnySegment(int segment) {
		Fixture fixture = fixture();
		fixture.primary().configureGroup(3);
		Map<BlockPos, BlockState> originalStates = new HashMap<>();
		for (int index = 0; index < 3; index++) {
			BlockPos pos = BlockPos.ZERO.above(index);
			originalStates.put(pos, fixture.members.get(pos).getBlockState());
		}
		fixture.members.get(BlockPos.ZERO.above(segment)).toggleTierVisiblity();
		assertFalse(fixture.primary().shouldShowTier());
		originalStates.forEach((pos, state) -> assertEquals(state.setValue(TankBlock.SHOW_TIER, false), fixture.members.get(pos).getBlockState()));
	}

	@Test
	void toggleTierVisiblityRestoresTheTierModelAcrossTheWholeColumn() {
		Fixture fixture = fixture();
		fixture.primary().configureGroup(3);
		fixture.primary().toggleTierVisiblity();
		fixture.members.get(BlockPos.ZERO.above(2)).toggleTierVisiblity();
		assertTrue(fixture.primary().shouldShowTier());
		for (int index = 0; index < 3; index++) {
			assertTrue(fixture.members.get(BlockPos.ZERO.above(index)).getBlockState().getValue(TankBlock.SHOW_TIER));
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 3, 5})
	void configureGroupAppliesThePrimaryVisibilityToNewSegments(int height) {
		Fixture fixture = fixture();
		fixture.primary().configureGroup(1);
		fixture.primary().toggleTierVisiblity();
		fixture.primary().configureGroup(height);
		assertHiddenColumn(fixture, height);
	}

	@Test
	void changeTierPreservesHiddenTierAndContentsAcrossTheColumn() {
		Fixture fixture = fixture();
		fixture.primary().configureGroup(3);
		fixture.primary().toggleTierVisiblity();
		fill(fixture.primary(), 10000);
		boolean changed = fixture.primary().changeTier(ModBlocks.DIAMOND_TANK.get());
		assertTrue(changed);
		assertHiddenColumn(fixture, 3);
		assertTrue(fixture.primary().getBlockState().is(ModBlocks.DIAMOND_TANK.get()));
		assertEquals(10000, fixture.primary().getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@Test
	void restoreStackAppliesPortableTierVisibilityToEverySegment() {
		Fixture original = fixture();
		original.primary().configureGroup(3);
		original.primary().toggleTierVisiblity();
		ItemStack stack = original.primary().createPackedStack();
		Fixture restored = fixture();
		restored.primary().configureGroup(3);
		restored.primary().restoreStack(stack);
		assertHiddenColumn(restored, 3);
	}

	@SuppressWarnings("deprecation")
	@Test
	void onLoadUpdatesOldBlockStatesFromPersistedTierVisibility() {
		Fixture fixture = fixture();
		fixture.primary().configureGroup(3);
		fixture.primary().toggleTierVisiblity();
		for (int index = 0; index < 3; index++) {
			TankBlockEntity tank = fixture.members.get(BlockPos.ZERO.above(index));
			tank.setBlockState(tank.getBlockState().setValue(TankBlock.SHOW_TIER, true));
		}
		for (int index = 0; index < 3; index++) {
			fixture.members.get(BlockPos.ZERO.above(index)).onLoad();
		}
		assertHiddenColumn(fixture, 3);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2})
	void packSelectsTheOverlayAcrossTheWholeColumnWithoutChangingItsContentsOrTierVisibility(int segment) {
		Fixture fixture = fixture();
		fixture.primary().configureGroup(3);
		fixture.primary().toggleTierVisiblity();
		fill(fixture.primary(), 10000);
		Map<BlockPos, BlockState> originalStates = new HashMap<>();
		for (int index = 0; index < 3; index++) {
			BlockPos pos = BlockPos.ZERO.above(index);
			originalStates.put(pos, fixture.members.get(pos).getBlockState());
		}
		fixture.members.get(BlockPos.ZERO.above(segment)).pack();
		originalStates.forEach((pos, state) -> assertEquals(state.setValue(TankBlock.PACKED, true), fixture.members.get(pos).getBlockState()));
		assertTrue(fixture.primary().isPacked());
		assertEquals(10000, fixture.primary().getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
		assertHiddenColumn(fixture, 3);
	}

	@SuppressWarnings("deprecation")
	@Test
	void onLoadRestoresPackedModelsFromThePrimaryWithoutAddingInternalCaps() {
		Fixture fixture = fixture();
		fixture.primary().configureGroup(3);
		fixture.primary().pack();
		for (int index = 0; index < 3; index++) {
			TankBlockEntity tank = fixture.members.get(BlockPos.ZERO.above(index));
			tank.setBlockState(tank.getBlockState().setValue(TankBlock.PACKED, false));
		}
		for (int index = 0; index < 3; index++) {
			fixture.members.get(BlockPos.ZERO.above(index)).onLoad();
		}
		for (int index = 0; index < 3; index++) {
			BlockState state = fixture.members.get(BlockPos.ZERO.above(index)).getBlockState();
			assertTrue(state.getValue(TankBlock.PACKED));
			assertEquals(TankForm.at(index, 3), state.getValue(TankBlock.FORM));
		}
	}

	@Test
	void restoreStackUnpacksTheColumnAndRetainsHiddenTierAndFluid() {
		Fixture original = fixture();
		original.primary().configureGroup(3);
		original.primary().toggleTierVisiblity();
		fill(original.primary(), 10000);
		original.primary().pack();
		ItemStack stack = original.primary().createPackedStack();
		Fixture restored = fixture();
		restored.primary().configureGroup(3);
		restored.primary().restoreStack(stack);
		assertHiddenColumn(restored, 3);
		assertEquals(10000, restored.primary().getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
		for (int index = 0; index < 3; index++) {
			assertTrue(restored.members.get(BlockPos.ZERO.above(index)).getBlockState().getValue(TankBlock.PACKED));
		}
	}

	private static void fill(TankBlockEntity tank, int amount) {
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(amount,
					tank.getStorageWrapper().getTankFluidHandler().insert(0, FluidResource.of(new FluidStack(Fluids.WATER, amount)), amount, tx, true));
			tx.commit();
		}
	}

	private static void assertHiddenColumn(Fixture fixture, int height) {
		assertFalse(fixture.primary().shouldShowTier());
		for (int index = 0; index < height; index++) {
			BlockState state = fixture.members.get(BlockPos.ZERO.above(index)).getBlockState();
			assertFalse(state.getValue(TankBlock.SHOW_TIER));
			assertEquals(TankForm.at(index, height), state.getValue(TankBlock.FORM));
			assertEquals(Direction.EAST, state.getValue(TankBlock.FACING));
		}
	}

	@SuppressWarnings("deprecation")
	private static Fixture fixture() {
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		when(level.dimension()).thenReturn(Level.OVERWORLD);
		when(level.hasChunkAt(any())).thenReturn(true);
		Map<BlockPos, TankBlockEntity> members = new HashMap<>();
		when(level.getBlockEntity(any())).thenAnswer(call -> members.get(call.getArgument(0)));
		when(level.getBlockState(any())).thenAnswer(call -> {
			TankBlockEntity tank = members.get(call.getArgument(0));
			return tank == null ? Blocks.AIR.defaultBlockState() : tank.getBlockState();
		});
		when(level.setBlock(any(), any(), anyInt())).thenAnswer(call -> {
			BlockPos pos = call.getArgument(0);
			BlockState state = call.getArgument(1);
			TankBlockEntity tank = members.get(pos);
			if (!tank.getBlockState().is(state.getBlock())) {
				tank = new TankBlockEntity(pos, state);
				tank.setLevel(level);
				members.put(pos, tank);
			} else {
				tank.setBlockState(state);
			}
			return true;
		});
		for (int index = 0; index < TankTier.MAX_HEIGHT; index++) {
			BlockPos pos = BlockPos.ZERO.above(index);
			TankBlockEntity tank = new TankBlockEntity(pos, ModBlocks.GOLD_TANK.get().defaultBlockState().setValue(TankBlock.FACING, Direction.EAST));
			tank.setLevel(level);
			members.put(pos, tank);
		}
		return new Fixture(members);
	}

	private record Fixture(Map<BlockPos, TankBlockEntity> members) {
		TankBlockEntity primary() {
			return members.get(BlockPos.ZERO);
		}
	}
}
