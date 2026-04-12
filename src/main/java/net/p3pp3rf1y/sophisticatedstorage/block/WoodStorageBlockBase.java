package net.p3pp3rf1y.sophisticatedstorage.block;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.PackingTapeItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class WoodStorageBlockBase extends StorageBlockBase implements IAdditionalDropDataBlock {
	public static final Map<WoodType, BlockFamily> CUSTOM_TEXTURE_WOOD_TYPES = ImmutableMap.<WoodType, BlockFamily>builder()
			.put(WoodType.ACACIA, BlockFamilies.ACACIA_PLANKS)
			.put(WoodType.BIRCH, BlockFamilies.BIRCH_PLANKS)
			.put(WoodType.CRIMSON, BlockFamilies.CRIMSON_PLANKS)
			.put(WoodType.DARK_OAK, BlockFamilies.DARK_OAK_PLANKS)
			.put(WoodType.JUNGLE, BlockFamilies.JUNGLE_PLANKS)
			.put(WoodType.OAK, BlockFamilies.OAK_PLANKS)
			.put(WoodType.PALE_OAK, BlockFamilies.PALE_OAK_PLANKS)
			.put(WoodType.SPRUCE, BlockFamilies.SPRUCE_PLANKS)
			.put(WoodType.WARPED, BlockFamilies.WARPED_PLANKS)
			.put(WoodType.MANGROVE, BlockFamilies.MANGROVE_PLANKS)
			.put(WoodType.CHERRY, BlockFamilies.CHERRY_PLANKS)
			.put(WoodType.BAMBOO, BlockFamilies.BAMBOO_PLANKS)
			.build();

	protected WoodStorageBlockBase(Properties properties, Supplier<Integer> numberOfInventorySlotsSupplier, Supplier<Integer> numberOfUpgradeSlotsSupplier) {
		super(properties, numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
	}

	public void addNameWoodAndTintData(ItemStack stack, BlockGetter level, BlockPos pos) {
		WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).ifPresent(be -> addNameWoodAndTintData(stack, be));
	}

	@Override
	public void onBlockExploded(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion) {
		if (Boolean.TRUE.equals(Config.COMMON.dropPacked.get())) {
			WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).ifPresent(wbe -> {
				if (isNonEmpty(wbe)) {
					wbe.setPacked(true);
				}
			});
		}
		super.onBlockExploded(state, level, pos, explosion);
	}

	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		if (be instanceof WoodStorageBlockEntity wbe) {
			addNameWoodAndTintData(stack, wbe);
			boolean packed = wbe.isPacked() || shouldNonEmptyDropPacked(wbe);
			if (packed) {
				StorageWrapper storageWrapper = be.getStorageWrapper();
				UUID storageUuid = storageWrapper.getContentsUuid().orElse(UUID.randomUUID());
				CompoundTag storageContents = wbe.getStorageContentsTag();
				if (!storageContents.isEmpty()) {
					ItemContentsStorage.get().setStorageContents(storageUuid, storageContents);
					stack.set(ModCoreDataComponents.STORAGE_UUID, storageUuid);
				}
				WoodStorageBlockItem.setPacked(stack, true);
				StorageBlockItem.setShowsTier(stack, be.shouldShowTier());
				StorageBlockItem.setNumberOfInventorySlots(stack, storageWrapper.getInventoryHandler().getSlots());
				StorageBlockItem.setNumberOfUpgradeSlots(stack, storageWrapper.getUpgradeHandler().getSlots());
			}
		}
	}

	private static boolean shouldNonEmptyDropPacked(WoodStorageBlockEntity wbe) {
		if (Boolean.FALSE.equals(Config.COMMON.dropPacked.get())) {
			return false;
		}

		return isNonEmpty(wbe);
	}

	private static boolean isNonEmpty(WoodStorageBlockEntity wbe) {
		return !InventoryHelper.isEmpty(wbe.getStorageWrapper().getInventoryHandler()) || !InventoryHelper.isEmpty(wbe.getStorageWrapper().getUpgradeHandler());
	}

	private void addNameWoodAndTintData(ItemStack stack, WoodStorageBlockEntity wbe) {
		if (stack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			int mainColor = wbe.getStorageWrapper().getMainColor();
			if (mainColor != -1) {
				tintableBlockItem.setMainColor(stack, mainColor);
			}
			int accentColor = wbe.getStorageWrapper().getAccentColor();
			if (accentColor != -1) {
				tintableBlockItem.setAccentColor(stack, accentColor);
			}
		}
		if (wbe.hasCustomName()) {
			stack.set(DataComponents.CUSTOM_NAME, wbe.getCustomName());
		}
		wbe.getWoodType().ifPresent(n -> WoodStorageBlockItem.setWoodType(stack, n));
	}

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		if (!Config.CLIENT_SPEC.isLoaded() || Boolean.TRUE.equals(Config.CLIENT.showSingleWoodVariantOnly.get())) {
			itemConsumer.accept(WoodStorageBlockItem.setWoodType(new ItemStack(this), WoodType.ACACIA));
		} else {
			CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> itemConsumer.accept(WoodStorageBlockItem.setWoodType(new ItemStack(this), woodType)));
		}

		if (isBasicTier() || Boolean.TRUE.equals(!Config.CLIENT_SPEC.isLoaded() || Config.CLIENT.showHigherTierTintedVariants.get())) {
			for (DyeColor color : DyeColor.values()) {
				ItemStack storageStack = new ItemStack(this);
				if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
					tintableBlockItem.setMainColor(storageStack, color.getTextureDiffuseColor());
					tintableBlockItem.setAccentColor(storageStack, color.getTextureDiffuseColor());
				}
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

	private boolean isBasicTier() {
		return this == ModBlocks.BARREL.get() || this == ModBlocks.CHEST.get()
				|| this == ModBlocks.LIMITED_BARREL_1.get() || this == ModBlocks.LIMITED_BARREL_2.get()
				|| this == ModBlocks.LIMITED_BARREL_3.get() || this == ModBlocks.LIMITED_BARREL_4.get();
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = new ItemStack(this);
		WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).ifPresentOrElse(be -> {
			if (includeData && be.isPacked()) {
				addDropData(stack, be);
			} else {
				addNameWoodAndTintData(stack, be);
			}
		}, () -> addNameWoodAndTintData(stack, level, pos));
		return stack;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (level.isClientSide()) {
			return;
		}

		WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).ifPresent(be -> {
			UUID storageUuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
			if (storageUuid != null) {
				ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
				if (itemContentsStorage.has(storageUuid)) {
					be.setBeingUpgraded(true);
					be.loadAdditional(itemContentsStorage.getOrCreateStorageContents(storageUuid), level.registryAccess());
					itemContentsStorage.removeStorageContents(storageUuid);
					setNewSize(stack, be);
					setTicking(level, pos, state, !be.getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class).isEmpty());
				} else {
					SophisticatedStorage.LOGGER.error("No storage contents found for uuid: " + storageUuid + " when placing " + stack.getHoverName().getString());
				}
			}

			if (stack.has(DataComponents.CUSTOM_NAME)) {
				be.setCustomName(stack.getHoverName());
			}
			setRenderBlockRenderProperties(stack, be);

			be.getStorageWrapper().onInit(level);
			be.tryToAddToController();

			if (placer != null && placer.getOffhandItem().getItem() == ModItems.STORAGE_TOOL.get()) {
				StorageToolItem.useOffHandOnPlaced(placer.getOffhandItem(), be);
			}
			be.setBeingUpgraded(false);
		});
	}

	private void setNewSize(ItemStack stack, WoodStorageBlockEntity be) {
		StorageWrapper storageWrapper = be.getStorageWrapper();
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		UpgradeHandler upgradeHandler = storageWrapper.getUpgradeHandler();
		storageWrapper.changeSize(StorageBlockItem.getNumberOfInventorySlots(stack) - inventoryHandler.getSlots(),
				StorageBlockItem.getNumberOfUpgradeSlots(stack) - upgradeHandler.getSlots());
	}

	protected void setRenderBlockRenderProperties(ItemStack stack, WoodStorageBlockEntity be) {
		WoodStorageBlockItem.getWoodType(stack).ifPresent(be::setWoodType);
		be.getStorageWrapper().setColors(StorageBlockItem.getMainColorFromComponentHolder(stack).orElse(-1), StorageBlockItem.getAccentColorFromComponentHolder(stack).orElse(-1));
		be.setUpdateBlockRender();
		WorldHelper.notifyBlockUpdate(be);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockState ret = super.playerWillDestroy(level, pos, state, player);
		WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class)
				.ifPresent(wbe -> {
					if (Boolean.TRUE.equals(Config.COMMON.dropPacked.get()) && isNonEmpty(wbe)) {
						wbe.setPacked(true);
					}

					if (wbe.isPacked()) {
						if (player.isCreative() && (
								!InventoryHelper.isEmpty(wbe.getStorageWrapper().getInventoryHandler()) || !InventoryHelper.isEmpty(wbe.getStorageWrapper().getUpgradeHandler())
						)) {
							ItemStack drop = new ItemStack(this);
							addDropData(drop, wbe);
							ItemEntity itementity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
							itementity.setDefaultPickUpDelay();
							level.addFreshEntity(itementity);
						}
					}
				});
		return ret;
	}

	@SuppressWarnings("java:S1172") //parameter is used in override
	protected InteractionResult tryItemInteraction(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand, Direction facing, BlockHitResult hitResult) {
		if (stackInHand.getItem() instanceof PackingTapeItem) {
			if (Boolean.TRUE.equals(Config.COMMON.dropPacked.get())) {
				player.displayClientMessage(Component.translatable("gui.sophisticatedstorage.status.packing_tape_disabled"), true);
				return InteractionResult.FAIL;
			} else {
				InteractionResult interactionResult = packStorage(player, hand, b, stackInHand);
				if (interactionResult instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null && success.heldItemTransformedTo().isEmpty()) {
					player.setItemInHand(hand, ItemStack.EMPTY);
				}
				return interactionResult;
			}
		}
		return tryAddUpgrade(player, b, stackInHand, facing, hitResult);
	}

	protected InteractionResult packStorage(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand) {
		if (!player.isCreative()) {
			stackInHand.setDamageValue(stackInHand.getDamageValue() + 1);
		}

		BlockState blockState = b.getBlockState();
		if (blockState.getBlock() instanceof StorageBlockBase storageBlock && blockState.getValue(StorageBlockBase.TICKING)) {
			storageBlock.setTicking(player.level(), b.getBlockPos(), blockState, false);
		}

		b.setPacked(true);

		b.removeFromController();

		WorldHelper.notifyBlockUpdate(b);

		return InteractionResult.SUCCESS.heldItemTransformedTo(stackInHand.getDamageValue() >= stackInHand.getMaxDamage() ? ItemStack.EMPTY : stackInHand);
	}
}
