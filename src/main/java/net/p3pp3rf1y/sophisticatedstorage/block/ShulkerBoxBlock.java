package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.p3pp3rf1y.sophisticatedcore.controller.IControllerBoundable;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
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
import java.util.function.Consumer;

public class ShulkerBoxBlock extends StorageBlockBase implements IAdditionalDropDataBlock {
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
	private static final VoxelShape ITEM_ENTITY_COLLISION_SHAPE = box(0.05, 0.05, 0.05, 15.95, 15.95, 15.95);

	public ShulkerBoxBlock(Config.Server.StorageConfig config, Properties properties) {
		this(config, 2.0F, properties);
	}

	public ShulkerBoxBlock(Config.Server.StorageConfig config, float explosionResistance, Properties properties) {
		super(setProperties(explosionResistance, properties), config::numberOfInventorySlots, config::numberOfUpgradeSlots);
	}

	private static Properties setProperties(float explosionResistance, Properties properties) {
		StatePredicate statePredicate = (state, blockGetter, pos) -> {
			BlockEntity blockentity = blockGetter.getBlockEntity(pos);
			if (!(blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity)) {
				return true;
			} else {
				return shulkerboxblockentity.isClosed();
			}
		};
		return properties.strength(2.0F, explosionResistance).forceSolidOn().dynamicShape().noOcclusion().isSuffocating(statePredicate).isViewBlocking(statePredicate).pushReaction(PushReaction.DESTROY).mapColor(DyeColor.PURPLE);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return createTickerHelper(blockEntityType, ModBlocks.SHULKER_BOX_BLOCK_ENTITY_TYPE.get(), (l, pos, s, blockEntity) -> {
			ShulkerBoxBlockEntity.tick(l, pos, s, blockEntity);
			if (!l.isClientSide()) {
				StorageBlockEntity.serverTick(l, pos, blockEntity);
			}
		});
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		} else if (player.isSpectator()) {
			return InteractionResult.CONSUME;
		} else if (!(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) || !canOpen(state, level, pos, shulkerBoxBlockEntity)) {
			return InteractionResult.FAIL;
		}

