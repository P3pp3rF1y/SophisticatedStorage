package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.FluidTooltipHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class TankBlockItem extends BlockItem {
	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		return FMLEnvironment.getDist().isClient() ? Optional.ofNullable(StorageItemClient.getLinkedStorageTooltipImage(stack)) : Optional.empty();
	}
	private static final String TRANSLATION_NAME = "tank";
	private static final String MODE_TRANSLATION_KEY = "mode";
	public TankBlockItem(TankBlock block, Properties properties) {
		super(block, properties.stacksTo(64));
	}
	public static void setNumberOfUpgradeSlots(ItemStack stack, int slots) {
		stack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, slots);
	}
	public static Mode getMode(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.TANK_ITEM_MODE, Mode.PLACE_TANK);
	}
	public static void cycleMode(ItemStack stack, boolean next) {
		Mode[] modes = Mode.values();
		stack.set(ModDataComponents.TANK_ITEM_MODE, modes[Math.floorMod(getMode(stack).ordinal() + (next ? 1 : -1), modes.length)]);
	}
	public static Component getModeMessage(ItemStack stack) {
		return StorageTranslationHelper.INSTANCE
				.translItem(TRANSLATION_NAME, MODE_TRANSLATION_KEY, getMode(stack).getDisplayName().copy().withStyle(ChatFormatting.WHITE))
				.withStyle(ChatFormatting.GOLD);
	}
	@Override
	public Component getHighlightTip(ItemStack stack, Component displayName) {
		return displayName.copy().append(" - ").append(getModeMessage(stack));
	}
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(StorageTranslationHelper.INSTANCE
				.translItem(TRANSLATION_NAME, MODE_TRANSLATION_KEY, getMode(stack).getDisplayName().copy().withStyle(ChatFormatting.BLUE))
				.withStyle(ChatFormatting.GOLD));
		FluidStack contents = FluidStack.EMPTY;
		int capacity = ((TankBlock) getBlock()).getTier().capacity(stack.getOrDefault(ModDataComponents.TANK_HEIGHT, 1), 1);
		if (RegistryHelper.getRegistryAccess().isPresent()) {
			TankItemStorageWrapper wrapper = new TankItemStorageWrapper(stack);
			contents = wrapper.getTankFluidHandler().getFluidInTank(0);
			capacity = wrapper.getCapacity();
		}
		tooltip.accept(contents.isEmpty()
				? StorageTranslationHelper.INSTANCE.translItemTooltip(TRANSLATION_NAME, "capacity", FluidTooltipHelper.formatAmount(contents, capacity))
						.withStyle(ChatFormatting.GRAY)
				: FluidTooltipHelper.getFluidTooltip(contents, capacity));
		tooltip.accept(StorageTranslationHelper.INSTANCE
				.translItemTooltip("storage_tool", "controls",
						StorageTranslationHelper.INSTANCE.translItemTooltip("storage_tool", "controls.combination").withStyle(ChatFormatting.AQUA))
				.withStyle(ChatFormatting.GRAY));
	}
	@Override
	public InteractionResult useOn(UseOnContext context) {
		return getMode(context.getItemInHand()) == Mode.PLACE_TANK ? super.useOn(context) : InteractionResult.FAIL;
	}
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Mode mode = getMode(stack);
		if (mode == Mode.PLACE_TANK) {
			return super.use(level, player, hand);
		}
		BlockHitResult hit = getPlayerPOVHitResult(level, player, mode == Mode.COLLECT_FLUID ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
		BlockPos pos = hit.getBlockPos();
		if (hit.getType() != HitResult.Type.BLOCK || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)
				|| !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), stack)) {
			return InteractionResult.FAIL;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack single = stack.copyWithCount(1);
		ResourceHandler<FluidResource> handler = TankItemStorageWrapper.getCapability(single);
		if (handler == null || !transferFluid(level, player, hand, hit, handler, mode)) {
			return InteractionResult.FAIL;
		}
		if (stack.getCount() == 1) {
			player.setItemInHand(hand, single);
		} else {
			stack.shrink(1);
			if (!player.getInventory().add(single) && !single.isEmpty()) {
				player.drop(single, false);
			}
		}
		return InteractionResult.SUCCESS;
	}
	private boolean transferFluid(Level level, Player player, InteractionHand hand, BlockHitResult hit, ResourceHandler<FluidResource> handler, Mode mode) {
		BlockPos pos = hit.getBlockPos();
		ResourceHandler<FluidResource> target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, pos, hit.getDirection());
		if (mode == Mode.COLLECT_FLUID) {
			FluidResource contents = handler.getResource(0);
			if (target != null) {
				ResourceStack<FluidResource> moved = ResourceHandlerUtil.moveFirst(target, handler, resource -> contents.isEmpty() || contents.equals(resource),
						FluidType.BUCKET_VOLUME, null);
				if (moved != null) {
					FluidUtil.triggerSoundAndGameEvent(moved.resource(), level, net.minecraft.world.phys.Vec3.atCenterOf(pos), player, true);
				}
				return moved != null && moved.amount() > 0;
			}
			return !FluidUtil.tryPickupFluid(handler, player, level, pos, hit.getDirection()).isEmpty();
		}
		if (target != null) {
			ResourceStack<FluidResource> moved = ResourceHandlerUtil.moveFirst(handler, target, resource -> true, FluidType.BUCKET_VOLUME, null);
			if (moved != null) {
				FluidUtil.triggerSoundAndGameEvent(moved.resource(), level, net.minecraft.world.phys.Vec3.atCenterOf(pos), player, false);
			}
			return moved != null && moved.amount() > 0;
		}
		FluidResource fluid = handler.getResource(0);
		if (fluid.isEmpty() || handler.getAmountAsLong(0) < FluidType.BUCKET_VOLUME) {
			return false;
		}
		BlockState state = level.getBlockState(pos);
		BlockPos destination = state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlockContainer container
				&& container.canPlaceLiquid(player, level, pos, state, fluid.getFluid()) ? pos : pos.relative(hit.getDirection());
		return level.mayInteract(player, destination)
				&& FluidUtil.tryPlaceFluid(handler, player, level, hand, destination).getAmount() == FluidType.BUCKET_VOLUME;
	}
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		super.inventoryTick(stack, level, entity, slot);
		TankItemLinkedStorageEndpointAdapter.synchronizePrimaryCarrier(level, stack);
	}
	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		if (getMode(stack) != Mode.PLACE_TANK) {
			return context.getPlayer() == null ? InteractionResult.FAIL : use(context.getLevel(), context.getPlayer(), context.getHand());
		}
		boolean portable = TankItemContents.hasPortableData(stack);
		BlockPlaceContext placement = new BlockPlaceContext(context);
		TankBlockEntity extension = getExtensionPrimary(placement);
		TankBlockEntity above = getUpperPrimary(placement);
		boolean moveCreativePayload = portable && context.getPlayer() != null && context.getPlayer().isCreative();
		if (!moveCreativePayload && extension == null && above == null) {
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
		if (!result.consumesAction() && !context.getLevel().isClientSide()) {
			restoreCancelledGroupPlacement(context.getLevel(), snapshots, primaries);
		}
		if (result.consumesAction() && moveCreativePayload) {
			stack.shrink(1);
		}
		return result;
	}
	private List<BlockSnapshot> captureGroupSnapshots(Level level, List<TankBlockEntity> primaries) {
		List<BlockSnapshot> snapshots = new ArrayList<>();
		if (!level.isClientSide()) {
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
			snapshot.restoreBlockEntity(level, snapshot.getPos());
		}
		for (TankBlockEntity primary : primaries) {
			primary.configureGroup(primary.getHeight());
		}
	}
	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		int height = context.getItemInHand().getOrDefault(ModDataComponents.TANK_HEIGHT, 1);
		TankTier.validateHeight(height);
		TankBlockEntity below = getExtensionPrimary(context);
		if (!canPlaceGroup(context, state, height) || below != null && below.getHeight() + height > TankTier.MAX_HEIGHT) {
			return false;
		}
		for (int index = 0; index < height; index++) {
			level.setBlock(pos.above(index), state, 11);
		}
		if (!level.isClientSide()) {
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
	private @Nullable TankBlockEntity getExtensionPrimary(BlockPlaceContext context) {
		if (context.isSecondaryUseActive() || context.getClickedFace() != Direction.UP) {
			return null;
		}
		return WorldHelper.getBlockEntity(context.getLevel(), context.getClickedPos().below(), TankBlockEntity.class)
				.filter(segment -> segment.getBlockState().is(getBlock()) && !segment.isPacked()).flatMap(TankBlockEntity::getPrimary)
				.filter(primary -> primary.getBlockPos().above(primary.getHeight()).equals(context.getClickedPos())
						&& primary.canExtendWith(context.getItemInHand()))
				.orElse(null);
	}
	private @Nullable TankBlockEntity getUpperPrimary(BlockPlaceContext context) {
		if (context.isSecondaryUseActive()) {
			return null;
		}
		BlockPos top = context.getClickedPos().above(context.getItemInHand().getOrDefault(ModDataComponents.TANK_HEIGHT, 1));
		return WorldHelper.getBlockEntity(context.getLevel(), top, TankBlockEntity.class).filter(TankBlockEntity::isPrimary)
				.filter(tank -> tank.getBlockState().is(getBlock()) && !tank.isPacked()).orElse(null);
	}
	public enum Mode implements StringRepresentable {
		PLACE_TANK("place_tank"), COLLECT_FLUID("collect_fluid"), DISPENSE_FLUID("dispense_fluid");
		public static final StringRepresentable.EnumCodec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
		public static final StreamCodec<FriendlyByteBuf, Mode> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Mode.class);
		private final String name;
		Mode(String name) {
			this.name = name;
		}
		public Component getDisplayName() {
			return StorageTranslationHelper.INSTANCE.translItem(TRANSLATION_NAME, "mode." + name);
		}
		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
