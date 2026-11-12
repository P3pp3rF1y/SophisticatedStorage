package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.StorageContentsType;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllableStorage;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
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
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankStorageWrapperTest {
	private MockedStatic<RegistryHelper> registryMock;
	private Map<TankTier, Integer> previousCapacities;
	private Map<TankTier, Integer> previousUpgradeSlots;

	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModFluids.XP_STILL.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		for (String name : List.of("tank_upgrade", "battery_upgrade")) {
			BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("sophisticatedbackpacks", name)).builtInRegistryHolder()
					.bindComponents(DataComponentMap.EMPTY);
		}
		for (Item item : Stream.concat(Stream.of(Items.DIAMOND, ModBlocks.TANK_ITEM.get(), ModBlocks.IRON_TANK_ITEM.get(), ModItems.HOPPER_UPGRADE.get(),
				ModItems.STACK_UPGRADE_TIER_1.get(), ModItems.STACK_UPGRADE_TIER_2.get(), ModItems.STACK_UPGRADE_TIER_5.get(),
				ModItems.STACK_DOWNGRADE_TIER_1.get(), ModItems.ADVANCED_VOID_UPGRADE.get(), ModItems.VOID_UPGRADE.get(), ModItems.SMELTING_UPGRADE.get()),
				Stream.concat(pumpItems(), magnetItems())).toList()) {
			item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		}
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}

	@BeforeEach
	void initializeRegistryAndRememberCapacityConfiguration() {
		registryMock = registry();
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
		registryMock.close();
	}

	@Test
	void insertItemRejectsForbiddenUpgrades() {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 2, new AtomicBoolean());
		ItemStack forbidden = new ItemStack(ModItems.HOPPER_UPGRADE.get());
		assertEquals(0, insertUpgrade(wrapper, 1, forbidden, true));
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(1).isEmpty());
	}

	@Test
	void setStackInSlotRejectsForbiddenUpgrades() {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 2, new AtomicBoolean());
		assertThrows(IllegalArgumentException.class, () -> wrapper.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.HOPPER_UPGRADE.get())));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void extractItemRejectsRemovalWhenFluidWouldExceedRemainingCapacity(boolean commit) {
		TankStorageWrapper wrapper = upgradedWrapper(TankTier.IRON, 2);
		fill(wrapper.getTankFluidHandler(), new FluidStack(Fluids.WATER, 48000), true, true);
		assertEquals(0, extractUpgrade(wrapper, 0, commit));
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_TIER_1));
		assertEquals(64000, wrapper.getCapacity());
		assertEquals(48000, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void setStackInSlotRejectsCapacityReducingReplacements(boolean downgrade) {
		TankStorageWrapper wrapper = upgradedWrapper(TankTier.IRON, 2);
		fill(wrapper.getTankFluidHandler(), new FluidStack(Fluids.WATER, 48000), true, true);
		ItemStack replacement = downgrade ? new ItemStack(ModItems.STACK_DOWNGRADE_TIER_1.get()) : ItemStack.EMPTY;
		assertThrows(IllegalArgumentException.class, () -> wrapper.getUpgradeHandler().setStackInSlot(0, replacement));
		assertTrue(wrapper.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_TIER_1));
	}

	@Test
	void extractItemAllowsCapacityUpgradeRemovalAfterEnoughFluidWasDrained() {
		TankStorageWrapper wrapper = upgradedWrapper(TankTier.IRON, 2);
		fill(wrapper.getTankFluidHandler(), new FluidStack(Fluids.WATER, 48000), true, true);
		extract(wrapper.getTankFluidHandler(), FluidResource.of(Fluids.WATER), 16000, true, true);
		assertEquals(1, extractUpgrade(wrapper, 0, true));
		assertEquals(32000, wrapper.getCapacity());
		assertEquals(32000, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@EnumSource(TankTier.class)
	void getCapacityScalesEachTierConfiguredCapacityByHeightAndUpgrade(TankTier tier) {
		tankConfig(tier).capacity.set(12_345);
		TankStorageWrapper wrapper = upgradedWrapper(tier, 3);
		assertEquals(74_070, wrapper.getCapacity());
		assertEquals(74_070, wrapper.getTankFluidHandler().getTankCapacity(0));
	}

	@ParameterizedTest
	@EnumSource(TankTier.class)
	void loadCanonicalDataRetainsOverCapacityFluidAfterConfiguredCapacityReduction(TankTier tier) {
		tankConfig(tier).capacity.set(16_000);
		TankStorageWrapper original = upgradedWrapper(tier, 3);
		int amount = original.getCapacity() / 2;
		fill(original.getTankFluidHandler(), new FluidStack(Fluids.WATER, amount), true, true);
		TankItemContents.Contents data = original.partitionContents(original.getTankFluidHandler().getFluidInTank(0),
				List.of(original.getUpgradeHandler().getStackInSlot(0)), true);
		tankConfig(tier).capacity.set(1000);
		TankStorageWrapper restored = wrapper(tier, 3, new AtomicBoolean());
		restored.applyPartition(data);
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
		fill(wrapper.getTankFluidHandler(), new FluidStack(Fluids.WATER, amount), true, true);
		tankConfig(tier).capacity.set(1000);
		assertEquals(0, fill(wrapper.getTankFluidHandler(), new FluidStack(Fluids.WATER, 1000), true, true));
		assertEquals(amount, wrapper.getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@EnumSource(TankTier.class)
	void getCapacityClampsConfiguredCapacityOverflowToIntegerMaximum(TankTier tier) {
		tankConfig(tier).capacity.set(Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, upgradedWrapper(tier, 3).getCapacity());
	}

	@Test
	void loadCanonicalDataRestoresFluidAndCapacityUpgradesWithoutItemSlots() {
		TankStorageWrapper original = wrapper(TankTier.DIAMOND, 5, new AtomicBoolean());
		original.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_2.get()));
		fill(original.getTankFluidHandler(), new FluidStack(Fluids.WATER, 200000), true, true);
		TankStorageWrapper restored = wrapper(TankTier.DIAMOND, 5, new AtomicBoolean());
		restored.replaceContents(original.getContents().copy());
		assertEquals(1280000, restored.getCapacity());
		assertEquals(200000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
		assertTrue(restored.getUpgradeHandler().getStackInSlot(0).is(ModItems.STACK_UPGRADE_TIER_2));
		assertEquals(0, restored.getInventoryHandler().size());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void fillUsesTheLiveLockAfterCanonicalContentsAreRestored(boolean matchingFluid) {
		AtomicBoolean locked = new AtomicBoolean();
		TankStorageWrapper original = wrapper(TankTier.DIAMOND, 5, locked);
		fill(original.getTankFluidHandler(), new FluidStack(Fluids.WATER, 200000), true, true);
		TankStorageWrapper restored = wrapper(TankTier.DIAMOND, 5, locked);
		restored.replaceContents(original.getContents().copy());
		locked.set(true);
		assertEquals(matchingFluid ? 1000 : 0,
				fill(restored.getTankFluidHandler(), new FluidStack(matchingFluid ? Fluids.WATER : Fluids.LAVA, 1000), true, true));
		assertEquals(matchingFluid ? 201000 : 200000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@EnumSource(TankTier.class)
	void loadCanonicalDataPreservesUpgradesInAdditionalConfiguredSlots(TankTier tier) {
		tankConfig(tier).upgradeSlotCount.set(7);
		TankStorageWrapper original = wrapper(tier, 1, new AtomicBoolean());
		original.getUpgradeHandler().setStackInSlot(6, new ItemStack(ModItems.PUMP_UPGRADE.get()));
		fill(original.getTankFluidHandler(), new FluidStack(Fluids.WATER, 1000), true, true);
		TankStorageWrapper restored = wrapper(tier, 1, new AtomicBoolean());
		restored.replaceContents(original.getContents().copy());
		assertEquals(7, restored.getUpgradeHandler().size());
		assertTrue(restored.getUpgradeHandler().getStackInSlot(6).is(ModItems.PUMP_UPGRADE.get()));
		assertEquals(1000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@EnumSource(TankTier.class)
	void partitionContentsKeepsUpgradesBeyondTheDefaultTierSlots(TankTier tier) {
		tankConfig(tier).upgradeSlotCount.set(7);
		List<ItemStack> upgrades = new ArrayList<>();
		for (int slot = 0; slot < 6; slot++) {
			upgrades.add(ItemStack.EMPTY);
		}
		upgrades.add(new ItemStack(ModItems.PUMP_UPGRADE.get()));
		TankItemContents.Contents partition = wrapper(tier, 1, new AtomicBoolean()).partitionContents(new FluidStack(Fluids.WATER, 1000), upgrades, false);
		TankStorageWrapper restored = wrapper(tier, 1, new AtomicBoolean());
		restored.applyPartition(partition);
		assertEquals(7, restored.getUpgradeHandler().size());
		assertTrue(restored.getUpgradeHandler().getStackInSlot(6).is(ModItems.PUMP_UPGRADE.get()));
		assertEquals(1000, restored.getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@Test
	void getCapacityDoesNotUseAnotherTierConfiguredCapacity() {
		TankStorageWrapper iron = wrapper(TankTier.IRON, 2, new AtomicBoolean());
		int originalCapacity = iron.getCapacity();
		Config.SERVER.woodTank.capacity.set(123);
		Config.SERVER.goldTank.capacity.set(456);
		assertEquals(originalCapacity, iron.getCapacity());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void fillItemCapabilityMutatesOnlyWhenExecuted(boolean commit) {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get());
		ItemStack original = stack.copy();
		ResourceHandler<FluidResource> capability = TankItemStorageWrapper.getCapability(stack);
		assertNotNull(capability);
		assertEquals(1000, insert(capability, FluidResource.of(Fluids.WATER), 1000, false, commit));
		assertEquals(commit ? 1000 : 0, capability.getAmountAsLong(0));
		if (!commit) {
			assertTrue(ItemStack.isSameItemSameComponents(original, stack), () -> "before=" + original.getComponents() + ", after=" + stack.getComponents());
		}
	}

	@Test
	void fillItemCapabilityReadsExternallyReplacedComponents() {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get());
		ResourceHandler<FluidResource> capability = TankItemStorageWrapper.getCapability(stack);
		assertNotNull(capability);
		insert(capability, FluidResource.of(Fluids.WATER), 1000, false, true);
		stack.remove(ModCoreDataComponents.FLUID_CONTENTS);
		assertEquals(500, insert(capability, FluidResource.of(Fluids.LAVA), 500, false, true));
		assertEquals(FluidResource.of(Fluids.LAVA), capability.getResource(0));
		assertEquals(500, capability.getAmountAsLong(0));
	}

	@Test
	void drainItemCapabilitySimulationLeavesComponentsUnchanged() {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get());
		ResourceHandler<FluidResource> capability = TankItemStorageWrapper.getCapability(stack);
		assertNotNull(capability);
		insert(capability, FluidResource.of(Fluids.LAVA), 500, false, true);
		ItemStack original = stack.copy();
		assertEquals(500, extract(capability, FluidResource.of(Fluids.LAVA), 1000, false, false));
		assertTrue(ItemStack.isSameItemSameComponents(original, stack));
	}

	@Test
	void fillItemCapabilityRejectsStackedPhysicalContainers() {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get(), 64);
		ResourceHandler<FluidResource> capability = TankItemStorageWrapper.getCapability(stack);
		assertNotNull(capability);
		assertEquals(0, insert(capability, FluidResource.of(Fluids.WATER), 1000, false, true));
		assertFalse(stack.has(ModCoreDataComponents.FLUID_CONTENTS));
	}

	@Test
	void onItemUseFirstConsumesOnlyOneMatchingPortableTankInCreative() {
		ServerLevel level = mock(ServerLevel.class);
		when(level.getBlockState(any())).thenReturn(Blocks.AIR.defaultBlockState());
		ServerPlayer player = mock(ServerPlayer.class);
		when(player.isCreative()).thenReturn(true);
		ItemStack stack = spy(new ItemStack(ModBlocks.TANK_ITEM.get(), 64));
		stack.set(ModDataComponents.TANK_HEIGHT, 1);
		BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(BlockPos.ZERO), Direction.UP, BlockPos.ZERO, false));
		doReturn(InteractionResult.SUCCESS).when(stack).useOn(context);
		assertEquals(InteractionResult.SUCCESS, stack.getItem().onItemUseFirst(stack, context));
		assertEquals(63, stack.getCount());
		assertEquals(1, stack.get(ModDataComponents.TANK_HEIGHT));
		verify(player, never()).setItemInHand(any(), any());
	}

	@Test
	void fillingOnePortableTankFromAStackDoesNotMultiplyItsFluid() {
		ItemStack stack = new ItemStack(ModBlocks.TANK_ITEM.get(), 64);
		stack.set(ModDataComponents.TANK_ITEM_MODE, TankBlockItem.Mode.COLLECT_FLUID);
		TankStorageWrapper source = wrapper(TankTier.IRON, 1, new AtomicBoolean());
		fill(source.getTankFluidHandler(), new FluidStack(Fluids.WATER, 2000), true, true);
		ServerLevel level = mock(ServerLevel.class);
		Player player = mock(Player.class);
		Inventory inventory = mock(Inventory.class);
		AtomicReference<ItemStack> received = new AtomicReference<>();
		BlockPos pos = BlockPos.ZERO;
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.add(any(ItemStack.class))).thenAnswer(invocation -> {
			received.set(invocation.getArgument(0));
			return true;
		});
		when(player.getEyePosition()).thenReturn(Vec3.ZERO);
		when(player.calculateViewVector(anyFloat(), anyFloat())).thenReturn(new Vec3(0, 0, 1));
		when(player.mayUseItemAt(any(), any(), any())).thenReturn(true);
		when(level.clip(any(ClipContext.class))).thenReturn(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		when(level.mayInteract(eq(player), any())).thenReturn(true);
		when(level.dimensionType()).thenReturn(mock(DimensionType.class));
		EnvironmentAttributeSystem attributes = mock(EnvironmentAttributeSystem.class);
		when(attributes.getValue(any(), any(BlockPos.class))).thenReturn(false);
		when(level.environmentAttributes()).thenReturn(attributes);
		when(level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP)).thenReturn(source.getTankFluidHandler());
		assertEquals(InteractionResult.SUCCESS, stack.getItem().use(level, player, InteractionHand.MAIN_HAND));
		assertEquals(63, stack.getCount());
		assertFalse(stack.has(ModCoreDataComponents.FLUID_CONTENTS));
		assertEquals(1, received.get().getCount());
		assertEquals(1000, TankItemStorageWrapper.getCapability(received.get()).getAmountAsLong(0));
		assertEquals(1000, source.getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@Test
	void getExternalFluidHandlerExposesSynchronizedClientContentsForStandardFluidTooltips() {
		Level level = mock(Level.class);
		when(level.isClientSide()).thenReturn(true);
		when(level.registryAccess()).thenReturn(registries());
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		fill(tank.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 6000), true, true);
		IStorageFluidHandler capability = tank.getExternalFluidHandler(null);
		assertNotNull(capability);
		assertEquals(6000, capability.getAmountAsLong(0));
		assertEquals(16000, capability.getCapacityAsLong(0, FluidResource.of(Fluids.WATER)));
	}

	@Test
	void getExternalFluidHandlerStopsServingRemovedPhysicalOwners() {
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(registries());
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		IStorageFluidHandler retained = tank.getExternalFluidHandler(null);
		assertNotNull(retained);
		assertEquals(1000, insert(retained, FluidResource.of(Fluids.WATER), 1000, false, true));
		tank.setRemoved();
		assertEquals(0, retained.size());
		assertEquals(0, insert(retained, FluidResource.of(Fluids.WATER), 1000, false, true));
		assertEquals(0, extract(retained, FluidResource.of(Fluids.WATER), 1000, false, true));
		assertEquals(1000, tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void onItemUseFirstRestoresExistingGroupMetadataAfterCancelledExtension(int height) {
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(registries());
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
		fill(primary.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 1000), true, true);
		ItemStack stack = spy(new ItemStack(ModBlocks.IRON_TANK_ITEM.get()));
		TankItemStorageWrapper incoming = new TankItemStorageWrapper(stack);
		incoming.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		fill(incoming.getTankFluidHandler(), new FluidStack(Fluids.WATER, 20000), true, true);
		BlockPos top = BlockPos.ZERO.above(height - 1);
		BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false));
		doAnswer(invocation -> {
			primary.extendWith(stack);
			members.remove(BlockPos.ZERO.above(height));
			return InteractionResult.FAIL;
		}).when(stack).useOn(context);
		assertEquals(InteractionResult.FAIL, stack.getItem().onItemUseFirst(stack, context));
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
		when(level.registryAccess()).thenReturn(registries());
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
		IStorageFluidHandler retained = controller.getExternalFluidHandler(null);
		if (removed) {
			members.remove(tank);
		}
		assertEquals(removed ? 0 : 1, retained.size());
		assertEquals(removed ? 0 : 1000, insert(retained, FluidResource.of(Fluids.WATER), 1000, false, true));
		verifyNoInteractions(ordinary);
	}

	@Test
	void drainUsesAReaddedMemberThroughTheRetainedControllerHandler() {
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(registries());
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		fill(tank.getStorageWrapper().getTankFluidHandler(), new FluidStack(Fluids.WATER, 1000), true, true);
		List<IControllableStorage> members = new ArrayList<>(List.of(tank));
		ControllerBlockEntity controller = new ControllerBlockEntity(BlockPos.ZERO.west(), ModBlocks.CONTROLLER.get().defaultBlockState()) {
			@Override
			protected List<IControllableStorage> getLoadedStorageRepresentatives() {
				return members;
			}
		};
		controller.setLevel(level);
		IStorageFluidHandler retained = controller.getExternalFluidHandler(null);
		members.clear();
		assertEquals(0, retained.size());
		members.add(tank);
		assertEquals(1000, extract(retained, FluidResource.of(Fluids.WATER), 1000, false, true));
		assertEquals(0, tank.getStorageWrapper().getTankFluidHandler().getFluidInTank(0).getAmount());
	}

	@Test
	void clearContentRemovesFluidBeforeCapacityProvidingUpgrades() {
		TankBlockEntity tank = tankWithLevel();
		TankStorageWrapper contents = tank.getStorageWrapper();
		contents.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		fill(contents.getTankFluidHandler(), new FluidStack(Fluids.WATER, 30000), true, true);

		tank.clearContent();

		assertTrue(contents.getTankFluidHandler().getFluidInTank(0).isEmpty());
		assertTrue(contents.getUpgradeHandler().getStackInSlot(0).isEmpty());
	}

	@Test
	void clearContentRemovesLinkedFluidBeforeCapacityProvidingUpgrades() {
		TankBlockEntity tank = tankWithLevel();
		TankStorageWrapper contents = wrapper(TankTier.IRON, 1, new AtomicBoolean());
		tank.getStorageWrapper().setLinkedStorage(contents);
		contents.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		fill(contents.getTankFluidHandler(), new FluidStack(Fluids.WATER, 30000), true, true);

		tank.clearContent();

		assertTrue(contents.getTankFluidHandler().getFluidInTank(0).isEmpty());
		assertTrue(contents.getUpgradeHandler().getStackInSlot(0).isEmpty());
	}

	@ParameterizedTest
	@CsvSource({"false, 100", "false, 105", "true, 105"})
	void tickExperiencePumpHonorsCooldownAndScaledTransferLimit(boolean stacked, long nextTick) {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 5, new AtomicBoolean());
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.XP_PUMP_UPGRADE.get()));
		XpPumpUpgradeWrapper pump = wrapper.getUpgradeHandler().getTypeWrappers(XpPumpUpgradeItem.TYPE).getFirst();
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
		int transferred = nextTick == 100 ? 0 : (int) Math.max(FluidType.BUCKET_VOLUME, 20 * 5 * wrapper.getStackMultiplier());
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
		XpPumpUpgradeWrapper pump = wrapper.getUpgradeHandler().getTypeWrappers(XpPumpUpgradeItem.TYPE).getFirst();
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
			fill(handler, new FluidStack(Fluids.WATER, 100000), true, true);
		}
		int available = Math.min(100000, wrapper.getCapacity());
		int limit = params.ignoreLimit() ? available : Math.min(available, (int) Math.max(FluidType.BUCKET_VOLUME, 20 * 5 * wrapper.getStackMultiplier()));
		int transferred = params.drain()
				? extract(handler, FluidResource.of(Fluids.WATER), 100000, params.ignoreLimit(), params.commit())
				: fill(handler, new FluidStack(Fluids.WATER, 100000), params.ignoreLimit(), params.commit());
		assertEquals(limit, transferred);
		int initial = params.drain() ? available : 0;
		assertEquals(initial + (params.commit() ? (params.drain() ? -limit : limit) : 0), handler.getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@MethodSource("voidFilters")
	void fillAppliesInstalledVoidFluidFilters(VoidFilter params) {
		AtomicBoolean locked = new AtomicBoolean();
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 1, locked);
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.ADVANCED_VOID_UPGRADE.get()));
		VoidUpgradeWrapper voidUpgrade = wrapper.getUpgradeHandler().getTypeWrappers(VoidUpgradeItem.TYPE).getFirst();
		voidUpgrade.getFilterLogic().setAllowList(true);
		voidUpgrade.getFluidFilterLogic().setFluid(0, new FluidStack(Fluids.WATER, 1));
		voidUpgrade.setVoidType(params.type());
		TankFluidHandler handler = wrapper.getTankFluidHandler();
		if (params.full()) {
			fill(handler, new FluidStack(Fluids.WATER, 16000), true, true);
		}
		locked.set(params.locked());
		assertEquals(params.accepted(), fill(handler, new FluidStack(params.water() ? Fluids.WATER : Fluids.LAVA, 20000), true, params.commit()));
		assertEquals(params.stored(), handler.getFluidInTank(0).getAmount());
	}

	@ParameterizedTest
	@MethodSource("magnetItems")
	void pickupRejectsItemsWhileTheMagnetRetainsXpSupport(Item item) {
		TankStorageWrapper wrapper = wrapper(TankTier.IRON, 1, new AtomicBoolean());
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(item));
		MagnetUpgradeWrapper magnet = wrapper.getUpgradeHandler().getTypeWrappers(MagnetUpgradeItem.TYPE).getFirst();
		magnet.setPickupItems(true);
		ItemStack input = new ItemStack(Items.DIAMOND, 3);
		int pickedUp;
		try (Transaction tx = Transaction.openRoot()) {
			pickedUp = magnet.pickup(null, ItemResource.of(input), input.getCount(), tx);
		}
		assertFalse(magnet.supportsItemPickup());
		assertFalse(magnet.shouldPickupItems());
		assertTrue(magnet.shouldPickupXp());
		assertEquals(0, pickedUp);
		assertEquals(3, input.getCount());
		assertEquals(0, wrapper.getInventoryHandler().size());
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
		assertEquals(params.target() == PumpTargetKind.TANK || params.target() == PumpTargetKind.OTHER_FLUID,
				upgrade.canAddUpgradeTo(target, pump, true, true).successful());
	}

	@ParameterizedTest
	@MethodSource("pumpItems")
	void insertItemRejectsStoragePumpsInOrdinaryStorage(Item item) {
		StorageWrapper barrel = new BarrelBlockEntity(BlockPos.ZERO, ModBlocks.BARREL.get().defaultBlockState()).getStorageWrapper();
		ItemStack pump = new ItemStack(item);
		assertEquals(0, insertUpgrade(barrel, 0, pump, true));
		assertFalse(barrel.getUpgradeHandler().isValid(0, ItemResource.of(pump)));
		assertTrue(wrapper(TankTier.IRON, 1, new AtomicBoolean()).getUpgradeHandler().isValid(0, ItemResource.of(pump)));
	}

	@ParameterizedTest
	@MethodSource("allowedUpgrades")
	void isItemValidRestrictsTankUpgradesToFluidCompatibleItems(AllowedUpgrade params) {
		TankStorageWrapper tank = wrapper(TankTier.IRON, 1, new AtomicBoolean());
		ItemStack stack = new ItemStack(params.item());
		assertFalse(stack.isEmpty());
		assertEquals(params.allowed(), tank.getUpgradeHandler().isValid(0, ItemResource.of(stack)));
	}

	private static int insertUpgrade(IStorageWrapper storage, int slot, ItemStack stack, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int inserted = storage.getUpgradeHandler().insert(slot, ItemResource.of(stack), stack.getCount(), tx);
			if (commit) {
				tx.commit();
			}
			return inserted;
		}
	}

	private static int extractUpgrade(TankStorageWrapper storage, int slot, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int removed = storage.getUpgradeHandler().extract(slot, ItemResource.of(storage.getUpgradeHandler().getStackInSlot(slot)), 1, tx);
			if (commit) {
				tx.commit();
			}
			return removed;
		}
	}

	private static int fill(TankFluidHandler handler, FluidStack fluid, boolean ignoreLimit, boolean commit) {
		return insert(handler, FluidResource.of(fluid), fluid.getAmount(), ignoreLimit, commit);
	}

	private static int insert(ResourceHandler<FluidResource> handler, FluidResource fluid, int amount, boolean ignoreLimit, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int inserted = handler instanceof IStorageFluidHandler storage
					? storage.insert(0, fluid, amount, tx, ignoreLimit)
					: handler.insert(0, fluid, amount, tx);
			if (commit) {
				tx.commit();
			}
			return inserted;
		}
	}

	private static int extract(ResourceHandler<FluidResource> handler, FluidResource fluid, int amount, boolean ignoreLimit, boolean commit) {
		try (Transaction tx = Transaction.openRoot()) {
			int extracted = handler instanceof IStorageFluidHandler storage
					? storage.extract(0, fluid, amount, tx, ignoreLimit)
					: handler.extract(0, fluid, amount, tx);
			if (commit) {
				tx.commit();
			}
			return extracted;
		}
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

	private record TransferLimit(boolean stacked, boolean drain, boolean ignoreLimit, boolean commit) {
	}

	private record VoidFilter(VoidType type, boolean water, boolean full, boolean locked, boolean commit, int accepted, int stored) {
	}

	private static Stream<AllowedUpgrade> allowedUpgrades() {
		Stream<Item> allowed = Stream.concat(pumpItems(), Stream.concat(magnetItems(),
				Stream.of(ModItems.VOID_UPGRADE.get(), ModItems.ADVANCED_VOID_UPGRADE.get(), ModItems.STACK_UPGRADE_TIER_1.get())));
		Stream<Item> forbidden = Stream.of("tank_upgrade", "battery_upgrade")
				.map(name -> BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("sophisticatedbackpacks", name)));
		return Stream.concat(allowed.map(item -> new AllowedUpgrade(item, true)),
				Stream.concat(forbidden, Stream.of(ModItems.SMELTING_UPGRADE.get(), ModItems.STACK_DOWNGRADE_TIER_1.get(), Items.DIAMOND))
						.map(item -> new AllowedUpgrade(item, false)));
	}

	private static Stream<TransferLimit> transferLimits() {
		return Stream.of(false, true).flatMap(stacked -> Stream.of(false, true).flatMap(drain -> Stream.of(false, true)
				.flatMap(ignoreLimit -> Stream.of(false, true).map(commit -> new TransferLimit(stacked, drain, ignoreLimit, commit)))));
	}

	private static Stream<VoidFilter> voidFilters() {
		return Stream.of(new VoidFilter(VoidType.STORAGE_OVERFLOW, true, false, false, false, 20000, 0),
				new VoidFilter(VoidType.STORAGE_OVERFLOW, true, false, false, true, 20000, 16000),
				new VoidFilter(VoidType.STORAGE_OVERFLOW, false, true, false, true, 0, 16000),
				new VoidFilter(VoidType.STORAGE_OVERFLOW, false, false, false, true, 16000, 16000),
				new VoidFilter(VoidType.ALWAYS, true, false, false, true, 20000, 0), new VoidFilter(VoidType.ALWAYS, true, false, true, true, 0, 0));
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

	private static TankBlockEntity tankWithLevel() {
		ServerLevel level = mock(ServerLevel.class);
		when(level.registryAccess()).thenReturn(registries());
		TankBlockEntity tank = new TankBlockEntity(BlockPos.ZERO, ModBlocks.IRON_TANK.get().defaultBlockState());
		tank.setLevel(level);
		return tank;
	}

	private static TankStorageWrapper upgradedWrapper(TankTier tier, int height) {
		TankStorageWrapper wrapper = wrapper(tier, height, new AtomicBoolean());
		wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		return wrapper;
	}

	private static RegistryAccess registries() {
		return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
	}

	private static MockedStatic<RegistryHelper> registry() {
		MockedStatic<RegistryHelper> registry = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		registry.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(registries()));
		return registry;
	}

	private static TankStorageWrapper wrapper(TankTier tier, int height, AtomicBoolean locked) {
		return new TankStorageWrapper(() -> {
		}, () -> {
		}, () -> tier, () -> height, TankStorageWrapperTest::registries, locked::get) {
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
