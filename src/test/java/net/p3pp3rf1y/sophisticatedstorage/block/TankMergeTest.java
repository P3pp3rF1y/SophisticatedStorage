package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankMergeTest {
	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.GOLD_TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.STACK_UPGRADE_TIER_1.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.PUMP_UPGRADE.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void extendWithCombinesExactFluidAndKeepsTheOnlyUpgradeConfiguration(boolean lowerHasUpgrades) {
		try (MockedStatic<RegistryHelper> registry = registry()) {
			TankBlockEntity primary = group(2);
			FluidStack water = new FluidStack(Fluids.WATER, 10000);
			water.set(DataComponents.CUSTOM_NAME, Component.literal("Matching fluid"));
			fill(primary.getStorageWrapper().getTankFluidHandler(), water);
			Optional<UUID> id = primary.getStorageWrapper().getContentsUuid();
			ItemStack placed = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
			TankItemStorageWrapper incoming = new TankItemStorageWrapper(placed);
			TankStorageWrapper upgradeOwner = lowerHasUpgrades ? primary.getStorageWrapper() : incoming;
			upgradeOwner.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			upgradeOwner.getUpgradeHandler().setStackInSlot(2, new ItemStack(ModItems.PUMP_UPGRADE.get()));
			int incomingAmount = lowerHasUpgrades ? 20000 : 40000;
			fill(incoming.getTankFluidHandler(), water.copyWithAmount(incomingAmount));
			primary.toggleLock();
			ItemStack originalPayload = placed.copy();
			primary.extendWith(placed);
			assertEquals(3, primary.getHeight());
			assertEquals(192000, primary.getStorageWrapper().getCapacity());
			FluidStack merged = primary.getStorageWrapper().getTankFluidHandler().getFluidInTank(0);
			assertEquals(10000 + incomingAmount, merged.getAmount());
			assertTrue(FluidStack.isSameFluidSameComponents(water, merged));
			assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).isEmpty());
			assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(1).is(ModItems.STACK_UPGRADE_TIER_1));
			assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(2).is(ModItems.PUMP_UPGRADE));
			assertEquals(id, primary.getStorageWrapper().getContentsUuid());
			assertTrue(primary.isLocked());
			assertTrue(ItemStack.isSameItemSameComponents(originalPayload, placed));
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 3, 4, 5})
	void tryUpgradeStorageReportsTheWholeColumnsRequiredCountWhenUsingASecondary(int height) {
		try (MockedStatic<RegistryHelper> registry = registry()) {
			TankBlockEntity primary = group(height);
			fill(primary.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 1000));
			ServerLevel level = (ServerLevel) primary.getLevel();
			BlockPos clickedPos = BlockPos.ZERO.above(height - 1);
			ItemStack upgrades = new ItemStack(ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get(), height - 1);
			ServerPlayer player = mock(ServerPlayer.class);
			InteractionResult result = ((StorageTierUpgradeItem) upgrades.getItem()).tryUpgradeStorage(upgrades, level, clickedPos, primary.getBlockState(),
					player);
			assertEquals(InteractionResult.FAIL, result);
			verify(player).sendOverlayMessage(
					Component.translatable(StorageTranslationHelper.INSTANCE.translGui("status.too_low_tier_upgrade_count"), height, upgrades.getHoverName()));
			assertEquals(height - 1, upgrades.getCount());
			assertEquals(TankTier.GOLD, primary.getTier());
			assertEquals(height, primary.getHeight());
			assertEquals(1000, primary.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
		}
	}

	@ParameterizedTest
	@EnumSource(MergeCase.class)
	void canExtendWithChecksCompatibilityWithoutMutatingEitherOwner(MergeCase mergeCase) {
		try (MockedStatic<RegistryHelper> registry = registry()) {
			TankBlockEntity primary = group(1);
			fill(primary.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 1000));
			primary.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.PUMP_UPGRADE.get()));
			ItemStack placed = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
			TankItemStorageWrapper incoming = new TankItemStorageWrapper(placed);
			switch (mergeCase) {
				case UPGRADE_CONFLICT -> {
					incoming.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
					fill(incoming.getTankFluidHandler(), new FluidStack(Fluids.WATER, 30000));
				}
				case DIFFERENT_COMPONENTS -> {
					FluidStack water = new FluidStack(Fluids.WATER, 1000);
					water.set(DataComponents.CUSTOM_NAME, Component.literal("Different components"));
					fill(incoming.getTankFluidHandler(), water);
				}
				case DIFFERENT_FLUID -> fill(incoming.getTankFluidHandler(), new FluidStack(Fluids.LAVA, 1000));
				case EMPTY -> placed.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY);
			}
			ItemStack originalPayload = placed.copy();
			boolean canExtend = primary.canExtendWith(placed);
			assertEquals(mergeCase == MergeCase.EMPTY, canExtend);
			assertEquals(1, primary.getHeight());
			assertEquals(1000, primary.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
			assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.PUMP_UPGRADE));
			assertTrue(ItemStack.isSameItemSameComponents(originalPayload, placed));
		}
	}

	@Test
	void joinAboveTransfersTheUpperColumnsFluidAndUpgradesToTheNewLowerOwner() {
		try (MockedStatic<RegistryHelper> registry = registry()) {
			TankBlockEntity lower = group(1);
			TankBlockEntity upper = (TankBlockEntity) lower.getLevel().getBlockEntity(BlockPos.ZERO.above());
			upper.configureGroup(2);
			upper.getStorageWrapper().getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			fill(upper.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 50000));
			fill(lower.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 1000));
			Optional<UUID> lowerId = lower.getStorageWrapper().getContentsUuid();
			lower.joinAbove();
			CompoundTag savedUpper = ValueIOHelper.collectOutputToTag(lower.getLevel().registryAccess(), upper::saveAdditional);
			assertEquals(3, lower.getHeight());
			assertSame(lower, upper.getPrimary().orElseThrow());
			assertEquals(51000, lower.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
			assertEquals(192000, lower.getStorageWrapper().getCapacity());
			assertEquals(lowerId, lower.getStorageWrapper().getContentsUuid());
			assertTrue(lower.getStorageWrapper().getUpgradeHandler().getStackInSlot(1).is(ModItems.STACK_UPGRADE_TIER_1));
			assertFalse(savedUpper.contains(StorageBlockEntity.STORAGE_WRAPPER));
		}
	}

	@Test
	void canExtendWithLeavesMaximumHeightColumnsIndependent() {
		try (MockedStatic<RegistryHelper> registry = registry()) {
			TankBlockEntity fullHeight = group(TankTier.MAX_HEIGHT);
			ItemStack next = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
			boolean canExtend = fullHeight.canExtendWith(next);
			assertFalse(canExtend);
			assertEquals(TankTier.MAX_HEIGHT, fullHeight.getHeight());
			assertEquals(160000, fullHeight.getStorageWrapper().getCapacity());
		}
	}

	@Test
	void onItemUseFirstRestoresTheUpperOwnerAfterCancelledPlacementBeneathIt() {
		try (MockedStatic<RegistryHelper> registry = registry()) {
			ServerLevel level = mock(ServerLevel.class);
			when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
			when(level.dimension()).thenReturn(Level.OVERWORLD);
			when(level.hasChunkAt(any())).thenReturn(true);
			Map<BlockPos, TankBlockEntity> members = new HashMap<>();
			for (int index = 1; index <= 2; index++) {
				BlockPos pos = BlockPos.ZERO.above(index);
				TankBlockEntity segment = new TankBlockEntity(pos, ModBlocks.GOLD_TANK.get().defaultBlockState());
				segment.setLevel(level);
				members.put(pos, segment);
			}
			when(level.getBlockEntity(any())).thenAnswer(call -> members.get(call.getArgument(0)));
			when(level.getBlockState(any())).thenAnswer(call -> {
				BlockPos pos = call.getArgument(0);
				return pos.equals(BlockPos.ZERO.below())
						? Blocks.STONE.defaultBlockState()
						: members.containsKey(pos) ? ModBlocks.GOLD_TANK.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
			});
			TankBlockEntity upper = members.get(BlockPos.ZERO.above());
			upper.configureGroup(2);
			upper.getStorageWrapper().getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
			fill(upper.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 50000));
			Optional<UUID> upperId = upper.getStorageWrapper().getContentsUuid();
			ItemStack stack = spy(new ItemStack(ModBlocks.GOLD_TANK_ITEM.get()));
			BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, stack,
					new BlockHitResult(Vec3.atCenterOf(BlockPos.ZERO.below()), Direction.UP, BlockPos.ZERO.below(), false));
			doAnswer(call -> {
				TankBlockEntity placed = new TankBlockEntity(BlockPos.ZERO, ModBlocks.GOLD_TANK.get().defaultBlockState());
				placed.setLevel(level);
				members.put(BlockPos.ZERO, placed);
				placed.joinAbove();
				members.remove(BlockPos.ZERO);
				return InteractionResult.FAIL;
			}).when(stack).useOn(context);
			InteractionResult result = ((TankBlockItem) stack.getItem()).onItemUseFirst(stack, context);
			assertEquals(InteractionResult.FAIL, result);
			assertTrue(upper.isPrimary());
			assertEquals(2, upper.getHeight());
			assertEquals(50000, upper.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
			assertTrue(upper.getStorageWrapper().getUpgradeHandler().getStackInSlot(1).is(ModItems.STACK_UPGRADE_TIER_1));
			assertEquals(upperId, upper.getStorageWrapper().getContentsUuid());
			assertSame(upper, members.get(BlockPos.ZERO.above(2)).getPrimary().orElseThrow());
		}
	}

	private enum MergeCase {
		UPGRADE_CONFLICT, DIFFERENT_COMPONENTS, DIFFERENT_FLUID, EMPTY
	}

	private static void fill(TankFluidHandler handler, FluidStack fluid) {
		try (Transaction tx = Transaction.openRoot()) {
			assertEquals(fluid.getAmount(), handler.insert(0, FluidResource.of(fluid), fluid.getAmount(), tx, true));
			tx.commit();
		}
	}

	private static TankBlockEntity group(int height) {
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		when(level.hasChunkAt(any())).thenReturn(true);
		Map<BlockPos, TankBlockEntity> members = new HashMap<>();
		for (int index = 0; index < TankTier.MAX_HEIGHT; index++) {
			BlockPos pos = BlockPos.ZERO.above(index);
			TankBlockEntity tank = new TankBlockEntity(pos, ModBlocks.GOLD_TANK.get().defaultBlockState());
			tank.setLevel(level);
			members.put(pos, tank);
		}
		when(level.getBlockEntity(any())).thenAnswer(invocation -> members.get(invocation.getArgument(0)));
		TankBlockEntity primary = members.get(BlockPos.ZERO);
		primary.configureGroup(height);
		return primary;
	}

	private static MockedStatic<RegistryHelper> registry() {
		MockedStatic<RegistryHelper> registry = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		registry.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
		return registry;
	}
}
