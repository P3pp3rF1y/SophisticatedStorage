package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankLightingTest {
	@BeforeAll
	static void bootstrap() {
		StorageTankTestBootstrap.bootstrapMinecraft();
		initializeVanillaFluidTypes();
	}

	static void initializeVanillaFluidTypes() {
		try {
			Field value = RegistryObject.class.getDeclaredField("value");
			value.setAccessible(true);
			value.set(ForgeMod.WATER_TYPE, new FluidType(FluidType.Properties.create().sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
					.sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
				@Override
				public String getDescriptionId() {
					return "block.minecraft.water";
				}
			});
			value.set(ForgeMod.LAVA_TYPE, new FluidType(FluidType.Properties.create().lightLevel(15)
					.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to initialize vanilla fluid types in plain JVM", e);
		}
	}

	@AfterAll
	static void clearConfig() {
		StorageTankTestBootstrap.clearConfig();
	}

	@Test
	void tickDoesNotEmitLightForSimulatedFluid() {
		Fixture fixture = fixture();
		fixture.fluid().fill(new FluidStack(Fluids.LAVA, 16001), IFluidHandler.FluidAction.SIMULATE);

		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);

		assertTrue(fixture.fluid().getFluidInTank(0).isEmpty());
		assertTrue(fixture.segments.values().stream().allMatch(segment -> segment.getFluidLightEmission() == 0));
	}

	@ParameterizedTest
	@ValueSource(ints = {16001, 48000})
	void tickEmitsLightOnlyFromOccupiedSegments(int amount) {
		Fixture fixture = fixture();
		fixture.fluid().fill(new FluidStack(Fluids.LAVA, amount), IFluidHandler.FluidAction.EXECUTE);

		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);

		assertEquals(15, ModBlocks.IRON_TANK.get().getLightEmission(fixture.primary.getBlockState(), fixture.level, BlockPos.ZERO));
		assertEquals(15, fixture.level.getBlockState(BlockPos.ZERO.above()).getValue(TankBlock.LIGHT_LEVEL));
		assertEquals(amount == 48000 ? 15 : 0, fixture.level.getBlockState(BlockPos.ZERO.above(2)).getValue(TankBlock.LIGHT_LEVEL));
		assertEquals(15, fixture.segments.get(BlockPos.ZERO.above()).getFluidLightEmission());
		assertEquals(amount == 48000 ? 15 : 0, fixture.segments.get(BlockPos.ZERO.above(2)).getFluidLightEmission());
		verify(fixture.lights).checkBlock(BlockPos.ZERO.above());
		assertEquals(15, fixture.primary.getFluidLightEmission());
	}

	@Test
	void tickClearsTheSegmentLightWhenFluidDropsBelowItsBoundary() {
		Fixture fixture = fixture();
		fixture.fluid().fill(new FluidStack(Fluids.LAVA, 16001), IFluidHandler.FluidAction.EXECUTE);
		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);
		fixture.fluid().drain(1, IFluidHandler.FluidAction.EXECUTE);

		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);

		assertEquals(0, fixture.segments.get(BlockPos.ZERO.above()).getFluidLightEmission());
		assertEquals(0, fixture.level.getBlockState(BlockPos.ZERO.above()).getValue(TankBlock.LIGHT_LEVEL));
		assertEquals(15, fixture.segments.get(BlockPos.ZERO).getFluidLightEmission());
		verify(fixture.lights, atLeastOnce()).checkBlock(BlockPos.ZERO.above());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void tickClearsLightingAfterPackingOrReplacingLavaWithWater(boolean packed) {
		Fixture fixture = fixture();
		fixture.fluid().fill(new FluidStack(Fluids.LAVA, 48000), IFluidHandler.FluidAction.EXECUTE);
		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);
		if (packed) {
			fixture.primary.pack();
		} else {
			fixture.fluid().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
			fixture.fluid().fill(new FluidStack(Fluids.WATER, 48000), IFluidHandler.FluidAction.EXECUTE);
		}

		TankBlockEntity.tick(fixture.level, BlockPos.ZERO, fixture.primary);

		assertEquals(3, fixture.segments.size());
		assertTrue(fixture.segments.values().stream().allMatch(segment -> segment.getFluidLightEmission() == 0));
		assertTrue(fixture.segments.keySet().stream().allMatch(pos -> fixture.level.getBlockState(pos).getValue(TankBlock.LIGHT_LEVEL) == 0));
		for (int index = 0; index < 3; index++) {
			verify(fixture.lights, atLeastOnce()).checkBlock(BlockPos.ZERO.above(index));
		}
	}

	private static Fixture fixture() {
		ServerLevel level = mock(ServerLevel.class);
		when(level.hasChunkAt(any())).thenReturn(true);
		HashMap<BlockPos, TankBlockEntity> segments = new HashMap<>();
		LevelLightEngine lights = mock(LevelLightEngine.class);
		when(level.getLightEngine()).thenReturn(lights);
		for (int index = 0; index < 3; index++) {
			BlockPos pos = BlockPos.ZERO.above(index);
			TankBlockEntity tank = new TankBlockEntity(pos, ModBlocks.IRON_TANK.get().defaultBlockState());
			tank.setLevel(level);
			segments.put(pos, tank);
		}
		when(level.getBlockEntity(any())).thenAnswer(call -> segments.get(call.getArgument(0)));
		when(level.getBlockState(any())).thenAnswer(call -> segments.get(call.getArgument(0)).getBlockState());
		when(level.setBlock(any(), any(), anyInt())).thenAnswer(call -> {
			segments.get(call.getArgument(0)).setBlockState(call.getArgument(1, BlockState.class));
			return true;
		});
		TankBlockEntity primary = segments.get(BlockPos.ZERO);
		primary.configureGroup(3);
		clearInvocations(lights);
		return new Fixture(level, primary, segments, lights);
	}

	private record Fixture(ServerLevel level, TankBlockEntity primary, HashMap<BlockPos, TankBlockEntity> segments, LevelLightEngine lights) {
		TankFluidHandler fluid() {
			return primary.getStorageWrapper().getTankFluidHandler();
		}
	}
}
