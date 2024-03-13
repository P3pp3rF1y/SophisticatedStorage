package net.p3pp3rf1y.sophisticatedstorage.block;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
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

	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		if (be instanceof WoodStorageBlockEntity wbe) {
			addNameWoodAndTintData(stack, wbe);
			if (wbe.isPacked()) {
				StorageWrapper storageWrapper = be.getStorageWrapper();
				UUID storageUuid = storageWrapper.getContentsUuid().orElse(UUID.randomUUID());
				CompoundTag storageContents = wbe.getStorageContentsTag();
				if (!storageContents.isEmpty()) {
					ItemContentsStorage.get().setStorageContents(storageUuid, storageContents);
					NBTHelper.setUniqueId(stack, "uuid", storageUuid);
				}
				WoodStorageBlockItem.setPacked(stack, true);
				StorageBlockItem.setShowsTier(stack, be.shouldShowTier());
				WoodStorageBlockItem.setNumberOfUpgradeSlots(stack, storageWrapper.getInventoryHandler().getSlots());
				WoodStorageBlockItem.setNumberOfUpgradeSlots(stack, storageWrapper.getUpgradeHandler().getSlots());
			}
		}
	}

	private void addNameWoodAndTintData(ItemStack stack, WoodStorageBlockEntity wbe) {
		if (stack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			int mainColor = wbe.getStorageWrapper().getMainColor();
			if (mainColor > -1) {
				tintableBlockItem.setMainColor(stack, mainColor);
			}
			int accentColor = wbe.getStorageWrapper().getAccentColor();
			if (accentColor > -1) {
				tintableBlockItem.setAccentColor(stack, accentColor);
			}
		}
		if (wbe.hasCustomName()) {
			stack.setHoverName(wbe.getCustomName());
		}
		wbe.getWoodType().ifPresent(n -> WoodStorageBlockItem.setWoodType(stack, n));
	}

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		if (Boolean.TRUE.equals(Config.CLIENT.showSingleWoodVariantOnly.get())) {
			itemConsumer.accept(WoodStorageBlockItem.setWoodType(new ItemStack(this), WoodType.ACACIA));
		} else {
			CUSTOM_TEXTURE_WOOD_TYPES.keySet().forEach(woodType -> itemConsumer.accept(WoodStorageBlockItem.setWoodType(new ItemStack(this), woodType)));
		}

		if (isBasicTier() || Boolean.TRUE.equals(Config.CLIENT.showHigherTierTintedVariants.get())) {
			for (DyeColor color : DyeColor.values()) {
				ItemStack storageStack = new ItemStack(this);
				if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
					tintableBlockItem.setMainColor(storageStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
					tintableBlockItem.setAccentColor(storageStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
				}
				itemConsumer.accept(storageStack);
			}
			ItemStack storageStack = new ItemStack(this);
			if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
				tintableBlockItem.setMainColor(storageStack, ColorHelper.getColor(DyeColor.YELLOW.getTextureDiffuseColors()));
				tintableBlockItem.setAccentColor(storageStack, ColorHelper.getColor(DyeColor.LIME.getTextureDiffuseColors()));
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
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
		ItemStack stack = new ItemStack(this);
		addNameWoodAndTintData(stack, world, pos);
		return stack;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		WorldHelper.getBlockEntity(level, pos, WoodStorageBlockEntity.class).ifPresent(be -> {
			NBTHelper.getUniqueId(stack, "uuid").ifPresent(uuid -> {
				ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
				be.setBeingUpgraded(true);
				be.load(itemContentsStorage.getOrCreateStorageContents(uuid));
				itemContentsStorage.removeStorageContents(uuid);
			});

			if (stack.hasCustomHoverName()) {
				be.setCustomName(stack.getHoverName());
			}
			setRenderBlockRenderProperties(stack, be);

			be.getStorageWrapper().onInit();
			be.tryToAddToController();

			if (placer != null && placer.getOffhandItem().getItem() == ModItems.STORAGE_TOOL.get()) {
				StorageToolItem.useOffHandOnPlaced(placer.getOffhandItem(), be);
			}
			be.setBeingUpgraded(false);
		});
	}

	protected void setRenderBlockRenderProperties(ItemStack stack, WoodStorageBlockEntity be) {
		WoodStorageBlockItem.getWoodType(stack).ifPresent(be::setWoodType);
		StorageBlockItem.getMainColorFromStack(stack).ifPresent(be.getStorageWrapper()::setMainColor);
		StorageBlockItem.getAccentColorFromStack(stack).ifPresent(be.getStorageWrapper()::setAccentColor);
	}

	@SuppressWarnings("java:S1172") //parameter is used in override
	protected boolean tryItemInteraction(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand, Direction facing, BlockHitResult hitResult) {
		if (stackInHand.getItem() == ModItems.PACKING_TAPE.get()) {
			packStorage(player, hand, b, stackInHand);
			return true;
		}
		return tryAddUpgrade(player, hand, b, stackInHand, facing, hitResult);
	}

	protected void packStorage(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand) {
		if (!player.isCreative()) {
			stackInHand.setDamageValue(stackInHand.getDamageValue() + 1);
			if (stackInHand.getDamageValue() >= stackInHand.getMaxDamage()) {
				player.setItemInHand(hand, ItemStack.EMPTY);
			}
		}
		b.setPacked(true);

		BlockState blockState = b.getBlockState();
		if (blockState.getBlock() instanceof StorageBlockBase storageBlock && blockState.getValue(StorageBlockBase.TICKING)) {
			storageBlock.setTicking(player.level(), b.getBlockPos(), blockState, false);
		}

		b.removeFromController();

		WorldHelper.notifyBlockUpdate(b);
	}
}
