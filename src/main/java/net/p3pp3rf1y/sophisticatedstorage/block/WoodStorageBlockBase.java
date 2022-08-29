package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.HitResult;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class WoodStorageBlockBase extends StorageBlockBase implements IAdditionalDropDataBlock {
	public static final Set<WoodType> CUSTOM_TEXTURE_WOOD_TYPES = Set.of(WoodType.ACACIA, WoodType.BIRCH, WoodType.CRIMSON, WoodType.DARK_OAK, WoodType.JUNGLE, WoodType.OAK, WoodType.SPRUCE, WoodType.WARPED);

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
				wbe.setPacked(false);
				StorageWrapper storageWrapper = be.getStorageWrapper();
				UUID storageUuid = storageWrapper.getContentsUuid().orElse(UUID.randomUUID());
				CompoundTag storageContents = be.saveWithoutMetadata();
				if (!storageContents.isEmpty()) {
					ItemContentsStorage.get().setStorageContents(storageUuid, storageContents);
					NBTHelper.setUniqueId(stack, "uuid", storageUuid);
				}
				WoodStorageBlockItem.setPacked(stack, true);
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
		wbe.getCustomName().ifPresent(stack::setHoverName);
		wbe.getWoodType().ifPresent(n -> WoodStorageBlockItem.setWoodType(stack, n));
	}

	@Override
	public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
		CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> items.add(WoodStorageBlockItem.setWoodType(new ItemStack(this), woodType)));

		if (isBasicTier() || Boolean.TRUE.equals(Config.CLIENT.showHigherTierTintedVariants.get())) {
			for (DyeColor color : DyeColor.values()) {
				ItemStack storageStack = new ItemStack(this);
				if (storageStack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
					tintableBlockItem.setMainColor(storageStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
					tintableBlockItem.setAccentColor(storageStack, ColorHelper.getColor(color.getTextureDiffuseColors()));
				}
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

	private boolean isBasicTier() {
		return this == ModBlocks.BARREL.get() || this == ModBlocks.CHEST.get();
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
				be.load(itemContentsStorage.getOrCreateStorageContents(uuid));
				itemContentsStorage.removeStorageContents(uuid);
			});

			if (stack.hasCustomHoverName()) {
				be.setCustomName(stack.getHoverName());
			}
			WoodStorageBlockItem.getWoodType(stack).ifPresent(be::setWoodType);
			StorageBlockItem.getMainColorFromStack(stack).ifPresent(be.getStorageWrapper()::setMainColor);
			StorageBlockItem.getAccentColorFromStack(stack).ifPresent(be.getStorageWrapper()::setAccentColor);

			be.tryToAddToController();
		});
	}

	protected boolean tryPackBlock(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand) {
		if (stackInHand.getItem() == ModItems.PACKING_TAPE.get()) {
			if (!player.isCreative()) {
				stackInHand.setDamageValue(stackInHand.getDamageValue() + 1);
				if (stackInHand.getDamageValue() >= stackInHand.getMaxDamage()) {
					player.setItemInHand(hand, ItemStack.EMPTY);
				}
			}
			b.setPacked(true);

			b.removeFromController();

			WorldHelper.notifyBlockUpdate(b);
			return true;
		}
		return false;
	}
}
