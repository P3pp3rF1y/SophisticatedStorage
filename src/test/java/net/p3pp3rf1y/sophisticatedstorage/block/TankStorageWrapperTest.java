package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.StorageContentsType;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankStorageWrapperTest {
	private Map<TankTier, Integer> previousCapacities;
	private Map<TankTier, Integer> previousUpgradeSlots;

	@BeforeEach
	void initializeRegistryAndRememberCapacityConfiguration() {
		previousCapacities = new HashMap<>();
		previousUpgradeSlots = new HashMap<>();
		for (TankTier tier : TankTier.values()) {
			previousCapacities.put(tier, tankConfig(tier).capacity.get());
			previousUpgradeSlots.put(tier, tankConfig(tier).upgradeSlotCount.get());
		}
	}

	@AfterEach
	void restoreCapacityConfigurationAndCloseRegistry() {
		previousCapacities.forEach((tier, value) -> tankConfig(tier).capacity.set(value));
		previousUpgradeSlots.forEach((tier, value) -> tankConfig(tier).upgradeSlotCount.set(value));
	}
	@BeforeAll
	static void bootstrap() {
		StorageTankTestBootstrap.bootstrapMinecraft();
		for (String name : List.of("tank_upgrade", "battery_upgrade")) {
			ResourceLocation id = new ResourceLocation("sophisticatedbackpacks", name);
			if (!BuiltInRegistries.ITEM.containsKey(id)) {
				Item item = new Item(new Item.Properties());
				ForgeRegistries.ITEMS.register(id, item);
			}
		}
	}

	@AfterAll
	static void clearConfig() {
		StorageTankTestBootstrap.clearConfig();
	}

	@Test
	void insertItemRejectsForbiddenUpgrades() {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 2, new AtomicBoolean());
		ItemStack forbidden = new ItemStack(ModItems.HOPPER_UPGRADE.get());

		ItemStack rejected = wrapper.getUpgradeHandler().insertItem(1, forbidden, false);

		assertTrue(ItemStack.isSameItemSameTags(forbidden, rejected));
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(1).isEmpty());
	}
	@Test
	void setStackInSlotRejectsForbiddenUpgrades() {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 2, new AtomicBoolean());
		ItemStack forbidden = new ItemStack(ModItems.HOPPER_UPGRADE.get());

		assertThrows(IllegalArgumentException.class, () -> wrapper.getUpgradeHandler().setStackInSlot(1, forbidden));
	}
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void extractItemRejectsRemovalWhenFluidWouldExceedRemainingCapacity(boolean simulate) {
		TankStorageWrapper wrapper = upgradedWrapper(TankTier.IRON, 2);
		wrapper.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 48000), IFluidHandler.FluidAction.EXECUTE);

		ItemStack removed = wrapper.getUpgradeHandler().extractItem(0, 1, simulate);

		assertTrue(removed.isEmpty());
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_TIER_1.get()));
		assertEquals(64000, wrapper.getCapacity());
		assertEquals(48000, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void setStackInSlotRejectsCapacityReducingReplacements(boolean downgrade) {
		TankStorageWrapper wrapper = upgradedWrapper(TankTier.IRON, 2);
		wrapper.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 48000), IFluidHandler.FluidAction.EXECUTE);
		ItemStack replacement = downgrade ? new ItemStack(ModItems.STACK_DOWNGRADE_TIER_1.get()) : ItemStack.EMPTY;

		assertThrows(IllegalArgumentException.class, () -> wrapper.getUpgradeHandler().setStackInSlot(0, replacement));
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_TIER_1.get()));
	}
	@Test
	void extractItemAllowsCapacityUpgradeRemovalAfterEnoughFluidWasDrained() {
		TankStorageWrapper wrapper = upgradedWrapper(TankTier.IRON, 2);
		wrapper.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 48000), IFluidHandler.FluidAction.EXECUTE);
		wrapper.getTankFluidHandler().drain(16000, IFluidHandler.FluidAction.EXECUTE);

		ItemStack removed = wrapper.getUpgradeHandler().extractItem(0, 1, false);

		assertTrue(removed.is(ModItems.STACK_UPGRADE_TIER_1.get()));
		assertEquals(32000, wrapper.getCapacity());
		assertEquals(32000, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@EnumSource(TankTier.class)
	void getCapacityScalesEachTierConfiguredCapacityByHeightAndUpgrade(TankTier tier) {
		tankConfig(tier).capacity.set(12_345);
		TankStorageWrapper wrapper = upgradedWrapper(tier, 3);

		int capacity = wrapper.getCapacity();

		assertEquals(74_070, capacity);
		assertEquals(capacity, wrapper.getTankFluidHandler().getTankCapacity(0));
	}
	@ParameterizedTest
	@EnumSource(TankTier.class)
	void loadCanonicalDataRetainsOverCapacityFluidAfterConfiguredCapacityReduction(TankTier tier) {
		tankConfig(tier).capacity.set(16_000);
		TankStorageWrapper original = upgradedWrapper(tier, 3);
		int amount = original.getCapacity() / 2;
		original.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, amount), IFluidHandler.FluidAction.EXECUTE);
		CompoundTag data = original.saveCanonicalData(new CompoundTag());
		tankConfig(tier).capacity.set(1000);
		TankStorageWrapper restored = wrapper(tier, 3, new AtomicBoolean());

		restored.loadCanonicalData(data);

		assertEquals(original.getCapacity(), restored.getCapacity());
		assertTrue(restored.getCapacity() < amount);
		assertEquals(amount, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@EnumSource(TankTier.class)
	void fillRejectsAdditionalFluidAfterConfiguredCapacityReduction(TankTier tier) {
		tankConfig(tier).capacity.set(16_000);
		TankStorageWrapper wrapper = upgradedWrapper(tier, 3);
		int amount = wrapper.getCapacity() / 2;
		wrapper.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, amount), IFluidHandler.FluidAction.EXECUTE);
		tankConfig(tier).capacity.set(1000);

		int filled = wrapper.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

		assertEquals(0, filled);
		assertEquals(amount, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@EnumSource(TankTier.class)
	void getCapacityClampsConfiguredCapacityOverflowToIntegerMaximum(TankTier tier) {
		tankConfig(tier).capacity.set(Integer.MAX_VALUE);
		TankStorageWrapper wrapper = upgradedWrapper(tier, 3);

		int capacity = wrapper.getCapacity();

		assertEquals(Integer.MAX_VALUE, capacity);
	}
	@Test
	void loadCanonicalDataRestoresFluidAndCapacityUpgradesWithoutItemSlots() {
		TankStorageWrapper original = wrapper(TankTier.DIAMOND, 5, new AtomicBoolean());
		original.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
		original.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 200000), IFluidHandler.FluidAction.EXECUTE);
		CompoundTag saved = original.saveCanonicalData(new CompoundTag());
		TankStorageWrapper restored = wrapper(TankTier.DIAMOND, 5, new AtomicBoolean());

		restored.loadCanonicalData(saved.copy());

		assertEquals(1280000, restored.getCapacity());
		assertEquals(200000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
		assertTrue(restored.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_TIER_2.get()));
		assertEquals(0, restored.getInventoryHandler().getSlots());
	}
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void fillUsesTheLiveLockAfterCanonicalContentsAreRestored(boolean matchingFluid) {
		AtomicBoolean locked = new AtomicBoolean();
		TankStorageWrapper original = wrapper(TankTier.DIAMOND, 5, locked);
		original.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 200000), IFluidHandler.FluidAction.EXECUTE);
		TankStorageWrapper restored = wrapper(TankTier.DIAMOND, 5, locked);
		restored.loadCanonicalData(original.saveCanonicalData(new CompoundTag()));
		locked.set(true);

		int filled = restored.getTankFluidHandler().fill(new FluidStack(matchingFluid ? Fluids.WATER : Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);

		assertEquals(matchingFluid ? 1000 : 0, filled);
		assertEquals(matchingFluid ? 201000 : 200000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@EnumSource(TankTier.class)
	void loadCanonicalDataPreservesUpgradesInAdditionalConfiguredSlots(TankTier tier) {
		tankConfig(tier).upgradeSlotCount.set(7);
		TankStorageWrapper original = wrapper(tier, 1, new AtomicBoolean());
		original.getUpgradeHandler().setStackInSlot(6, new ItemStack(ModItems.PUMP_UPGRADE.get()));
		original.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		CompoundTag saved = original.saveCanonicalData(new CompoundTag());
		TankStorageWrapper restored = wrapper(tier, 1, new AtomicBoolean());

		restored.loadCanonicalData(saved);

		assertEquals(7, restored.getUpgradeHandler().getSlots());
		assertTrue(restored.getUpgradeHandler().getStackInSlot(6).is(ModItems.PUMP_UPGRADE.get()));
		assertEquals(1000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@EnumSource(TankTier.class)
	void partitionContentsKeepsUpgradesBeyondTheDefaultTierSlots(TankTier tier) {
		tankConfig(tier).upgradeSlotCount.set(7);
		TankStorageWrapper original = wrapper(tier, 1, new AtomicBoolean());
		List<ItemStack> upgrades = new ArrayList<>();
		for (int slot = 0; slot < 6; slot++) {
			upgrades.add(ItemStack.EMPTY);
		}
		upgrades.add(new ItemStack(ModItems.PUMP_UPGRADE.get()));

		CompoundTag partition = original.partitionContents(new FluidStack(Fluids.WATER, 1000), upgrades, false);
		TankStorageWrapper restored = wrapper(tier, 1, new AtomicBoolean());
		restored.loadCanonicalData(partition);

		assertEquals(7, restored.getUpgradeHandler().getSlots());
		assertTrue(restored.getUpgradeHandler().getStackInSlot(6).is(ModItems.PUMP_UPGRADE.get()));
		assertEquals(1000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@Test
	void getCapacityDoesNotUseAnotherTierConfiguredCapacity() {
		TankStorageWrapper iron = wrapper(TankTier.IRON, 2, new AtomicBoolean());
		int originalCapacity = iron.getCapacity();
		Config.SERVER.woodTank.capacity.set(123);
		Config.SERVER.goldTank.capacity.set(456);

		int capacity = iron.getCapacity();

		assertEquals(originalCapacity, capacity);
	}
	@ParameterizedTest
	@EnumSource(IFluidHandler.FluidAction.class)
	void fillItemCapabilityMutatesOnlyWhenExecuted(IFluidHandler.FluidAction action) {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get());
		ItemStack original = stack.copy();
		IFluidHandlerItem capability = TankItemStorageWrapper.getCapability(stack);

		int filled = capability.fill(new FluidStack(Fluids.WATER, 1000), action);

		assertEquals(1000, filled);
		assertSame(stack, capability.getContainer());
		assertEquals(action.execute() ? 1000 : 0, capability.getFluidInTank(0).getAmount());
		if (action.simulate()) {
			assertTrue(ItemStack.isSameItemSameTags(original, stack));
		}
	}
	@Test
	void fillItemCapabilityReadsExternallyReplacedComponents() {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get());
		IFluidHandlerItem capability = TankItemStorageWrapper.getCapability(stack);
		capability.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		stack.getTag().remove("fluidContents");

		int filled = capability.fill(new FluidStack(Fluids.LAVA, 500), IFluidHandler.FluidAction.EXECUTE);

		assertEquals(500, filled);
		assertTrue(capability.getFluidInTank(0).getFluid() == Fluids.LAVA);
		assertEquals(500, capability.getFluidInTank(0).getAmount());
	}
	@Test
	void drainItemCapabilitySimulationLeavesComponentsUnchanged() {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get());
		IFluidHandlerItem capability = TankItemStorageWrapper.getCapability(stack);
		capability.fill(new FluidStack(Fluids.LAVA, 500), IFluidHandler.FluidAction.EXECUTE);
		ItemStack original = stack.copy();

		FluidStack drained = capability.drain(1000, IFluidHandler.FluidAction.SIMULATE);

		assertEquals(500, drained.getAmount());
		assertTrue(ItemStack.isSameItemSameTags(original, stack));
	}
	@Test
	void onItemUseFirstConsumesOnlyOneMatchingPortableTankInCreative() {
		ServerLevel level = mock(ServerLevel.class);
		when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
		ServerPlayer player = mock(ServerPlayer.class);
		when(player.isCreative()).thenReturn(true);
		ItemStack stack = spy(new ItemStack(ModBlocks.TANK_ITEM.get(), 64));
		TankItemContents.height(stack, 1);
		BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(BlockPos.ZERO), Direction.UP, BlockPos.ZERO, false));
		doReturn(InteractionResult.SUCCESS).when(stack).useOn(context);

		InteractionResult result = stack.getItem().onItemUseFirst(stack, context);

		assertEquals(InteractionResult.SUCCESS, result);
		assertEquals(63, stack.getCount());
		assertEquals(1, TankItemContents.height(stack));
		verify(player, never()).setItemInHand(any(), any());
	}
	@Test
	void tryFillContainerAndStowSplitsOneTankWithoutMultiplyingFluidAcrossTheStack() {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get(), 64);
		TankStorageWrapper source = wrapper(TankTier.IRON, 1, new AtomicBoolean());
		source.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
		ItemStackHandler inventory = new ItemStackHandler(1);

		FluidActionResult result = FluidUtil.tryFillContainerAndStow(stack, source.getTankFluidHandler(), inventory, 1000, null, true);

		assertTrue(result.isSuccess());
		assertEquals(63, result.getResult().getCount());
		assertFalse(TankItemContents.hasContents(result.getResult()));
		assertEquals(64, stack.getCount());
		assertFalse(TankItemContents.hasContents(stack));
		assertEquals(1, inventory.getStackInSlot(0).getCount());
		assertEquals(1000, TankItemStorageWrapper.getCapability(inventory.getStackInSlot(0)).getFluidInTank(0).getAmount());
		assertEquals(1000, source.getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@Test
	void fillItemCapabilityRejectsStackedPhysicalContainers() {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get(), 64);
		IFluidHandlerItem capability = TankItemStorageWrapper.getCapability(stack);

		int filled = capability.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

		assertEquals(0, filled);
		assertFalse(TankItemContents.hasContents(stack));
	}
	@Test
	void getExternalFluidHandlerExposesSynchronizedClientContentsForStandardFluidTooltips() {
		Level level = mock(Level.class);
		when(level.isClientSide()).thenReturn(true);
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		tank.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 6000), IFluidHandler.FluidAction.EXECUTE);

		IFluidHandler capability = tank.getExternalFluidHandler(null);

		assertNotNull(capability);
		assertEquals(6000, capability.getFluidInTank(0).getAmount());
		assertEquals(16000, capability.getTankCapacity(0));
	}
	@Test
	void clearContentRemovesFluidBeforeCapacityProvidingUpgrades() {
		ServerLevel level = mock(ServerLevel.class);
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		tank.getStorageWrapper().getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		tank.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 30000), IFluidHandler.FluidAction.EXECUTE);

		tank.clearContent();

		assertTrue(tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).isEmpty());
		assertTrue(tank.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).isEmpty());
	}
	@ParameterizedTest
	@CsvSource({"false, 100", "false, 105", "true, 105"})
	void tickExperiencePumpHonorsCooldownAndScaledTransferLimit(boolean stacked, long nextTick) {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 5, new AtomicBoolean());
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.XP_PUMP_UPGRADE.get()));
		XpPumpUpgradeWrapper pump = wrapper.getUpgradeHandler().getTypeWrappers(XpPumpUpgradeItem.TYPE).get(0);
		pump.setMendItems(false);
		ServerPlayer player = mock(ServerPlayer.class);
		player.experienceLevel = 114;
		Level level = mock(Level.class);
		when(level.getGameTime()).thenReturn(100L);
		pump.tick(player, level, BlockPos.ZERO);
		if (stacked) {
			wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_5.get()));
		}
		clearInvocations(player);
		when(level.getGameTime()).thenReturn(nextTick);
		int transferred = nextTick == 100 ? 0 : (int) Math.max(1000, 20 * 5 * wrapper.getStackMultiplier());

		pump.tick(player, level, BlockPos.ZERO);

		assertEquals(1000 + transferred, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
		if (transferred == 0) {
			verify(player, never()).giveExperiencePoints(anyInt());
		} else {
			verify(player).giveExperiencePoints(-transferred / 20);
		}
	}
	@Test
	void tickExperiencePumpTakesOneRateLimitedBatchFromLevel114() {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 5, new AtomicBoolean());
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.XP_PUMP_UPGRADE.get()));
		XpPumpUpgradeWrapper pump = wrapper.getUpgradeHandler().getTypeWrappers(XpPumpUpgradeItem.TYPE).get(0);
		pump.setMendItems(false);
		ServerPlayer player = mock(ServerPlayer.class);
		player.experienceLevel = 114;
		Level level = mock(Level.class);
		when(level.getGameTime()).thenReturn(100L);

		pump.tick(player, level, BlockPos.ZERO);

		assertEquals(1000, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
		verify(player).giveExperiencePoints(-50);
	}
	@ParameterizedTest
	@MethodSource("transferLimits")
	void fillAndDrainApplyTheConfiguredAutomaticTransferLimit(TransferLimit params) {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 5, new AtomicBoolean());
		if (params.stacked()) {
			wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_5.get()));
		}
		TankFluidHandler handler = wrapper.getTankFluidHandler();
		if (params.drain()) {
			handler.fill(new FluidStack(Fluids.WATER, 100000), IFluidHandler.FluidAction.EXECUTE, true);
		}
		int available = Math.min(100000, wrapper.getCapacity());
		int limit = params.ignoreLimit() ? available : Math.min(available, (int) Math.max(1000, 20 * 5 * wrapper.getStackMultiplier()));

		int transferred = params.drain()
				? handler.drain(100000, params.action(), params.ignoreLimit()).getAmount()
				: handler.fill(new FluidStack(Fluids.WATER, 100000), params.action(), params.ignoreLimit());

		assertEquals(limit, transferred);
		int initial = params.drain() ? available : 0;
		assertEquals(initial + (params.action().execute() ? (params.drain() ? -limit : limit) : 0), handler.getFluidInTank(0).getAmount());
	}
	@Test
	void getExternalFluidHandlerStopsServingRemovedPhysicalOwners() {
		ServerLevel level = mock(ServerLevel.class);
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		IFluidHandler retained = tank.getExternalFluidHandler(null);
		retained.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		tank.setRemoved();

		int filled = retained.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		FluidStack drained = retained.drain(1000, IFluidHandler.FluidAction.EXECUTE);

		assertEquals(0, retained.getTanks());
		assertEquals(0, filled);
		assertTrue(drained.isEmpty());
		assertEquals(1000, tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void onItemUseFirstRestoresExistingGroupMetadataAfterCancelledExtension(int height) {
		ServerLevel level = mock(ServerLevel.class);
		when(level.dimension()).thenReturn(Level.OVERWORLD);
		when(level.hasChunkAt(any())).thenReturn(true);
		Map<BlockPos, TankBlockEntity> members = new HashMap<>();
		for (int index = 0; index <= height; index++) {
			BlockPos pos = BlockPos.ZERO.above(index);
			TankBlockEntity segment = new TankBlockEntity(pos, ModBlocks.IRON_TANK.get().defaultBlockState());
			segment.setLevel(level);
			members.put(pos, segment);
		}
		when(level.getBlockEntity(any())).thenAnswer(invocation -> members.get(invocation.getArgument(0)));
		when(level.getBlockState(any())).thenReturn(ModBlocks.IRON_TANK.get().defaultBlockState());
		TankBlockEntity primary = members.get(BlockPos.ZERO);
		primary.configureGroup(height);
		primary.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		ItemStack stack = spy(new ItemStack(ModBlocks.IRON_TANK_ITEM.get()));
		TankItemStorageWrapper incoming = new TankItemStorageWrapper(stack);
		incoming.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		incoming.getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 20000), IFluidHandler.FluidAction.EXECUTE);
		BlockPos top = BlockPos.ZERO.above(height - 1);
		BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false));
		doAnswer(invocation -> {
			primary.extendWith(stack);
			// Unchanged BOTTOM/MIDDLE states retain their updated NBT after block-state rollback.
			members.remove(BlockPos.ZERO.above(height));
			return InteractionResult.FAIL;
		}).when(stack).useOn(context);

		InteractionResult result = stack.getItem().onItemUseFirst(stack, context);

		assertEquals(InteractionResult.FAIL, result);
		assertEquals(height, primary.getHeight());
		assertEquals(16000 * height, primary.getStorageWrapper().getCapacity());
		assertEquals(1000, primary.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
		assertTrue(primary.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).isEmpty());
		for (TankBlockEntity segment : members.values()) {
			assertSame(primary, segment.getPrimary().orElseThrow());
			assertEquals(height, segment.getHeight());
		}
	}
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void getControllerFluidHandlerResolvesCurrentNativeMembership(boolean removed) {
		ServerLevel level = mock(ServerLevel.class);
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		StorageBlockEntity ordinary = mock(StorageBlockEntity.class);
		List<IControllableStorage> members = new ArrayList<>(List.of(ordinary, tank));
		ControllerBlockEntity controller = new ControllerBlockEntity(BlockPos.ZERO.west(), ModBlocks.CONTROLLER.get().defaultBlockState()) {
			@Override
			protected List<IControllableStorage> getLoadedStorageRepresentatives() {
				return members;
			}
		};
		controller.setLevel(level);
		IFluidHandler retained = controller.getExternalFluidHandler(null);
		if (removed) {
			members.remove(tank);
		}

		int filled = retained.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

		assertEquals(removed ? 0 : 1, retained.getTanks());
		assertEquals(removed ? 0 : 1000, filled);
		verifyNoInteractions(ordinary);
	}
	@Test
	void drainUsesAReaddedMemberThroughTheRetainedControllerHandler() {
		ServerLevel level = mock(ServerLevel.class);
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		tank.getStorageWrapper().getTankFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		List<IControllableStorage> members = new ArrayList<>(List.of(tank));
		ControllerBlockEntity controller = new ControllerBlockEntity(BlockPos.ZERO.west(), ModBlocks.CONTROLLER.get().defaultBlockState()) {
			@Override
			protected List<IControllableStorage> getLoadedStorageRepresentatives() {
				return members;
			}
		};
		controller.setLevel(level);
		IFluidHandler retained = controller.getExternalFluidHandler(null);
		members.clear();
		retained.getTanks();
		members.add(tank);

		FluidStack drained = retained.drain(1000, IFluidHandler.FluidAction.EXECUTE);

		assertEquals(1000, drained.getAmount());
		assertTrue(drained.getFluid() == Fluids.WATER);
		assertTrue(tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).isEmpty());
	}
	@ParameterizedTest
	@MethodSource("voidFilters")
	void fillAppliesInstalledVoidFluidFilters(VoidFilter params) {
		AtomicBoolean locked = new AtomicBoolean();
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 1, locked);
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.ADVANCED_VOID_UPGRADE.get()));
		VoidUpgradeWrapper voidUpgrade = wrapper.getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).get(0);
		voidUpgrade.getFilterLogic().setAllowList(true);
		voidUpgrade.getFluidFilterLogic().setFluid(0, new FluidStack(Fluids.WATER, 1));
		voidUpgrade.setVoidType(params.type());
		IFluidHandler fluid = wrapper.getTankFluidHandler();
		if (params.full()) {
			fluid.fill(new FluidStack(Fluids.WATER, 16000), IFluidHandler.FluidAction.EXECUTE);
		}
		locked.set(params.locked());

		int accepted = fluid.fill(new FluidStack(params.water() ? Fluids.WATER : Fluids.LAVA, 20000), params.action());

		assertEquals(params.accepted(), accepted);
		assertEquals(params.stored(), fluid.getFluidInTank(0).getAmount());
	}
	@ParameterizedTest
	@MethodSource("magnetItems")
	void pickupRejectsItemsWhileTheMagnetRetainsXpSupport(Item item) {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 1, new AtomicBoolean());
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(item));
		MagnetUpgradeWrapper magnet = wrapper.getUpgradeHandler().getTypeWrappers(MagnetUpgradeItem.TYPE).get(0);
		magnet.setPickupItems(true);
		ItemStack input = new ItemStack(Items.DIAMOND, 3);

		ItemStack remaining = magnet.pickup(null, input, false);

		assertFalse(magnet.supportsItemPickup());
		assertFalse(magnet.shouldPickupItems());
		assertTrue(magnet.shouldPickupXp());
		assertSame(input, remaining);
		assertEquals(0, wrapper.getInventoryHandler().getSlots());
	}
	@ParameterizedTest
	@MethodSource("pumpTargets")
	void canAddUpgradeToMatchesTheDeclaredStorageContents(PumpTarget params) {
		TankStorageWrapper tank = wrapper(TankTier.IRON, 1, new AtomicBoolean());
		StorageWrapper barrel = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState()).getStorageWrapper();
		IStorageWrapper otherStorage = mock(IStorageWrapper.class);
		when(otherStorage.getDisplayName()).thenReturn(Component.literal("Other storage"));
		when(otherStorage.getStorageType()).thenReturn("tank");
		when(otherStorage.getUpgradeHandler()).thenReturn(mock(UpgradeHandler.class));
		when(otherStorage.getSupportedContents())
				.thenReturn(Set.of(params.target() == PumpTargetKind.OTHER_FLUID ? StorageContentsType.FLUID : StorageContentsType.ITEM));
		ItemStack pump = new ItemStack(params.item());
		IUpgradeItem<?> upgrade = (IUpgradeItem<?>) params.item();
		IStorageWrapper target = switch (params.target()) {
			case TANK -> tank;
			case BARREL -> barrel;
			case OTHER_ITEM, OTHER_FLUID -> otherStorage;
		};

		boolean allowed = upgrade.canAddUpgradeTo(target, pump, true, true).isSuccessful();

		assertEquals(params.target() == PumpTargetKind.TANK || params.target() == PumpTargetKind.OTHER_FLUID, allowed);
	}
	@ParameterizedTest
	@MethodSource("pumpItems")
	void insertItemRejectsStoragePumpsInOrdinaryStorage(Item item) {
		StorageWrapper barrel = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState()).getStorageWrapper();
		ItemStack pump = new ItemStack(item);

		ItemStack rejected = barrel.getUpgradeHandler().insertItem(0, pump, false);

		assertTrue(ItemStack.isSameItemSameTags(pump, rejected));
		assertFalse(barrel.getUpgradeHandler().isItemValid(0, pump));
		assertTrue(wrapper(TankTier.IRON, 1, new AtomicBoolean()).getUpgradeHandler().isItemValid(0, pump));
	}
	@ParameterizedTest
	@MethodSource("allowedUpgrades")
	void isItemValidRestrictsTankUpgradesToFluidCompatibleItems(AllowedUpgrade params) {
		TankStorageWrapper tank = wrapper(TankTier.IRON, 1, new AtomicBoolean());
		ItemStack stack = new ItemStack(params.item());

		boolean allowed = tank.getUpgradeHandler().isItemValid(0, stack);

		assertFalse(stack.isEmpty());
		assertEquals(params.allowed(), allowed);
	}

	private static Stream<Item> pumpItems() {
		return Stream.of(ModItems.PUMP_UPGRADE.get(), ModItems.ADVANCED_PUMP_UPGRADE.get(), ModItems.XP_PUMP_UPGRADE.get());
	}
	private static Stream<Item> magnetItems() {
		return Stream.of(ModItems.MAGNET_UPGRADE.get(), ModItems.ADVANCED_MAGNET_UPGRADE.get());
	}
	private static Stream<PumpTarget> pumpTargets() {
		return pumpItems().flatMap(item -> Stream.of(PumpTargetKind.values()).map(target -> new PumpTarget(item, target)));
	}
	private enum PumpTargetKind {
		TANK, BARREL, OTHER_ITEM, OTHER_FLUID
	}
	private record PumpTarget(Item item, PumpTargetKind target) {
	}
	private record AllowedUpgrade(Item item, boolean allowed) {
	}
	private record TransferLimit(boolean stacked, boolean drain, boolean ignoreLimit, IFluidHandler.FluidAction action) {
	}
	private record VoidFilter(VoidType type, boolean water, boolean full, boolean locked, IFluidHandler.FluidAction action, int accepted, int stored) {
	}
	private static Stream<AllowedUpgrade> allowedUpgrades() {
		Stream<Item> allowed = Stream.concat(pumpItems(), Stream.concat(magnetItems(),
				Stream.of(ModItems.VOID_UPGRADE.get(), ModItems.ADVANCED_VOID_UPGRADE.get(), ModItems.STACK_UPGRADE_TIER_1.get())));
		Stream<Item> forbidden = Stream.of("tank_upgrade", "battery_upgrade")
				.map(name -> BuiltInRegistries.ITEM.get(new ResourceLocation("sophisticatedbackpacks", name)));
		return Stream.concat(allowed.map(item -> new AllowedUpgrade(item, true)),
				Stream.concat(forbidden, Stream.of(ModItems.SMELTING_UPGRADE.get(), ModItems.STACK_DOWNGRADE_TIER_1.get(), Items.DIAMOND))
						.map(item -> new AllowedUpgrade(item, false)));
	}
	private static Stream<TransferLimit> transferLimits() {
		return Stream.of(false, true).flatMap(stacked -> Stream.of(false, true).flatMap(drain -> Stream.of(false, true)
				.flatMap(ignoreLimit -> Stream.of(IFluidHandler.FluidAction.values()).map(action -> new TransferLimit(stacked, drain, ignoreLimit, action)))));
	}
	private static Stream<VoidFilter> voidFilters() {
		return Stream.of(new VoidFilter(VoidType.STORAGE_OVERFLOW, true, false, false, IFluidHandler.FluidAction.SIMULATE, 20000, 0),
				new VoidFilter(VoidType.STORAGE_OVERFLOW, true, false, false, IFluidHandler.FluidAction.EXECUTE, 20000, 16000),
				new VoidFilter(VoidType.STORAGE_OVERFLOW, false, true, false, IFluidHandler.FluidAction.EXECUTE, 0, 16000),
				new VoidFilter(VoidType.STORAGE_OVERFLOW, false, false, false, IFluidHandler.FluidAction.EXECUTE, 16000, 16000),
				new VoidFilter(VoidType.ALWAYS, true, false, false, IFluidHandler.FluidAction.EXECUTE, 20000, 0),
				new VoidFilter(VoidType.ALWAYS, true, false, true, IFluidHandler.FluidAction.EXECUTE, 0, 0));
	}
	private static Config.Server.TankConfig tankConfig(TankTier tier) {
		return switch (tier) {
			case WOOD -> Config.SERVER.woodTank;
			case COPPER -> Config.SERVER.copperTank;
			case IRON -> Config.SERVER.ironTank;
			case GOLD -> Config.SERVER.goldTank;
			case DIAMOND -> Config.SERVER.diamondTank;
			case NETHERITE -> Config.SERVER.netheriteTank;
		};
	}
	private static TankStorageWrapper upgradedWrapper(TankTier tier, int height) {
		TankStorageWrapper wrapper = wrapper(tier, height, new AtomicBoolean());
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		return wrapper;
	}
	private static TankStorageWrapper wrapper(TankTier tier, int height, AtomicBoolean locked) {
		return new TankStorageWrapper(() -> {
		}, () -> {
		}, () -> tier, () -> height, locked::get) {
			@Override
			protected void onUpgradeRefresh() {
			}
			@Override
			public String getStorageType() {
				return "tank";
			}
			@Override
			public Component getDisplayName() {
				return Component.literal("Tank");
			}
		};
	}
}
