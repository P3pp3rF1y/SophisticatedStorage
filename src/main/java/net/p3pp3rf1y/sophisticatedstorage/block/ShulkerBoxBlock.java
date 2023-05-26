package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticleData;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class ShulkerBoxBlock extends StorageBlockBase implements IAdditionalDropDataBlock {
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
	private static final VoxelShape ITEM_ENTITY_COLLISION_SHAPE = box(0.05, 0.05, 0.05, 15.95, 15.95, 15.95);

	public ShulkerBoxBlock(Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier) {
		super(getProperties(), numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
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
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return createTickerHelper(blockEntityType, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), (l, pos, s, blockEntity) -> {
			ShulkerBoxBlockEntity.tick(l, pos, s, blockEntity);
			if (!l.isClientSide) {
				StorageBlockEntity.serverTick(l, pos, blockEntity);
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

		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(b -> {
			if (tryItemInteraction(player, hand, b, player.getItemInHand(hand), getFacing(state), hitResult)) {
				return InteractionResult.SUCCESS;
			}

			player.awardStat(Stats.CUSTOM.get(Stats.OPEN_SHULKER_BOX));
			NetworkHooks.openGui((ServerPlayer) player, new SimpleMenuProvider((w, p, pl) -> new StorageContainerMenu(w, pl, pos),
					WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(TextComponent.EMPTY)), pos);
			PiglinAi.angerNearbyPiglins(player, true);
			return InteractionResult.CONSUME;
		}).orElse(InteractionResult.PASS);
	}

	private boolean tryItemInteraction(Player player, InteractionHand hand, StorageBlockEntity b, ItemStack itemInHand, Direction facing, BlockHitResult hitResult) {
		return tryAddUpgrade(player, hand, b, itemInHand, facing, hitResult);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> {
			NBTHelper.getUniqueId(stack, "uuid").ifPresent(uuid -> {
				ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
				be.load(itemContentsStorage.getOrCreateStorageContents(uuid));
				itemContentsStorage.removeStorageContents(uuid);
			});

			if (stack.hasCustomHoverName()) {
				be.setCustomName(stack.getHoverName());
			}
			if (stack.getItem() instanceof ShulkerBoxItem shulkerBoxItem) {
				StorageWrapper storageWrapper = be.getStorageWrapper();
				shulkerBoxItem.getMainColor(stack).ifPresent(storageWrapper::setMainColor);
				shulkerBoxItem.getAccentColor(stack).ifPresent(storageWrapper::setAccentColor);
				InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
				UpgradeHandler upgradeHandler = storageWrapper.getUpgradeHandler();
				storageWrapper.increaseSize(shulkerBoxItem.getNumberOfInventorySlots(stack) - inventoryHandler.getSlots(),
						shulkerBoxItem.getNumberOfUpgradeSlots(stack) - upgradeHandler.getSlots());
			}

			be.tryToAddToController();

			if (placer != null && placer.getOffhandItem().getItem() == ModItems.STORAGE_TOOL.get()) {
				StorageToolItem.useOffHandOnPlaced(placer.getOffhandItem(), be);
			}

			be.setChanged();
		});
	}

	@Override
	public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
		items.add(new ItemStack(this));

		if (this == ModBlocks.SHULKER_BOX.get() || Boolean.TRUE.equals(Config.CLIENT.showHigherTierTintedVariants.get())) {
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
	}

	public ItemStack getTintedStack(DyeColor color) {
		ItemStack storageStack = new ItemStack(this);
		if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			tintableBlockItem.setMainColor(storageStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
			tintableBlockItem.setAccentColor(storageStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
		}
		return storageStack;
	}

	private static boolean canOpen(BlockState state, Level level, BlockPos pos, ShulkerBoxBlockEntity blockEntity) {
		if (blockEntity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
			return true;
		} else {
			AABB aabb = Shulker.getProgressDeltaAabb(state.getValue(FACING), 0.0F, 0.5F).move(pos).deflate(1.0E-6D);
			return level.noCollision(aabb);
		}
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockEntity blockentity = level.getBlockEntity(pos);
		if (blockentity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity && !level.isClientSide && player.isCreative()) {
			ItemStack shulkerBoxDrop = new ItemStack(this);
			addShulkerContentsToStack(shulkerBoxDrop, shulkerBoxBlockEntity);

			ItemEntity itementity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, shulkerBoxDrop);
			itementity.setDefaultPickUpDelay();
			level.addFreshEntity(itementity);
		}

		super.playerWillDestroy(level, pos, state, player);
	}

	@SuppressWarnings("deprecation")
	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}

	@Override
	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		addShulkerContentsToStack(stack, be);
	}

	private void addShulkerContentsToStack(ItemStack stack, StorageBlockEntity be) {
		StorageWrapper storageWrapper = be.getStorageWrapper();
		UUID shulkerBoxUuid = storageWrapper.getContentsUuid().orElse(UUID.randomUUID());
		CompoundTag shulkerContents = be.saveWithoutMetadata();
		shulkerContents.remove(IControllerBoundable.CONTROLLER_POS_TAG);
		if (!shulkerContents.isEmpty()) {
			ItemContentsStorage.get().setStorageContents(shulkerBoxUuid, shulkerContents);
			NBTHelper.setUniqueId(stack, "uuid", shulkerBoxUuid);
		}
		addBasicPropertiesToStack(stack, be, storageWrapper);
		StorageBlockItem.setShowsTier(stack, be.shouldShowTier());
	}

	private void addBasicPropertiesToStack(ItemStack stack, StorageBlockEntity be, StorageWrapper storageWrapper) {
		if (be.hasCustomName()) {
			stack.setHoverName(be.getCustomName());
		}
		if (stack.getItem() instanceof ShulkerBoxItem shulkerBoxItem) {
			int mainColor = storageWrapper.getMainColor();
			if (mainColor > -1) {
				shulkerBoxItem.setMainColor(stack, mainColor);
			}
			int accentColor = storageWrapper.getAccentColor();
			if (accentColor > -1) {
				shulkerBoxItem.setAccentColor(stack, accentColor);
			}
			shulkerBoxItem.setNumberOfInventorySlots(stack, storageWrapper.getInventoryHandler().getSlots());
			shulkerBoxItem.setNumberOfUpgradeSlots(stack, storageWrapper.getUpgradeHandler().getSlots());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockEntity blockentity = level.getBlockEntity(pos);
		return blockentity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity ? Shapes.create(shulkerBoxBlockEntity.getBoundingBox(state)) : Shapes.block();
	}

	@SuppressWarnings("deprecation")
	@Override
	public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
		ItemStack stack = super.getCloneItemStack(level, pos, state);
		WorldHelper.getBlockEntity(level, pos, ShulkerBoxBlockEntity.class).ifPresent(be -> {
			StorageWrapper storageWrapper = be.getStorageWrapper();
			addBasicPropertiesToStack(stack, be, storageWrapper);
		});
		return stack;
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public void setTicking(Level level, BlockPos pos, BlockState currentState, boolean ticking) {
		//noop as shulker box is always ticking due to calculation of animation and related bounding box size on server
	}

	@SuppressWarnings("deprecation")
	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
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
	public Direction getFacing(BlockState state) {
		return state.getValue(FACING);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return context instanceof EntityCollisionContext entityCollisionContext && entityCollisionContext.getEntity() instanceof ItemEntity ? ITEM_ENTITY_COLLISION_SHAPE : super.getCollisionShape(state, level, pos, context);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return false;
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
		level.sendParticles(new CustomTintTerrainParticleData(state1, pos), entity.getX(), entity.getY(), entity.getZ(), numberOfParticles, 0.0D, 0.0D, 0.0D, 0.15D);
		return true;
	}

	@Override
	public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity) {
		Vec3 vec3 = entity.getDeltaMovement();
		level.addParticle(new CustomTintTerrainParticleData(state, pos),
				entity.getX() + (level.random.nextDouble() - 0.5D) * entity.getBbWidth(), entity.getY() + 0.1D, entity.getZ() + (level.random.nextDouble() - 0.5D) * entity.getBbWidth(),
				vec3.x * -4.0D, 1.5D, vec3.z * -4.0D);
		return true;
	}
}
