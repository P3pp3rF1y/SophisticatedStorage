package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import javax.annotation.Nullable;
import java.util.UUID;

public class ShulkerBoxBlock extends StorageBlockBase implements IAdditionalDropDataBlock {
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;

	public ShulkerBoxBlock(int numberOfInventorySlots, int numberOfUpgradeSlots) {
		super(getProperties(), numberOfInventorySlots, numberOfUpgradeSlots);
	}

	private static Properties getProperties() {
		BlockBehaviour.StatePredicate statePredicate = (state, blockGetter, pos) -> {
			BlockEntity blockentity = blockGetter.getBlockEntity(pos);
			if (!(blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity)) {
				return true;
			} else {
				return shulkerboxblockentity.isClosed();
			}
		};
		return BlockBehaviour.Properties.of(Material.SHULKER_SHELL).strength(2.0F).dynamicShape().noOcclusion().isSuffocating(statePredicate).isViewBlocking(statePredicate);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
		return createTickerHelper(pBlockEntityType, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), (level, pos, state, blockEntity) -> {
			ShulkerBoxBlockEntity.tick(level, pos, state, blockEntity);
			if (!level.isClientSide) {
				StorageBlockEntity.serverTick(level, pos, blockEntity);
			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		} else if (player.isSpectator()) {
			return InteractionResult.CONSUME;
		} else if (!(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) || !canOpen(state, level, pos, shulkerBoxBlockEntity)) {
			return InteractionResult.PASS;
		}

		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(b -> {
			player.awardStat(Stats.CUSTOM.get(Stats.OPEN_SHULKER_BOX));
			NetworkHooks.openGui((ServerPlayer) player, new SimpleMenuProvider((w, p, pl) -> new StorageContainerMenu(w, pl, pos),
					WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(TextComponent.EMPTY)), pos);
			PiglinAi.angerNearbyPiglins(player, true);
		});

		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> {
			NBTHelper.getUniqueId(stack, "uuid").ifPresent(uuid -> {
				ShulkerBoxStorage shulkerBoxStorage = ShulkerBoxStorage.get();
				be.load(shulkerBoxStorage.getOrCreateShulkerBoxContents(uuid));
				shulkerBoxStorage.removeShulkerBoxContents(uuid);
			});

			if (stack.hasCustomHoverName()) {
				be.setCustomName(stack.getHoverName());
			}
			StorageBlockItem.getMaincolorFromStack(stack).ifPresent(be.getStorageWrapper()::setMainColor);
			StorageBlockItem.getAccentColorFromStack(stack).ifPresent(be.getStorageWrapper()::setAccentColor);
		});
	}

	@Override
	public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
		items.add(new ItemStack(this));

		for (DyeColor color : DyeColor.values()) {
			ItemStack storageStack = getTintedStack(color);
			items.add(storageStack);
		}
		ItemStack storageStack = new ItemStack(this);
		if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			tintableBlockItem.setMainColor(storageStack, ColorHelper.getColor(DyeColor.YELLOW.getTextureDiffuseColors()));
			tintableBlockItem.setAccentColor(storageStack, ColorHelper.getColor(DyeColor.LIME.getTextureDiffuseColors()));
		}
		items.add(storageStack);
	}

	public ItemStack getTintedStack(DyeColor color) {
		ItemStack storageStack = new ItemStack(this);
		if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			tintableBlockItem.setMainColor(storageStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
			tintableBlockItem.setAccentColor(storageStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
		}
		return storageStack;
	}

	private static boolean canOpen(BlockState pState, Level pLevel, BlockPos pPos, ShulkerBoxBlockEntity pBlockEntity) {
		if (pBlockEntity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
			return true;
		} else {
			AABB aabb = Shulker.getProgressDeltaAabb(pState.getValue(FACING), 0.0F, 0.5F).move(pPos).deflate(1.0E-6D);
			return pLevel.noCollision(aabb);
		}
	}

	@Override
	public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
		BlockEntity blockentity = pLevel.getBlockEntity(pPos);
		if (blockentity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity && !pLevel.isClientSide && pPlayer.isCreative()) {
			ItemStack shulkerBoxDrop = new ItemStack(this);
			addShulkerContentsToStack(shulkerBoxDrop, shulkerBoxBlockEntity);

			ItemEntity itementity = new ItemEntity(pLevel, pPos.getX() + 0.5D, pPos.getY() + 0.5D, pPos.getZ() + 0.5D, shulkerBoxDrop);
			itementity.setDefaultPickUpDelay();
			pLevel.addFreshEntity(itementity);
		}

		super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
	}

	@SuppressWarnings("deprecation")
	@Override
	public PushReaction getPistonPushReaction(BlockState pState) {
		return PushReaction.DESTROY;
	}

	@Override
	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		addShulkerContentsToStack(stack, be);
	}

	private void addShulkerContentsToStack(ItemStack stack, StorageBlockEntity be) {
		UUID shulkerBoxUuid = be.getStorageWrapper().getContentsUuid().orElse(UUID.randomUUID());
		CompoundTag shulkerContents = be.saveWithoutMetadata();
		if (!shulkerContents.isEmpty()) {
			ShulkerBoxStorage.get().setShulkerBoxContents(shulkerBoxUuid, shulkerContents);
			NBTHelper.setUniqueId(stack, "uuid", shulkerBoxUuid);
		}
		be.getCustomName().ifPresent(stack::setHoverName);
		if (stack.getItem() instanceof StorageBlockItem storageBlockItem) {
			int mainColor = be.getStorageWrapper().getMainColor();
			if (mainColor > -1) {
				storageBlockItem.setMainColor(stack, mainColor);
			}
			int accentColor = be.getStorageWrapper().getAccentColor();
			if (accentColor > -1) {
				storageBlockItem.setAccentColor(stack, accentColor);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		BlockEntity blockentity = pLevel.getBlockEntity(pPos);
		return blockentity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity ? Shapes.create(shulkerBoxBlockEntity.getBoundingBox(pState)) : Shapes.block();
	}

	@SuppressWarnings("deprecation")
	@Override
	public ItemStack getCloneItemStack(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
		ItemStack stack = super.getCloneItemStack(pLevel, pPos, pState);
		WorldHelper.getBlockEntity(pLevel, pPos, ShulkerBoxBlockEntity.class).ifPresent(be -> addShulkerContentsToStack(stack, be));
		return stack;
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState pState, Mirror pMirror) {
		return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
	}

	@Override
	public void setTicking(Level level, BlockPos pos, BlockState currentState, boolean ticking) {
		//noop as shulker box is always ticking due to calculation of animation and related bounding box size on server
	}

	@SuppressWarnings("deprecation")
	@Override
	public RenderShape getRenderShape(BlockState pState) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		return defaultBlockState().setValue(FACING, pContext.getClickedFace());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(FACING);
	}

	@Override
	public StorageBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShulkerBoxBlockEntity(pos, state);
	}

	@Override
	protected BlockEntityType<? extends StorageBlockEntity> getBlockEntityType() {
		return ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get();
	}

	@Override
	protected Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}

	@Override
	protected boolean shouldDropContents() {
		return false;
	}
}
