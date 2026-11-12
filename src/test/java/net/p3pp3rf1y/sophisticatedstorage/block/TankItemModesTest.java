package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem.Mode;
import net.p3pp3rf1y.sophisticatedstorage.network.ScrolledToolMessage;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TankItemModesTest {
	private static final BlockPos POS = new BlockPos(4, 70, 4);
	private static final BlockHitResult HIT = new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false);
	private MockedStatic<FluidUtil> fluids;
	@BeforeEach
	void mockFluidLookup() {
		fluids = mockStatic(FluidUtil.class, CALLS_REAL_METHODS);
	}
	@AfterEach
	void closeFluidLookup() {
		fluids.close();
	}
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
	@CsvSource({"PLACE_TANK,true,COLLECT_FLUID", "COLLECT_FLUID,true,DISPENSE_FLUID", "DISPENSE_FLUID,true,PLACE_TANK", "PLACE_TANK,false,DISPENSE_FLUID"})
	void handlePayloadCyclesTheSelectedMode(Mode initial, boolean next, Mode expected) throws ReflectiveOperationException {
		Fixture fixture = fixture(initial);
		ServerPlayer player = mock(ServerPlayer.class);
		when(player.getMainHandItem()).thenReturn(fixture.held.get());
		Method handler = ScrolledToolMessage.class.getDeclaredMethod("handleMessage", ServerPlayer.class, ScrolledToolMessage.class);
		handler.setAccessible(true);
		handler.invoke(null, player, new ScrolledToolMessage(next));
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
			TankItemStorageWrapper.getCapability(stack).fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		}
		Component result = fixture.item.getHighlightTip(stack.copy(), name);
		assertEquals(!changeMode, initial.equals(result));
		assertTrue(result.getSiblings().contains(TankBlockItem.getModeMessage(stack)));
	}
	@ParameterizedTest
	@MethodSource("containerTransfers")
	void useTransfersOnlyInTheSelectedDirectionAndRejectsMismatches(ContainerTransfer params) {
		Fixture fixture = fixture(params.mode);
		TankItemStorageWrapper.getCapability(fixture.held.get()).fill(params.held.copy(), FluidAction.EXECUTE);
		TankFluidHandler target = handler(8000);
		target.fill(params.target.copy(), FluidAction.EXECUTE);
		fixture.target.set(target);
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
		CompoundTag tag = new CompoundTag();
		tag.putString("identity", "different");
		differentWater.setTag(tag);
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
		when(fixture.level.getFluidState(POS))
				.thenAnswer(invocation -> source.get().isAir() ? Fluids.EMPTY.defaultFluidState() : Fluids.WATER.defaultFluidState());
		when(fixture.level.setBlock(eq(POS), any(), anyInt())).thenAnswer(invocation -> {
			source.set(invocation.getArgument(1));
			return true;
		});
		IFluidHandlerItem itemHandler = TankItemStorageWrapper.getCapability(fixture.held.get());
		itemHandler.fill(new FluidStack(Fluids.WATER, initialAmount), FluidAction.EXECUTE);
		InteractionResult result = fixture.use();
		assertEquals(success ? InteractionResult.SUCCESS : InteractionResult.FAIL, result);
		assertEquals(success, source.get().isAir());
		assertEquals(initialAmount + (success ? 1000 : 0), contents(fixture).getAmount());
		verify(fixture.level, times(success ? 1 : 0)).playSound(null, POS, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
	}
	@ParameterizedTest
	@CsvSource({"16000, true", "500, false"})
	void useDispensesOnlyWholeBucketsIntoTheWorld(int initialAmount, boolean success) {
		Fixture fixture = fixture(Mode.DISPENSE_FLUID);
		TankItemStorageWrapper.getCapability(fixture.held.get()).fill(new FluidStack(Fluids.WATER, initialAmount), FluidAction.EXECUTE);
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
		verify(fixture.level, times(success ? 1 : 0)).playSound(fixture.player, POS.above(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
		verify(fixture.player, times(success ? 1 : 0)).playNotifySound(SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
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
				target.fill(new FluidStack(lava ? Fluids.LAVA : Fluids.WATER, 1000), FluidAction.EXECUTE);
			} else {
				TankItemStorageWrapper.getCapability(fixture.held.get()).fill(new FluidStack(lava ? Fluids.LAVA : Fluids.WATER, 1000), FluidAction.EXECUTE);
			}
		}
		fixture.target.set(target);
		SoundEvent sound = mode == Mode.COLLECT_FLUID
				? (lava ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL)
				: lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
		InteractionResult result = fixture.use();
		assertEquals(success ? InteractionResult.SUCCESS : InteractionResult.FAIL, result);
		verify(fixture.level, times(success ? 1 : 0)).playSound(null, POS, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
	}
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void useCollectsIntoExactlyOneStackedTankEvenInCreative(boolean creative) {
		try (MockedStatic<ItemHandlerHelper> delivery = mockStatic(ItemHandlerHelper.class, CALLS_REAL_METHODS)) {
			Fixture fixture = fixture(Mode.COLLECT_FLUID);
			when(fixture.player.isCreative()).thenReturn(creative);
			fixture.held.get().setCount(64);
			AtomicReference<ItemStack> received = new AtomicReference<>();
			delivery.when(() -> ItemHandlerHelper.giveItemToPlayer(eq(fixture.player), any())).thenAnswer(invocation -> {
				received.set(invocation.getArgument(1));
				return null;
			});
			TankFluidHandler target = handler(8000);
			target.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
			fixture.target.set(target);
			InteractionResult result = fixture.use();
			assertEquals(InteractionResult.SUCCESS, result);
			assertEquals(63, fixture.held.get().getCount());
			assertTrue(new TankItemStorageWrapper(fixture.held.get()).getTankFluidHandler().getFluidInTank(0).isEmpty());
			assertEquals(1, received.get().getCount());
			assertEquals(1000, TankItemStorageWrapper.getCapability(received.get()).getFluidInTank(0).getAmount());
			assertTrue(target.getFluidInTank(0).isEmpty());
		}
	}
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void useDispensesWithoutDuplicatingFluidInCreative(boolean creative) {
		Fixture fixture = fixture(Mode.DISPENSE_FLUID);
		when(fixture.player.isCreative()).thenReturn(creative);
		TankItemStorageWrapper.getCapability(fixture.held.get()).fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		TankFluidHandler target = handler(8000);
		fixture.target.set(target);
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
		target.fill(new FluidStack(Fluids.WATER, 2000), FluidAction.EXECUTE);
		fixture.target.set(target);
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
		target.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		fixture.target.set(target);
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
		TankItemStorageWrapper.getCapability(fixture.held.get()).fill(new FluidStack(Fluids.WATER, 12_000), FluidAction.EXECUTE);
		List<Component> tooltip = new ArrayList<>();
		fixture.item.appendHoverText(fixture.held.get(), fixture.level, tooltip, mock(TooltipFlag.class));
		assertTrue(tooltip.get(1).getString().contains("12,000") || tooltip.get(1).getString().contains("12000"));
		assertTrue(tooltip.get(1).getString().contains("16,000") || tooltip.get(1).getString().contains("16000"));
		assertEquals(3, tooltip.size());
	}
	@Test
	void appendHoverTextRetainsCapacityAndModeAfterTheLastDrain() {
		Fixture fixture = fixture(Mode.PLACE_TANK);
		TankItemStorageWrapper.getCapability(fixture.held.get()).fill(new FluidStack(Fluids.WATER, 12_000), FluidAction.EXECUTE);
		TankItemStorageWrapper.getCapability(fixture.held.get()).drain(12_000, FluidAction.EXECUTE);
		List<Component> tooltip = new ArrayList<>();
		fixture.item.appendHoverText(fixture.held.get(), fixture.level, tooltip, mock(TooltipFlag.class));
		assertEquals(3, tooltip.size());
		assertTrue(tooltip.get(0).getString().contains("place_tank") || tooltip.get(0).getString().contains("tank.mode"));
		assertTrue(tooltip.get(1).getString().contains("16000"));
	}
	@Test
	void appendHoverTextIncludesHeightAndStackUpgradeCapacity() {
		Fixture fixture = fixture(Mode.PLACE_TANK);
		TankItemContents.height(fixture.held.get(), 3);
		new TankItemStorageWrapper(fixture.held.get()).getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
		List<Component> tooltip = new ArrayList<>();
		fixture.item.appendHoverText(fixture.held.get(), fixture.level, tooltip, mock(TooltipFlag.class));
		assertTrue(tooltip.get(1).getString().contains("96000"));
	}
	@Test
	void appendHoverTextShowsCapacityWithoutWorldRegistryAccess() {
		List<Component> tooltip = new ArrayList<>();
		ModBlocks.TANK_ITEM.get().appendHoverText(new ItemStack(ModBlocks.TANK_ITEM.get()), null, tooltip, mock(TooltipFlag.class));
		assertTrue(tooltip.get(1).getString().contains("8000"));
	}
	private static FluidStack contents(Fixture fixture) {
		return TankItemStorageWrapper.getCapability(fixture.held.get()).getFluidInTank(0);
	}
	private static TankFluidHandler handler(int capacity) {
		return new TankFluidHandler(() -> capacity, () -> ItemStack.EMPTY, () -> false, (fluid, type) -> false, () -> {
		}, () -> {
		});
	}
	private Fixture fixture(Mode mode) {
		ServerLevel level = mock(ServerLevel.class);
		Player player = mock(Player.class);
		AtomicReference<ItemStack> held = new AtomicReference<>(new ItemStack(ModBlocks.IRON_TANK_ITEM.get()));
		TankItemContents.mode(held.get(), switch (mode) {
			case PLACE_TANK -> "place_tank";
			case COLLECT_FLUID -> "collect_fluid";
			case DISPENSE_FLUID -> "dispense_fluid";
		});
		AtomicReference<TankFluidHandler> target = new AtomicReference<>();
		when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenAnswer(invocation -> held.get());
		doAnswer(invocation -> {
			held.set(invocation.getArgument(1));
			return null;
		}).when(player).setItemInHand(eq(InteractionHand.MAIN_HAND), any());
		when(player.getEyePosition()).thenReturn(Vec3.ZERO);
		when(player.getViewVector(anyFloat())).thenReturn(new Vec3(0, 0, 1));
		when(player.mayUseItemAt(any(), any(), any())).thenReturn(true);
		when(level.clip(any(ClipContext.class))).thenReturn(HIT);
		when(level.mayInteract(eq(player), any())).thenReturn(true);
		when(level.getBlockState(any())).thenReturn(Blocks.STONE.defaultBlockState());
		when(level.getFluidState(any())).thenReturn(Fluids.EMPTY.defaultFluidState());
		when(level.dimensionType()).thenReturn(mock(DimensionType.class));
		fluids.when(() -> FluidUtil.getFluidHandler(level, POS, Direction.UP))
				.thenAnswer(invocation -> target.get() == null ? LazyOptional.empty() : LazyOptional.of(target::get));
		return new Fixture(level, player, held, (TankBlockItem) ModBlocks.IRON_TANK_ITEM.get(), target);
	}
	private record Fixture(ServerLevel level, Player player, AtomicReference<ItemStack> held, TankBlockItem item, AtomicReference<TankFluidHandler> target) {
		InteractionResult use() {
			return item.use(level, player, InteractionHand.MAIN_HAND).getResult();
		}
	}
}
