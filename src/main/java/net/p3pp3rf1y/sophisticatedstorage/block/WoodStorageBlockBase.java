package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
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
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import javax.annotation.Nullable;
import java.util.Set;

public abstract class WoodStorageBlockBase extends StorageBlockBase implements IAdditionalDropDataBlock {
	public static final Set<WoodType> CUSTOM_TEXTURE_WOOD_TYPES = Set.of(WoodType.ACACIA, WoodType.BIRCH, WoodType.CRIMSON, WoodType.DARK_OAK, WoodType.JUNGLE, WoodType.OAK, WoodType.SPRUCE, WoodType.WARPED);

	protected WoodStorageBlockBase(Properties properties, int numberOfInventorySlots, int numberOfUpgradeSlots) {
		super(properties, numberOfInventorySlots, numberOfUpgradeSlots);
	}

	public void addWoodAndTintData(ItemStack stack, BlockGetter level, BlockPos pos) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> addDropData(stack, be));
	}

	public void addDropData(ItemStack stack, StorageBlockEntity be) {
		if (stack.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			int mainColor = be.getMainColor();
			if (mainColor > -1) {
				tintableBlockItem.setMainColor(stack, mainColor);
			}
			int accentColor = be.getAccentColor();
			if (accentColor > -1) {
				tintableBlockItem.setAccentColor(stack, accentColor);
			}
		}
		be.getWoodType().ifPresent(n -> WoodStorageBlockItem.setWoodType(stack, n));
		be.getCustomName().ifPresent(stack::setHoverName);
	}

	@Override
	public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
		CUSTOM_TEXTURE_WOOD_TYPES.forEach(woodType -> items.add(WoodStorageBlockItem.setWoodType(new ItemStack(this), woodType)));

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

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
		ItemStack stack = new ItemStack(this);
		addWoodAndTintData(stack, world, pos);
		return stack;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(be -> {
			if (stack.hasCustomHoverName()) {
				be.setCustomName(stack.getHoverName());
			}
			WoodStorageBlockItem.getWoodType(stack).ifPresent(be::setWoodType);
			StorageBlockItem.getMaincolorFromStack(stack).ifPresent(be::setMainColor);
			StorageBlockItem.getAccentColorFromStack(stack).ifPresent(be::setAccentColor);
		});
	}
}
