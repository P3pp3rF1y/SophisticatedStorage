package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem.Mode;
import net.p3pp3rf1y.sophisticatedstorage.network.ScrolledToolPayload;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TankItemModesTest {
	private static final BlockPos POS = new BlockPos(4, 70, 4);
	private static final BlockHitResult HIT = new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false);
	private MockedStatic<RegistryHelper> registry;

	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Fluids.LAVA.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.IRON_TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModBlocks.TANK_ITEM.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		ModItems.STACK_UPGRADE_TIER_1.get().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.WATER_BUCKET.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}
	@BeforeEach
	void mockRegistryAccess() {
		registry = registry();
	}
	@AfterEach
	void closeRegistryMock() {
		registry.close();
	}

	@ParameterizedTest
	@CsvSource({"PLACE_TANK,true,COLLECT_FLUID", "COLLECT_FLUID,true,DISPENSE_FLUID", "DISPENSE_FLUID,true,PLACE_TANK", "PLACE_TANK,false,DISPENSE_FLUID"})
	void handlePayloadCyclesTheSelectedMode(Mode initial, boolean next, Mode expected) {
		Fixture fixture = fixture(initial);
		IPayloadContext context = mock(IPayloadContext.class);
		when(context.player()).thenReturn(fixture.player);
		when(fixture.player.getMainHandItem()).thenAnswer(invocation -> fixture.held.get());
		ScrolledToolPayload.handlePayload(new ScrolledToolPayload(next), context);
		assertEquals(expected, TankBlockItem.getMode(fixture.held.get()));
	}

	@Test
	void getHighlightTipPreservesTheNameAndIncludesTheMode() {
		Fixture fixture = fixture(Mode.PLACE_TANK);
		ItemStack stack = fixture.held.get();
		Component name = Component.literal("Portable tank");
		Component initial = fixture.item.getHighlightTip(stack, name);
		assertEquals("Portable tank", name.getString());
		assertTrue(initial.getString().startsWith(name.getString()));
		assertTrue(initial.getSiblings().contains(TankBlockItem.getModeMessage(stack)));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void getHighlightTipChangesOnlyForModeChanges(boolean changeMode) {
		Fixture fixture = fixture(Mode.PLACE_TANK);
		ItemStack stack = fixture.held.get();
		Component name = Component.literal("Portable tank");
		Component initial = fixture.item.getHighlightTip(stack, name);
		if (changeMode) {
			TankBlockItem.cycleMode(stack, true);
		} else {
			fill(TankItemStorageWrapper.getCapability(stack), new FluidStack(Fluids.WATER, 1000));
		}
		Component result = fixture.item.getHighlightTip(stack.copy(), name);
		assertEquals(!changeMode, initial.equals(result));
		assertTrue(result.getSiblings().contains(TankBlockItem.getModeMessage(stack)));
	}

	@ParameterizedTest
	@MethodSource("containerTransfers")
	void useTransfersOnlyInTheSelectedDirectionAndRejectsMismatches(ContainerTransfer params) {
		Fixture fixture = fixture(params.mode);
		fill(TankItemStorageWrapper.getCapability(fixture.held.get()), params.held.copy());
		TankFluidHandler target = handler(8000);
		fill(target, params.target.copy());
		when(fixture.level.getCapability(Capabilities.Fluid.BLOCK, POS, Direction.UP)).thenReturn(target);
		InteractionResult result = fixture.use();
		assertEquals(params.success ? InteractionResult.SUCCESS : InteractionResult.FAIL, result);
		int change = params.success ? (params.mode == Mode.COLLECT_FLUID ? 1000 : -1000) : 0;
		assertEquals(params.held.getAmount() + change, contents(fixture).getAmount());
		assertEquals(params.target.getAmount() - change, target.getFluidInTank(0).getAmount());
		verify(fixture.level, never()).setBlock(any(), any(), anyInt());
	}

	private static Stream<ContainerTransfer> containerTransfers() {
		FluidStack water = new FluidStack(Fluids.WATER, 1000);
		FluidStack differentWater = new FluidStack(Fluids.WATER, 2000);
		differentWater.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
		return Stream.of(new ContainerTransfer(Mode.COLLECT_FLUID, FluidStack.EMPTY, new FluidStack(Fluids.WATER, 3000), true),
				new ContainerTransfer(Mode.COLLECT_FLUID, water, FluidStack.EMPTY, false),
				new ContainerTransfer(Mode.DISPENSE_FLUID, water, FluidStack.EMPTY, true),
				new ContainerTransfer(Mode.DISPENSE_FLUID, FluidStack.EMPTY, water, false),
				new ContainerTransfer(Mode.COLLECT_FLUID, water, new FluidStack(Fluids.LAVA, 2000), false),
				new ContainerTransfer(Mode.DISPENSE_FLUID, water, new FluidStack(Fluids.LAVA, 2000), false),
				new ContainerTransfer(Mode.COLLECT_FLUID, water, differentWater, false),
				new ContainerTransfer(Mode.DISPENSE_FLUID, water, differentWater, false));
	}
	private record ContainerTransfer(Mode mode, FluidStack held, FluidStack target, boolean success) {
	}

	@ParameterizedTest
	@CsvSource({"15500, false", "15000, true"})
	void useCollectsAWorldSourceOnlyWhenAWholeBucketFits(int initialAmount, boolean success) {
		Fixture fixture = fixture(Mode.COLLECT_FLUID);
		AtomicReference<BlockState> source = new AtomicReference<>(Blocks.WATER.defaultBlockState());
		when(fixture.level.getBlockState(POS)).thenAnswer(invocation -> source.get());
		when(fixture.level.getFluidState(POS)).thenAnswer(invocation -> source.get().getFluidState());
		when(fixture.level.setBlock(eq(POS), any(), anyInt())).thenAnswer(invocation -> {
			source.set(invocation.getArgument(1));
			return true;
		});
		fill(TankItemStorageWrapper.getCapability(fixture.held.get()), new FluidStack(Fluids.WATER, initialAmount));
		InteractionResult result = fixture.use();
		assertEquals(success ? InteractionResult.SUCCESS : InteractionResult.FAIL, result);
		assertEquals(success, source.get().isAir());
		assertEquals(initialAmount + (success ? 1000 : 0), contents(fixture).getAmount());
		verify(fixture.level, times(success ? 1 : 0)).setBlock(eq(POS), eq(Blocks.AIR.defaultBlockState()), anyInt());
		verify(fixture.level, times(success ? 1 : 0)).playSound(isNull(), eq(4.5), eq(70.5), eq(4.5), eq(SoundEvents.BUCKET_FILL), eq(SoundSource.BLOCKS),
				eq(1.0F), eq(1.0F));
	}

	@ParameterizedTest
	@CsvSource({"16000, true", "500, false"})
	void useDispensesOnlyWholeBucketsIntoTheWorld(int initialAmount, boolean success) {
		Fixture fixture = fixture(Mode.DISPENSE_FLUID);
		fill(TankItemStorageWrapper.getCapability(fixture.held.get()), new FluidStack(Fluids.WATER, initialAmount));
		AtomicReference<BlockState> destination = new AtomicReference<>(Blocks.AIR.defaultBlockState());
		when(fixture.level.getBlockState(POS.above())).thenAnswer(invocation -> destination.get());
		when(fixture.level.isEmptyBlock(POS.above())).thenAnswer(invocation -> destination.get().isAir());
		when(fixture.level.setBlock(eq(POS.above()), any(), anyInt())).thenAnswer(invocation -> {
			destination.set(invocation.getArgument(1));
			return true;
		});
		InteractionResult result = fixture.use();
		assertEquals(success ? InteractionResult.SUCCESS : InteractionResult.FAIL, result);
		assertEquals(success, destination.get().is(Blocks.WATER));
		assertEquals(initialAmount - (success ? 1000 : 0), contents(fixture).getAmount());
		verify(fixture.level, times(success ? 1 : 0)).playSound(isNull(), eq(4.5), eq(71.5), eq(4.5), eq(SoundEvents.BUCKET_EMPTY), eq(SoundSource.BLOCKS),
				eq(1.0F), eq(1.0F));
		verify(fixture.level, never()).playSound(isNull(), eq(POS.above()), any(), any(), anyFloat(), anyFloat());
	}

	@ParameterizedTest
	@CsvSource({"COLLECT_FLUID, false, true", "COLLECT_FLUID, true, true", "DISPENSE_FLUID, false, true", "DISPENSE_FLUID, true, true",
			"COLLECT_FLUID, false, false", "DISPENSE_FLUID, true, false"})
	void usePlaysTheMatchingContainerSoundOnlyOnSuccess(Mode mode, boolean lava, boolean success) {
		Fixture fixture = fixture(mode);
		TankFluidHandler target = handler(8000);
		if (success) {
			if (mode == Mode.COLLECT_FLUID) {
				fill(target, new FluidStack(lava ? Fluids.LAVA : Fluids.WATER, 1000));
			} else {
				fill(TankItemStorageWrapper.getCapability(fixture.held.get()), new FluidStack(lava ? Fluids.LAVA : Fluids.WATER, 1000));
			}
		}
		when(fixture.level.getCapability(Capabilities.Fluid.BLOCK, POS, Direction.UP)).thenReturn(target);
		InteractionResult result = fixture.use();
		assertEquals(success ? InteractionResult.SUCCESS : InteractionResult.FAIL, result);
		verify(fixture.level, times(success ? 1 : 0)).playSound(isNull(), eq(4.5), eq(70.5), eq(4.5),
				eq(mode == Mode.COLLECT_FLUID
						? (lava ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL)
						: (lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY)),
				eq(SoundSource.BLOCKS), eq(1.0F), eq(1.0F));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void useCollectsIntoExactlyOneStackedTankEvenInCreative(boolean creative) {
		Fixture fixture = fixture(Mode.COLLECT_FLUID);
		when(fixture.player.isCreative()).thenReturn(creative);
		fixture.held.get().setCount(64);
		ItemStack inventoryStack = fixture.held.get();
		AtomicReference<ItemStack> received = new AtomicReference<>();
		Inventory inventory = mock(Inventory.class);
		when(fixture.player.getInventory()).thenReturn(inventory);
		when(inventory.add(any(ItemStack.class))).thenAnswer(invocation -> {
			received.set(invocation.getArgument(0));
			return true;
		});
		TankFluidHandler target = handler(8000);
		fill(target, new FluidStack(Fluids.WATER, 1000));
		when(fixture.level.getCapability(Capabilities.Fluid.BLOCK, POS, Direction.UP)).thenReturn(target);
		InteractionResult result = fixture.use();
		assertEquals(InteractionResult.SUCCESS, result);
		assertEquals(63, inventoryStack.getCount());
		assertFalse(inventoryStack.has(ModCoreDataComponents.FLUID_CONTENTS));
		assertEquals(1, received.get().getCount());
		assertEquals(1000, TankItemStorageWrapper.getCapability(received.get()).getAmountAsLong(0));
		assertTrue(target.getFluidInTank(0).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void useDispensesWithoutDuplicatingFluidInCreative(boolean creative) {
		Fixture fixture = fixture(Mode.DISPENSE_FLUID);
		when(fixture.player.isCreative()).thenReturn(creative);
		fill(TankItemStorageWrapper.getCapability(fixture.held.get()), new FluidStack(Fluids.WATER, 1000));
		TankFluidHandler target = handler(8000);
		when(fixture.level.getCapability(Capabilities.Fluid.BLOCK, POS, Direction.UP)).thenReturn(target);
		InteractionResult result = fixture.use();
		assertEquals(InteractionResult.SUCCESS, result);
		assertTrue(contents(fixture).isEmpty());
		assertEquals(1000, target.getFluidInTank(0).getAmount());
		assertEquals(1, fixture.held.get().getCount());
	}

	@Test
	void onItemUseFirstTransfersFluidBeforeBlockInteraction() {
		Fixture fixture = fixture(Mode.COLLECT_FLUID);
		TankFluidHandler target = handler(8000);
		fill(target, new FluidStack(Fluids.WATER, 2000));
		when(fixture.level.getCapability(Capabilities.Fluid.BLOCK, POS, Direction.UP)).thenReturn(target);
		UseOnContext context = new UseOnContext(fixture.level, fixture.player, InteractionHand.MAIN_HAND, fixture.held.get(), HIT);
		InteractionResult result = fixture.item.onItemUseFirst(fixture.held.get(), context);
		assertEquals(InteractionResult.SUCCESS, result);
		assertEquals(1000, target.getFluidInTank(0).getAmount());
		verify(fixture.level, never()).setBlock(any(), any(), anyInt());
	}

	@ParameterizedTest
	@EnumSource(value = Mode.class, names = {"COLLECT_FLUID", "DISPENSE_FLUID"})
	void useOnDoesNotPlaceTanksInFluidModes(Mode mode) {
		Fixture fixture = fixture(mode);
		UseOnContext context = new UseOnContext(fixture.level, fixture.player, InteractionHand.MAIN_HAND, fixture.held.get(), HIT);
		InteractionResult result = fixture.item.useOn(context);
		assertEquals(InteractionResult.FAIL, result);
		verify(fixture.level, never()).setBlock(any(), any(), anyInt());
	}

	@Test
	void useRejectsTransfersWithoutPermission() {
		Fixture fixture = fixture(Mode.COLLECT_FLUID);
		TankFluidHandler target = handler(8000);
		fill(target, new FluidStack(Fluids.WATER, 1000));
		when(fixture.level.getCapability(Capabilities.Fluid.BLOCK, POS, Direction.UP)).thenReturn(target);
		when(fixture.level.mayInteract(fixture.player, POS)).thenReturn(false);
		InteractionResult result = fixture.use();
		assertEquals(InteractionResult.FAIL, result);
		assertEquals(1000, target.getFluidInTank(0).getAmount());
		assertTrue(contents(fixture).isEmpty());
	}

	@Test
	void useLeavesPlaceModeToNormalBlockPlacement() {
		Fixture fixture = fixture(Mode.PLACE_TANK);
		InteractionResult result = fixture.use();
		assertEquals(InteractionResult.PASS, result);
		verify(fixture.level, never()).setBlock(any(), any(), anyInt());
	}

	@Test
	void appendHoverTextCombinesPersistedFluidAndCapacity() {
		Fixture fixture = fixture(Mode.PLACE_TANK);
		fill(TankItemStorageWrapper.getCapability(fixture.held.get()), new FluidStack(Fluids.WATER, 12000));
		List<Component> tooltip = new ArrayList<>();
		fixture.item.appendHoverText(fixture.held.get(), Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, tooltip::add, mock(TooltipFlag.class));
		TranslatableContents fluid = assertInstanceOf(TranslatableContents.class, tooltip.get(1).getContents());
		assertEquals("item.sophisticatedcore.storage.tooltip.fluid_with_capacity", fluid.getKey());
		assertEquals(String.format("%,d", 12000), fluid.getArgs()[0]);
		assertEquals(String.format("%,d", 16000), fluid.getArgs()[1]);
		assertEquals(3, tooltip.size());
	}

	@Test
	void appendHoverTextRetainsCapacityAndModeAfterTheLastDrain() {
		Fixture fixture = fixture(Mode.PLACE_TANK);
		fill(TankItemStorageWrapper.getCapability(fixture.held.get()), new FluidStack(Fluids.WATER, 12000));
		drain(TankItemStorageWrapper.getCapability(fixture.held.get()), 12000);
		List<Component> tooltip = new ArrayList<>();
		fixture.item.appendHoverText(fixture.held.get(), Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, tooltip::add, mock(TooltipFlag.class));
		assertEquals(3, tooltip.size());
		assertEquals("item.sophisticatedstorage.tank.mode", assertInstanceOf(TranslatableContents.class, tooltip.getFirst().getContents()).getKey());
		TranslatableContents empty = assertInstanceOf(TranslatableContents.class, tooltip.get(1).getContents());
		assertEquals("item.sophisticatedstorage.tank.tooltip.capacity", empty.getKey());
		assertArrayEquals(new Object[]{String.format("%,d", 16000)}, empty.getArgs());
	}

	@Test
	void appendHoverTextIncludesHeightAndStackUpgradeCapacity() {
		Fixture fixture = fixture(Mode.PLACE_TANK);
		fixture.held.get().set(ModDataComponents.TANK_HEIGHT, 3);
		new TankItemStorageWrapper(fixture.held.get()).getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		List<Component> tooltip = new ArrayList<>();
		fixture.item.appendHoverText(fixture.held.get(), Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, tooltip::add, mock(TooltipFlag.class));
		assertArrayEquals(new Object[]{String.format("%,d", 96000)}, assertInstanceOf(TranslatableContents.class, tooltip.get(1).getContents()).getArgs());
	}

	@Test
	void appendHoverTextShowsCapacityWithoutWorldRegistryAccess() {
		registry.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.empty());
		List<Component> tooltip = new ArrayList<>();
		ModBlocks.TANK_ITEM.get().appendHoverText(new ItemStack(ModBlocks.TANK_ITEM.get()), Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, tooltip::add,
				mock(TooltipFlag.class));
		TranslatableContents capacity = assertInstanceOf(TranslatableContents.class, tooltip.get(1).getContents());
		assertEquals("item.sophisticatedstorage.tank.tooltip.capacity", capacity.getKey());
		assertArrayEquals(new Object[]{String.format("%,d", 8000)}, capacity.getArgs());
	}

	private static FluidStack contents(Fixture fixture) {
		return TankItemStorageWrapper.getCapability(fixture.held.get()).getResource(0)
				.toStack((int) TankItemStorageWrapper.getCapability(fixture.held.get()).getAmountAsLong(0));
	}
	private static void fill(ResourceHandler<FluidResource> handler, FluidStack fluid) {
		if (fluid.isEmpty()) {
			return;
		}
		int filled = 0;
		while (filled < fluid.getAmount()) {
			try (Transaction tx = Transaction.openRoot()) {
				int inserted = handler.insert(0, FluidResource.of(fluid), fluid.getAmount() - filled, tx);
				assertTrue(inserted > 0, "The fluid could not fit in the tank");
				filled += inserted;
				tx.commit();
			}
		}
		assertEquals(fluid.getAmount(), filled);
	}
	private static void drain(ResourceHandler<FluidResource> handler, int amount) {
		int drained = 0;
		while (drained < amount) {
			try (Transaction tx = Transaction.openRoot()) {
				int extracted = handler.extract(0, handler.getResource(0), amount - drained, tx);
				assertTrue(extracted > 0, "The tank did not contain enough fluid");
				drained += extracted;
				tx.commit();
			}
		}
		assertEquals(amount, drained);
	}
	private static TankFluidHandler handler(int capacity) {
		return new TankFluidHandler(() -> capacity, () -> ItemStack.EMPTY, () -> false, (fluid, type) -> false, () -> {
		}, () -> {
		});
	}
	private static MockedStatic<RegistryHelper> registry() {
		MockedStatic<RegistryHelper> registry = mockStatic(RegistryHelper.class, CALLS_REAL_METHODS);
		registry.when(RegistryHelper::getRegistryAccess).thenReturn(Optional.of(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
		return registry;
	}
	private static Fixture fixture(Mode mode) {
		ServerLevel level = mock(ServerLevel.class);
		Player player = mock(Player.class);
		AtomicReference<ItemStack> held = new AtomicReference<>(new ItemStack(ModBlocks.IRON_TANK_ITEM.get()));
		held.get().set(ModDataComponents.TANK_ITEM_MODE, mode);
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenAnswer(invocation -> held.get());
		doAnswer(invocation -> {
			held.set(invocation.getArgument(1));
			return null;
		}).when(player).setItemInHand(eq(InteractionHand.MAIN_HAND), any());
		when(player.getEyePosition()).thenReturn(Vec3.ZERO);
		when(player.calculateViewVector(anyFloat(), anyFloat())).thenReturn(new Vec3(0, 0, 1));
		when(player.mayUseItemAt(any(), any(), any())).thenReturn(true);
		when(level.clip(any(ClipContext.class))).thenReturn(HIT);
		when(level.mayInteract(eq(player), any())).thenReturn(true);
		when(level.getBlockState(any())).thenReturn(Blocks.STONE.defaultBlockState());
		when(level.dimensionType()).thenReturn(mock(DimensionType.class));
		EnvironmentAttributeSystem attributes = mock(EnvironmentAttributeSystem.class);
		when(attributes.getValue(any(), any(BlockPos.class))).thenReturn(false);
		when(level.environmentAttributes()).thenReturn(attributes);
		return new Fixture(level, player, held, (TankBlockItem) ModBlocks.IRON_TANK_ITEM.get());
	}
	private record Fixture(ServerLevel level, Player player, AtomicReference<ItemStack> held, TankBlockItem item) {
		InteractionResult use() {
			return item.use(level, player, InteractionHand.MAIN_HAND);
		}
	}
}
