package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.ItemHandlerHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.TankBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.TankItemContents;
import net.p3pp3rf1y.sophisticatedstorage.block.TankItemLinkedStorageEndpointAdapter;
import net.p3pp3rf1y.sophisticatedstorage.block.TankItemStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.TankTier;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TankBlockItem extends BlockItem {
	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		AtomicReference<TooltipComponent> tooltip = new AtomicReference<>();
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> tooltip.set(StorageItemClient.getLinkedStorageTooltipImage(stack)));
		return Optional.ofNullable(tooltip.get());
	}
	public TankBlockItem(TankBlock block) {
		super(block, new Properties().stacksTo(64));
	}
	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private final NonNullLazy<net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer> renderer = NonNullLazy
					.of(net.p3pp3rf1y.sophisticatedstorage.client.render.TankItemRenderer::new);
			@Override
			public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer.get();
			}
		});
	}
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new ICapabilityProvider() {
			private final LazyOptional<IFluidHandlerItem> fluid = LazyOptional.of(() -> TankItemStorageWrapper.getCapability(stack));
			@Override
			public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
				return cap == ForgeCapabilities.FLUID_HANDLER_ITEM ? fluid.cast() : LazyOptional.empty();
			}
		};
	}

	public enum Mode {
		PLACE_TANK("place_tank"), COLLECT_FLUID("collect_fluid"), DISPENSE_FLUID("dispense_fluid");
		private final String name;
		Mode(String name) {
			this.name = name;
		}
		public Component getDisplayName() {
			return Component.translatable("item.sophisticatedstorage.tank.mode." + name);
		}
	}

	public static Mode getMode(ItemStack stack) {
		String name = TankItemContents.mode(stack);
		for (Mode mode : Mode.values()) {
			if (mode.name.equals(name)) {
				return mode;
			}
		}
		return Mode.PLACE_TANK;
	}
	public static void cycleMode(ItemStack stack, boolean next) {
		Mode[] modes = Mode.values();
		TankItemContents.mode(stack, modes[Math.floorMod(getMode(stack).ordinal() + (next ? 1 : -1), modes.length)].name);
	}
	public static Component getModeMessage(ItemStack stack) {
		return Component.translatable("item.sophisticatedstorage.tank.mode", getMode(stack).getDisplayName().copy().withStyle(ChatFormatting.WHITE))
				.withStyle(ChatFormatting.GOLD);
	}
	@Override
	public Component getHighlightTip(ItemStack stack, Component displayName) {
		return displayName.copy().append(" - ").append(getModeMessage(stack));
	}
	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(getModeMessage(stack));
		TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
		FluidStack contents = wrapper.getTankFluidHandler().getFluidInTank(0);
		tooltip.add(Component
				.literal(contents.isEmpty()
						? "Capacity: " + wrapper.getCapacity() + " mB"
						: contents.getDisplayName().getString() + ": " + contents.getAmount() + " / " + wrapper.getCapacity() + " mB")
				.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("storage_tool") + ".controls", Component
				.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("storage_tool") + ".controls.combination").withStyle(ChatFormatting.AQUA))
				.withStyle(ChatFormatting.GRAY));
	}
	@Override
	public InteractionResult useOn(UseOnContext context) {
		return getMode(context.getItemInHand()) == Mode.PLACE_TANK ? super.useOn(context) : InteractionResult.FAIL;
	}
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Mode mode = getMode(stack);
		if (mode == Mode.PLACE_TANK) {
			return super.use(level, player, hand);
		}
		BlockHitResult hit = getPlayerPOVHitResult(level, player, mode == Mode.COLLECT_FLUID ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
		BlockPos pos = hit.getBlockPos();
		if (hit.getType() != HitResult.Type.BLOCK || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)
				|| !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), stack)) {
			return InteractionResultHolder.fail(stack);
		}
		if (level.isClientSide) {
			return InteractionResultHolder.success(stack);
		}
		ItemStack single = stack.copy();
		single.setCount(1);
		IFluidHandlerItem handler = TankItemStorageWrapper.getCapability(single);
		if (handler == null || !transferFluid(level, player, hand, hit, handler, mode)) {
			return InteractionResultHolder.fail(stack);
		}
		if (stack.getCount() == 1) {
			player.setItemInHand(hand, handler.getContainer());
		} else {
			stack.shrink(1);
			ItemHandlerHelper.giveItemToPlayer(player, handler.getContainer());
		}
		return InteractionResultHolder.success(player.getItemInHand(hand));
	}
	private boolean transferFluid(Level level, Player player, InteractionHand hand, BlockHitResult hit, IFluidHandlerItem handler, Mode mode) {
		BlockPos pos = hit.getBlockPos();
		IFluidHandler target = FluidUtil.getFluidHandler(level, pos, hit.getDirection()).orElse(null);
		BlockState state = level.getBlockState(pos);
		if (mode == Mode.COLLECT_FLUID) {
			return collectFluid(level, player, pos, state, handler, target);
		}
		if (target != null) {
			return dispenseIntoContainer(level, pos, handler, target);
		}
		return dispenseIntoWorld(level, player, hand, hit, state, handler);
	}
	private boolean collectFluid(Level level, Player player, BlockPos pos, BlockState state, IFluidHandlerItem handler, @Nullable IFluidHandler source) {
		SoundEvent pickupSound = null;
		if (source == null && state.getBlock() instanceof BucketPickup pickup) {
			source = new BucketPickupHandlerWrapper(pickup, level, pos);
			pickupSound = pickup.getPickupSound(state).orElse(null);
		}
		if (source == null) {
			return false;
		}
		FluidStack transferred = FluidUtil.tryFluidTransfer(handler, source, FluidType.BUCKET_VOLUME, true);
		if (transferred.isEmpty()) {
			return false;
		}
		playTransferSound(level, pos,
				pickupSound != null ? pickupSound : transferred.getFluid().getFluidType().getSound(transferred, SoundActions.BUCKET_FILL));
		return true;
	}
	private boolean dispenseIntoContainer(Level level, BlockPos pos, IFluidHandlerItem handler, IFluidHandler target) {
		FluidStack transferred = FluidUtil.tryFluidTransfer(target, handler, FluidType.BUCKET_VOLUME, true);
		if (transferred.isEmpty()) {
			return false;
		}
		playTransferSound(level, pos, transferred.getFluid().getFluidType().getSound(transferred, SoundActions.BUCKET_EMPTY));
		return true;
	}
	private void playTransferSound(Level level, BlockPos pos, @Nullable SoundEvent sound) {
		if (sound != null) {
			level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}
	private boolean dispenseIntoWorld(Level level, Player player, InteractionHand hand, BlockHitResult hit, BlockState state, IFluidHandlerItem handler) {
		FluidStack fluid = handler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
		if (fluid.getAmount() < FluidType.BUCKET_VOLUME) {
			return false;
		}
		BlockPos pos = hit.getBlockPos();
		BlockPos destination = state.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(level, pos, state, fluid.getFluid())
				? pos
				: pos.relative(hit.getDirection());
		if (!level.mayInteract(player, destination) || !player.mayUseItemAt(destination, hit.getDirection(), player.getItemInHand(hand))) {
			return false;
		}
		boolean vaporizes = fluid.getFluid().getFluidType().isVaporizedOnPlacement(level, destination, fluid);
		if (!FluidUtil.tryPlaceFluid(player, level, hand, destination, handler, fluid)) {
			return false;
		}
		SoundEvent sound = vaporizes
				? fluid.getFluid().getFluidType().getSound(player, level, destination, SoundActions.FLUID_VAPORIZE)
				: fluid.getFluid().getFluidType().getSound(fluid, SoundActions.BUCKET_EMPTY);
		if (vaporizes && sound == null) {
			sound = SoundEvents.FIRE_EXTINGUISH;
		}
		if (sound != null) {
			player.playNotifySound(sound, SoundSource.BLOCKS, vaporizes ? 0.5F : 1, vaporizes ? 2.6F : 1);
		}
		return true;
	}
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);
		if (level instanceof ServerLevel serverLevel) {
			TankItemLinkedStorageEndpointAdapter.synchronizePrimaryCarrier(serverLevel, stack);
		}
	}
	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		if (getMode(stack) != Mode.PLACE_TANK) {
			return context.getPlayer() == null ? InteractionResult.FAIL : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
		}
		BlockPlaceContext placement = new BlockPlaceContext(context);
		TankBlockEntity extension = getExtensionPrimary(placement);
		TankBlockEntity above = getUpperPrimary(placement);
		boolean creativePayload = TankItemContents.hasPortableData(stack) && context.getPlayer() != null && context.getPlayer().isCreative();
		if (!creativePayload && extension == null && above == null) {
			return InteractionResult.PASS;
		}
		List<TankBlockEntity> primaries = new ArrayList<>();
		if (extension != null) {
			primaries.add(extension);
		}
		if (above != null) {
			primaries.add(above);
		}
		List<BlockSnapshot> snapshots = captureGroupSnapshots(context.getLevel(), primaries);
		InteractionResult result = stack.useOn(context);
		if (!result.consumesAction() && !context.getLevel().isClientSide) {
			restoreCancelledGroupPlacement(context.getLevel(), snapshots, primaries);
		}
		if (result.consumesAction() && creativePayload) {
			stack.shrink(1);
		}
		return result;
	}
	private List<BlockSnapshot> captureGroupSnapshots(Level level, List<TankBlockEntity> primaries) {
		List<BlockSnapshot> snapshots = new ArrayList<>();
		if (!level.isClientSide) {
			for (TankBlockEntity primary : primaries) {
				for (int index = 0; index < primary.getHeight(); index++) {
					snapshots.add(BlockSnapshot.create(level.dimension(), level, primary.getBlockPos().above(index)));
				}
			}
		}
		return snapshots;
	}
	private void restoreCancelledGroupPlacement(Level level, List<BlockSnapshot> snapshots, List<TankBlockEntity> primaries) {
		for (BlockSnapshot snapshot : snapshots) {
			snapshot.restoreToLocation(level, snapshot.getPos(), true, false);
		}
		for (TankBlockEntity primary : primaries) {
			WorldHelper.getBlockEntity(level, primary.getBlockPos(), TankBlockEntity.class)
					.ifPresent(restored -> restored.configureGroup(restored.getHeight()));
		}
	}
	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		int height = TankItemContents.height(context.getItemInHand());
		TankTier.validateHeight(height);
		TankBlockEntity below = getExtensionPrimary(context);
		if (!canPlaceGroup(context, state, height) || below != null && below.getHeight() + height > TankTier.MAX_HEIGHT) {
			return false;
		}
		for (int index = 0; index < height; index++) {
			level.setBlock(pos.above(index), state, 11);
		}
		if (!level.isClientSide) {
			TankBlockEntity primary = below == null ? WorldHelper.getBlockEntity(level, pos, TankBlockEntity.class).orElseThrow() : below;
			if (below == null) {
				primary.configureGroup(height);
				primary.restoreStack(context.getItemInHand());
			} else {
				primary.extendWith(context.getItemInHand());
			}
			if (!context.isSecondaryUseActive()) {
				primary.joinAbove();
			}
		}
		return true;
	}
	private boolean canPlaceGroup(BlockPlaceContext context, BlockState state, int height) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		for (int index = 0; index < height; index++) {
			BlockPos target = pos.above(index);
			if (!level.isInWorldBounds(target) || !level.getBlockState(target).canBeReplaced(context) || !level.isUnobstructed(state, target,
					context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer()))) {
				return false;
			}
		}
		return true;
	}
	@Nullable
	private TankBlockEntity getExtensionPrimary(BlockPlaceContext context) {
		if (context.isSecondaryUseActive() || context.getClickedFace() != Direction.UP) {
			return null;
		}
		return WorldHelper.getBlockEntity(context.getLevel(), context.getClickedPos().below(), TankBlockEntity.class)
				.filter(segment -> segment.getBlockState().is(getBlock()) && !segment.isPacked()).flatMap(TankBlockEntity::getPrimary)
				.filter(primary -> primary.getBlockPos().above(primary.getHeight()).equals(context.getClickedPos())
						&& primary.canExtendWith(context.getItemInHand()))
				.orElse(null);
	}
	@Nullable
	private TankBlockEntity getUpperPrimary(BlockPlaceContext context) {
		if (context.isSecondaryUseActive()) {
			return null;
		}
		BlockPos top = context.getClickedPos().above(TankItemContents.height(context.getItemInHand()));
		return WorldHelper.getBlockEntity(context.getLevel(), top, TankBlockEntity.class).filter(TankBlockEntity::isPrimary)
				.filter(tank -> tank.getBlockState().is(getBlock()) && !tank.isPacked()).orElse(null);
	}
}
