package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankMergeTest {
	@BeforeAll
	static void bootstrap() {
		StorageTankTestBootstrap.bootstrapMinecraft();
		TankLightingTest.initializeVanillaFluidTypes();
	}
	@AfterAll
	static void clearConfig() {
		StorageTankTestBootstrap.clearConfig();
	}
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void extendWithCombinesExactFluidAndKeepsTheOnlyUpgradeConfiguration(boolean lowerHasUpgrades) {
		TankBlockEntity primary = group(2);
		FluidStack water = new FluidStack(Fluids.WATER, 10000);
		CompoundTag identity = new CompoundTag();
		identity.putString("name", "Matching fluid");
		water.setTag(identity);
		primary.getStorageWrapper().getTankFluidHandler().fill(water, IFluidHandler.FluidAction.EXECUTE);
		Optional<UUID> id = primary.getStorageWrapper().getContentsUuid();
		ItemStack placed = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
		TankItemStorageWrapper incoming = new TankItemStorageWrapper(placed);
		TankStorageWrapper upgradeOwner = lowerHasUpgrades ? primary.getStorageWrapper() : incoming;
		upgradeOwner.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		upgradeOwner.getUpgradeHandler().setStackInSlot(2, new ItemStack(ModItems.PUMP_UPGRADE.get()));
		int incomingAmount = lowerHasUpgrades ? 20000 : 40000;
		FluidStack incomingWater = water.copy();
		incomingWater.setAmount(incomingAmount);
		incoming.getTankFluidHandler().fill(incomingWater, IFluidHandler.FluidAction.EXECUTE);
		primary.toggleLock();
		ItemStack originalPayload = placed.copy();

		primary.extendWith(placed);

		assertEquals(3, primary.getHeight());
		assertEquals(192000, primary.getStorageWrapper().getCapacity());
		FluidStack merged = primary.getStorageWrapper().getTankFluidHandler().getFluidInTank(0);
		assertEquals(10000 + incomingAmount, merged.getAmount());
		assertTrue(water.isFluidEqual(merged));
		assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).isEmpty());
		assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(1).is(ModItems.STACK_UPGRADE_TIER_1.get()));
		assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(2).is(ModItems.PUMP_UPGRADE.get()));
		assertEquals(id, primary.getStorageWrapper().getContentsUuid());
		assertTrue(primary.isLocked());
		assertTrue(ItemStack.isSameItemSameTags(originalPayload, placed));
	}
	@ParameterizedTest
	@ValueSource(ints = {2, 3, 4, 5})
	void tryUpgradeStorageReportsTheWholeColumnsRequiredCountWhenUsingASecondary(int height) {
		TankBlockEntity primary = group(height);
		primary.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		ServerLevel level = (ServerLevel) primary.getLevel();
		BlockPos clickedPos = BlockPos.ZERO.above(height - 1);
		ItemStack upgrades = new ItemStack(ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get(), height - 1);
		ServerPlayer player = mock(ServerPlayer.class);
		InteractionResult result = ((StorageTierUpgradeItem) upgrades.getItem()).tryUpgradeStorage(upgrades, level, clickedPos, primary.getBlockState(),
				player);
		assertEquals(InteractionResult.FAIL, result);
		verify(player).displayClientMessage(
				Component.translatable(StorageTranslationHelper.INSTANCE.translGui("status.too_low_tier_upgrade_count"), height, upgrades.getHoverName()),
				true);
		assertEquals(height - 1, upgrades.getCount());
		assertEquals(TankTier.GOLD, primary.getTier());
		assertEquals(height, primary.getHeight());
		assertEquals(1000, primary.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@EnumSource(MergeCase.class)
	void canExtendWithChecksCompatibilityWithoutMutatingEitherOwner(MergeCase mergeCase) {
		TankBlockEntity primary = group(1);
		primary.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		primary.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.PUMP_UPGRADE.get()));
		ItemStack placed = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
		TankItemStorageWrapper incoming = new TankItemStorageWrapper(placed);
		switch (mergeCase) {
			case UPGRADE_CONFLICT -> {
				incoming.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
				incoming.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 30000), IFluidHandler.FluidAction.EXECUTE);
			}
			case DIFFERENT_COMPONENTS -> {
				FluidStack water = new FluidStack(Fluids.WATER, 1000);
				CompoundTag identity = new CompoundTag();
				identity.putString("name", "Different components");
				water.setTag(identity);
				incoming.getTankFluidHandler().fill(water, IFluidHandler.FluidAction.EXECUTE);
			}
			case DIFFERENT_FLUID -> incoming.getTankFluidHandler().fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
			case EMPTY -> placed.getOrCreateTag().put("fluidContents", new CompoundTag());
		}
		ItemStack originalPayload = placed.copy();
		boolean canExtend = primary.canExtendWith(placed);
		assertEquals(mergeCase == MergeCase.EMPTY, canExtend);
		assertEquals(1, primary.getHeight());
		assertEquals(1000, primary.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
		assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(ModItems.PUMP_UPGRADE.get()));
		assertTrue(ItemStack.isSameItemSameTags(originalPayload, placed));
	}
	@Test
	void joinAboveTransfersTheUpperColumnsFluidAndUpgradesToTheNewLowerOwner() {
		TankBlockEntity lower = group(1);
		TankBlockEntity upper = (TankBlockEntity) lower.getLevel().getBlockEntity(BlockPos.ZERO.above());
		upper.configureGroup(2);
		upper.getStorageWrapper().getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		upper.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 50000), IFluidHandler.FluidAction.EXECUTE);
		lower.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		Optional<UUID> lowerId = lower.getStorageWrapper().getContentsUuid();

		lower.joinAbove();
		CompoundTag savedUpper = new CompoundTag();
		upper.saveAdditional(savedUpper);

		assertEquals(3, lower.getHeight());
		assertSame(lower, upper.getPrimary().orElseThrow());
		assertEquals(51000, lower.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
		assertEquals(192000, lower.getStorageWrapper().getCapacity());
		assertEquals(lowerId, lower.getStorageWrapper().getContentsUuid());
		assertTrue(lower.getStorageWrapper().getUpgradeHandler().getStackInSlot(1).is(ModItems.STACK_UPGRADE_TIER_1.get()));
		assertFalse(savedUpper.contains(StorageBlockEntity.STORAGE_WRAPPER_TAG));
	}
	@Test
	void canExtendWithLeavesMaximumHeightColumnsIndependent() {
		TankBlockEntity fullHeight = group(TankTier.MAX_HEIGHT);
		ItemStack next = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
		assertFalse(fullHeight.canExtendWith(next));
		assertEquals(TankTier.MAX_HEIGHT, fullHeight.getHeight());
		assertEquals(160000, fullHeight.getStorageWrapper().getCapacity());
	}
	@Test
	void onItemUseFirstRestoresTheUpperOwnerAfterCancelledPlacementBeneathIt() {
		ServerLevel level = mock(ServerLevel.class);
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
		upper.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 50000), IFluidHandler.FluidAction.EXECUTE);
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
		assertTrue(upper.getStorageWrapper().getUpgradeHandler().getStackInSlot(1).is(ModItems.STACK_UPGRADE_TIER_1.get()));
		assertEquals(upperId, upper.getStorageWrapper().getContentsUuid());
		assertSame(upper, members.get(BlockPos.ZERO.above(2)).getPrimary().orElseThrow());
	}
	private enum MergeCase {
		UPGRADE_CONFLICT, DIFFERENT_COMPONENTS, DIFFERENT_FLUID, EMPTY
	}
	private static TankBlockEntity group(int height) {
		ServerLevel level = mock(ServerLevel.class);
		when(level.hasChunkAt(any())).thenReturn(true);
		Map<BlockPos, TankBlockEntity> members = new HashMap<>();
		for (int index = 0; index < TankTier.MAX_HEIGHT; index++) {
			BlockPos pos = BlockPos.ZERO.above(index);
			TankBlockEntity tank = new TankBlockEntity(pos, ModBlocks.GOLD_TANK.get().defaultBlockState());
			tank.setLevel(level);
			members.put(pos, tank);
		}
		when(level.getBlockEntity(any())).thenAnswer(invocation -> members.get(invocation.getArgument(0)));
		when(level.getBlockState(any())).thenAnswer(invocation -> {
			TankBlockEntity tank = members.get(invocation.getArgument(0));
			return tank == null ? Blocks.AIR.defaultBlockState() : tank.getBlockState();
		});
		TankBlockEntity primary = members.get(BlockPos.ZERO);
		primary.configureGroup(height);
		return primary;
	}
}