		return WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(b -> {
			InteractionResult result = tryItemInteraction(player, b, player.getItemInHand(hand), getFacing(state), hitResult);
			if (result.consumesAction()) {
				return result;
			}
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}).orElse(InteractionResult.TRY_WITH_EMPTY_HAND);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		} else if (player.isSpectator()) {
			return InteractionResult.CONSUME;
		} else if (!(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) || !canOpen(state, level, pos, shulkerBoxBlockEntity)) {
			return InteractionResult.PASS;
		}

		player.awardStat(Stats.CUSTOM.get(Stats.OPEN_SHULKER_BOX));
		player.openMenu(new SimpleMenuProvider((w, p, pl) -> new StorageContainerMenu(w, pl, pos),
				WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).map(StorageBlockEntity::getDisplayName).orElse(Component.empty())), pos);
		if (player.level() instanceof ServerLevel serverLevel) {
			PiglinAi.angerNearbyPiglins(serverLevel, player, true);
		}
		return InteractionResult.CONSUME;
	}

	private InteractionResult tryItemInteraction(Player player, StorageBlockEntity b, ItemStack itemInHand, Direction facing, BlockHitResult hitResult) {
		return tryAddUpgrade(player, b, itemInHand, facing, hitResult);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> {
			UUID storageUuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
			if (storageUuid != null) {
				ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
				CompoundTag beTag = itemContentsStorage.getOrCreateAddtionalBeData(storageUuid);
				CompoundTag storageWrapperTag = beTag.getCompound(StorageBlockEntity.STORAGE_WRAPPER).orElseGet(() -> {
					CompoundTag newStorageWrapperTag = new CompoundTag();
					beTag.put(StorageBlockEntity.STORAGE_WRAPPER, newStorageWrapperTag);
					return newStorageWrapperTag;
				});
				ContainerContents contents = itemContentsStorage.getOrCreateContents(storageUuid);
				Tag contentsTag = ContainerContents.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, be.getLevel().registryAccess()), contents).getOrThrow();
				storageWrapperTag.put(StorageWrapper.CONTENTS, contentsTag);
				be.loadAdditional(ValueIOHelper.inputFromCompoundTag(level.registryAccess(), beTag));
				itemContentsStorage.removeContents(storageUuid);
				itemContentsStorage.removeAddtionalBeData(storageUuid);
			}

			if (stack.has(DataComponents.CUSTOM_NAME)) {
				be.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
			}
			if (stack.getItem() instanceof ShulkerBoxItem shulkerBoxItem) {
				StorageWrapper storageWrapper = be.getStorageWrapper();
				storageWrapper.setColors(shulkerBoxItem.getMainColor(stack).orElse(-1), shulkerBoxItem.getAccentColor(stack).orElse(-1));
				InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
				UpgradeHandler upgradeHandler = storageWrapper.getUpgradeHandler();
				storageWrapper.changeSize(shulkerBoxItem.getNumberOfInventorySlots(level.registryAccess(), stack) - inventoryHandler.size(),
						shulkerBoxItem.getNumberOfUpgradeSlots(level.registryAccess(), stack) - upgradeHandler.size());
			}

			be.getStorageWrapper().onInit(level);
			be.tryToAddToController();

			if (placer != null && placer.getOffhandItem().getItem() == ModItems.STORAGE_TOOL.get()) {
				StorageToolItem.useOffHandOnPlaced(placer.getOffhandItem(), be);
			}

			be.setChanged();
		});
	}

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		itemConsumer.accept(new ItemStack(this));

		if (this == ModBlocks.SHULKER_BOX.get() || !Config.CLIENT_SPEC.isLoaded() || Config.CLIENT.showHigherTierTintedVariants.get()) {
			for (DyeColor color : DyeColor.values()) {
				ItemStack storageStack = getTintedStack(color);
				itemConsumer.accept(storageStack);
			}
			ItemStack storageStack = new ItemStack(this);
			if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				tintableBlockItem.setMainColor(storageStack, DyeColor.YELLOW.getTextureDiffuseColor());
				tintableBlockItem.setAccentColor(storageStack, DyeColor.LIME.getTextureDiffuseColor());
			}
			itemConsumer.accept(storageStack);
		}
	}

	public ItemStack getTintedStack(DyeColor color) {
		ItemStack storageStack = new ItemStack(this);
		if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			tintableBlockItem.setMainColor(storageStack, color.getTextureDiffuseColor());
			tintableBlockItem.setAccentColor(storageStack, color.getTextureDiffuseColor());
		}
		return storageStack;
	}

	private static boolean canOpen(BlockState state, Level level, BlockPos pos, ShulkerBoxBlockEntity blockEntity) {
		if (blockEntity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
			return true;
		} else {
			AABB aabb = Shulker.getProgressDeltaAabb(1.0F, state.getValue(FACING), 0.0F, 0.5F, pos.getBottomCenter()).deflate(1.0E-6D);
			return level.noCollision(aabb);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockEntity blockentity = level.getBlockEntity(pos);
		if (blockentity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity && !level.isClientSide() && player.isCreative() && hasAnyItems(shulkerBoxBlockEntity)) {
			ItemStack shulkerBoxDrop = new ItemStack(this);
			addShulkerContentsToStack(shulkerBoxDrop, shulkerBoxBlockEntity);

			ItemEntity itementity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, shulkerBoxDrop);
			itementity.setDefaultPickUpDelay();
			level.addFreshEntity(itementity);
		}

		return super.playerWillDestroy(level, pos, state, player);
	}

	private boolean hasAnyItems(ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
		if (!ResourceHandlerUtil.isEmpty(shulkerBoxBlockEntity.getStorageWrapper().getInventoryHandler())) return true;
		return !ResourceHandlerUtil.isEmpty(shulkerBoxBlockEntity.getStorageWrapper().getUpgradeHandler());
	}

	@Override
	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		addShulkerContentsToStack(stack, be);
	}

	private void addShulkerContentsToStack(ItemStack stack, StorageBlockEntity be) {
		StorageWrapper storageWrapper = be.getStorageWrapper();
		UUID shulkerBoxUuid = storageWrapper.getContentsUuid().orElse(UUID.randomUUID());
		CompoundTag additionalBeData = be.saveWithoutMetadata(be.getLevel().registryAccess());
		ContainerContents contents = be.getStorageWrapper().getContents().copy();
		additionalBeData.getCompound(StorageBlockEntity.STORAGE_WRAPPER).ifPresent(tag -> tag.remove(StorageWrapper.CONTENTS));
		additionalBeData.remove(IControllerBoundable.CONTROLLER_POS);
		ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
		itemContentsStorage.setContents(shulkerBoxUuid, contents);
		if (!additionalBeData.isEmpty()) {
			itemContentsStorage.setAdditionalBeData(shulkerBoxUuid, additionalBeData);
			stack.set(ModCoreDataComponents.STORAGE_UUID, shulkerBoxUuid);
		}
		addBasicPropertiesToStack(stack, be, storageWrapper);
		StorageBlockItem.setShowsTier(stack, be.shouldShowTier());
	}

	private void addBasicPropertiesToStack(ItemStack stack, StorageBlockEntity be, StorageWrapper storageWrapper) {
		if (be.hasCustomName()) {
			stack.set(DataComponents.CUSTOM_NAME, be.getCustomName());
		}
		if (stack.getItem() instanceof ShulkerBoxItem shulkerBoxItem) {
			int mainColor = storageWrapper.getMainColor();
			if (mainColor != -1) {
				shulkerBoxItem.setMainColor(stack, mainColor);
			}
			int accentColor = storageWrapper.getAccentColor();
			if (accentColor != -1) {
				shulkerBoxItem.setAccentColor(stack, accentColor);
			}
			StorageBlockItem.setNumberOfInventorySlots(stack, storageWrapper.getInventoryHandler().size());
			StorageBlockItem.setNumberOfUpgradeSlots(stack, storageWrapper.getUpgradeHandler().size());
		}
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockEntity blockentity = level.getBlockEntity(pos);
		return blockentity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity ? Shapes.create(shulkerBoxBlockEntity.getBoundingBox(state)) : Shapes.block();
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
		WorldHelper.getBlockEntity(level, pos, ShulkerBoxBlockEntity.class).ifPresent(be -> {
			if (includeData) {
				addShulkerContentsToStack(stack, be);
			} else {
				StorageWrapper storageWrapper = be.getStorageWrapper();
				addBasicPropertiesToStack(stack, be, storageWrapper);
			}

		});
		return stack;
	}

	@Override
	public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public void setTicking(Level level, BlockPos pos, BlockState currentState, boolean ticking) {
		//noop as shulker box is always ticking due to calculation of animation and related bounding box size on server
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

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return context instanceof EntityCollisionContext entityCollisionContext && entityCollisionContext.getEntity() instanceof ItemEntity ? ITEM_ENTITY_COLLISION_SHAPE : super.getCollisionShape(state, level, pos, context);
	}

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
