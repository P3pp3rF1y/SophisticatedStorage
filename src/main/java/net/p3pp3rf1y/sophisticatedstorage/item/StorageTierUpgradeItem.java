package net.p3pp3rf1y.sophisticatedstorage.item;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class StorageTierUpgradeItem extends ItemBase {
	private static final Map<TierUpgrade, Map<Block, TierUpgradeDefinition<?>>> TIER_UPGRADE_DEFINITIONS = Map.of(
			TierUpgrade.BASIC, new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
					.put(Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.BARREL.get(), WoodType.SPRUCE, BlockStateProperties.FACING))
					.put(Blocks.CHEST, new VanillaTierUpgradeDefinition<>(ChestBlockEntity.class, chestBlockEntity -> chestBlockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.CHEST.get(), WoodType.OAK, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED))
					.put(Blocks.SHULKER_BOX, new VanillaTierUpgradeDefinition<>(ShulkerBoxBlockEntity.class, shulkerBoxBlockEntity -> shulkerBoxBlockEntity.openCount > 0, ModBlocks.SHULKER_BOX.get(), null, ShulkerBoxBlock.FACING))
					.put(Blocks.WHITE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.WHITE, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.ORANGE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.ORANGE, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.MAGENTA_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.MAGENTA, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.LIGHT_BLUE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIGHT_BLUE, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.YELLOW_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.YELLOW, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.LIME_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIME, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.PINK_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.PINK, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.GRAY_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.GRAY, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.LIGHT_GRAY_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIGHT_GRAY, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.CYAN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.CYAN, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.PURPLE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.PURPLE, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.BLUE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BLUE, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.BROWN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BROWN, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.GREEN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.GREEN, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.RED_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.RED, ModBlocks.SHULKER_BOX.get()))
					.put(Blocks.BLACK_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BLACK, ModBlocks.SHULKER_BOX.get()))
					.build()
			),
			TierUpgrade.BASIC_TO_IRON, new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
					.put(Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.IRON_BARREL.get(), WoodType.SPRUCE, BlockStateProperties.FACING))
					.put(Blocks.CHEST, new VanillaTierUpgradeDefinition<>(ChestBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.IRON_CHEST.get(), WoodType.OAK, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED))
					.put(Blocks.SHULKER_BOX, new VanillaTierUpgradeDefinition<>(ShulkerBoxBlockEntity.class, blockEntity -> blockEntity.openCount > 0, ModBlocks.IRON_SHULKER_BOX.get(), null, ShulkerBoxBlock.FACING))
					.put(Blocks.WHITE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.WHITE, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.ORANGE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.ORANGE, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.MAGENTA_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.MAGENTA, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.LIGHT_BLUE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIGHT_BLUE, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.YELLOW_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.YELLOW, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.LIME_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIME, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.PINK_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.PINK, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.GRAY_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.GRAY, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.LIGHT_GRAY_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIGHT_GRAY, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.CYAN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.CYAN, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.PURPLE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.PURPLE, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.BLUE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BLUE, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.BROWN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BROWN, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.GREEN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.GREEN, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.RED_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.RED, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(Blocks.BLACK_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BLACK, ModBlocks.IRON_SHULKER_BOX.get()))
					.put(ModBlocks.BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING))
					.put(ModBlocks.CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED))
					.put(ModBlocks.SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_SHULKER_BOX.get(), BlockStateProperties.FACING))
					.put(ModBlocks.LIMITED_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_1.get()))
					.put(ModBlocks.LIMITED_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_2.get()))
					.put(ModBlocks.LIMITED_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_3.get()))
					.put(ModBlocks.LIMITED_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_4.get()))
					.build()
			),
			TierUpgrade.IRON_TO_GOLD, Map.of(
					ModBlocks.IRON_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING),
					ModBlocks.IRON_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED),
					ModBlocks.IRON_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX.get(), BlockStateProperties.FACING),
					ModBlocks.LIMITED_IRON_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1.get()),
					ModBlocks.LIMITED_IRON_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2.get()),
					ModBlocks.LIMITED_IRON_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3.get()),
					ModBlocks.LIMITED_IRON_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4.get())
			),
			TierUpgrade.GOLD_TO_DIAMOND, Map.of(
					ModBlocks.GOLD_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING),
					ModBlocks.GOLD_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED),
					ModBlocks.GOLD_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX.get(), BlockStateProperties.FACING),
					ModBlocks.LIMITED_GOLD_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()),
					ModBlocks.LIMITED_GOLD_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()),
					ModBlocks.LIMITED_GOLD_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()),
					ModBlocks.LIMITED_GOLD_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4.get())
			),
			TierUpgrade.DIAMOND_TO_NETHERITE, Map.of(
					ModBlocks.DIAMOND_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING),
					ModBlocks.DIAMOND_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED),
					ModBlocks.DIAMOND_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX.get(), BlockStateProperties.FACING),
					ModBlocks.LIMITED_DIAMOND_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()),
					ModBlocks.LIMITED_DIAMOND_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()),
					ModBlocks.LIMITED_DIAMOND_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()),
					ModBlocks.LIMITED_DIAMOND_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4.get())
			)
	);

	public static void addTierUpgradeDefinition(TierUpgrade tier, Block block, TierUpgradeDefinition<?> tierUpgradeDefinition) {
		TIER_UPGRADE_DEFINITIONS.get(tier).put(block, tierUpgradeDefinition);
	}

	private final TierUpgrade tier;
	private final boolean hasTooltip;

	public StorageTierUpgradeItem(TierUpgrade tier) {
		this(tier, false);
	}

	public StorageTierUpgradeItem(TierUpgrade tier, boolean hasTooltip) {
		super(new Properties().stacksTo(1), SophisticatedStorage.CREATIVE_TAB);
		this.tier = tier;
		this.hasTooltip = hasTooltip;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
		if (hasTooltip) {
			tooltipComponents.addAll(StorageTranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY));
		}
		super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		Level level = context.getLevel();
		if (level.isClientSide) {
			return InteractionResult.PASS;
		}

		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (TIER_UPGRADE_DEFINITIONS.get(tier).containsKey(state.getBlock())) {
			TierUpgradeDefinition<?> def = TIER_UPGRADE_DEFINITIONS.get(tier).get(state.getBlock());
			return WorldHelper.getBlockEntity(level, pos, def.blockEntityClass()).map(be -> tryUpgradeStorage(stack, context, pos, level, state, def, be)).
					orElse(InteractionResult.PASS);
		}

		return super.onItemUseFirst(stack, context);
	}

	private <B extends BlockEntity> InteractionResult tryUpgradeStorage(ItemStack stack, UseOnContext context, BlockPos pos, Level level, BlockState state, TierUpgradeDefinition<B> def, BlockEntity blockEntity) {
		B be = def.blockEntityClass().cast(blockEntity);
		if (def.isUpgradingBlocked().test(be)) {
			return InteractionResult.PASS;
		}

		Player player = context.getPlayer();

		if (player == null) {
			return InteractionResult.PASS;
		}

		if (!def.upgradeStorage(player, pos, level, state, be)) {
			return InteractionResult.PASS;
		}

		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	private static class StorageTierUpgradeDefinition extends TierUpgradeDefinition<StorageBlockEntity> {
		private StorageTierUpgradeDefinition(StorageBlockBase newBlock, Property<?>... propertiesToCopy) {
			super(StorageBlockEntity.class, storageBlockEntity -> storageBlockEntity.isOpen() || (storageBlockEntity instanceof WoodStorageBlockEntity wbe && wbe.isPacked()), newBlock, propertiesToCopy);
		}

		@Override
		boolean upgradeStorage(@Nullable Player player, BlockPos pos, Level level, BlockState state, StorageBlockEntity blockEntity) {
			CompoundTag beTag = new CompoundTag();
			blockEntity.saveAdditional(beTag);

			BlockState newBlockState = getBlockState(state);
			StorageBlockEntity newBlockEntity = newBlock().newBlockEntity(pos, newBlockState);
			//noinspection ConstantConditions - all storage blocks create a block entity so no chancde of null here
			int newInventorySize = newBlockEntity.getStorageWrapper().getInventoryHandler().getSlots();
			int newUpgradeSize = newBlockEntity.getStorageWrapper().getUpgradeHandler().getSlots();
			newBlockEntity.load(beTag);

			blockEntity.setBeingUpgraded();
			level.removeBlockEntity(pos);
			level.removeBlock(pos, false);

			level.setBlock(pos, newBlockState, 3);
			level.setBlockEntity(newBlockEntity);
			newBlockEntity.increaseStorageSize(newInventorySize - newBlockEntity.getStorageWrapper().getInventoryHandler().getSlots(), newUpgradeSize - newBlockEntity.getStorageWrapper().getUpgradeHandler().getSlots());
			WorldHelper.notifyBlockUpdate(newBlockEntity);
			return true;
		}
	}

	private static class LimitedBarrelTierUpgradeDefinition extends StorageTierUpgradeDefinition {
		private LimitedBarrelTierUpgradeDefinition(StorageBlockBase newBlock) {
			super(newBlock, LimitedBarrelBlock.HORIZONTAL_FACING, LimitedBarrelBlock.VERTICAL_FACING, StorageBlockBase.TICKING);
		}
	}

	private static class VanillaTintedShulkerBoxTierUpgradeDefinition extends VanillaTierUpgradeDefinition<ShulkerBoxBlockEntity> {
		private VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor color, net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock newBlock) {
			super(ShulkerBoxBlockEntity.class, shulkerBoxBlockEntity -> shulkerBoxBlockEntity.openCount > 0, newBlock, null, color, ShulkerBoxBlock.FACING, StorageBlockBase.TICKING);
		}
	}

	public static class VanillaTierUpgradeDefinition<B extends RandomizableContainerBlockEntity> extends TierUpgradeDefinition<B> {
		private final @Nullable WoodType woodType;
		private final int color;

		public VanillaTierUpgradeDefinition(Class<B> blockEntityClass, Predicate<B> isUpgradingBlocked, StorageBlockBase newBlock, @Nullable WoodType woodType, Property<?>... propertiesToCopy) {
			this(blockEntityClass, isUpgradingBlocked, newBlock, woodType, -1, propertiesToCopy);
		}

		private VanillaTierUpgradeDefinition(Class<B> blockEntityClass, Predicate<B> isUpgradingBlocked, StorageBlockBase newBlock, @Nullable WoodType woodType, DyeColor color, Property<?>... propertiesToCopy) {
			this(blockEntityClass, isUpgradingBlocked, newBlock, woodType, ColorHelper.getColor(color.getTextureDiffuseColors()), propertiesToCopy);
		}

		private VanillaTierUpgradeDefinition(Class<B> blockEntityClass, Predicate<B> isUpgradingBlocked, StorageBlockBase newBlock, @Nullable WoodType woodType, int color, Property<?>... propertiesToCopy) {
			super(blockEntityClass, isUpgradingBlocked, newBlock, propertiesToCopy);
			this.woodType = woodType;
			this.color = color;
		}

		public @Nullable WoodType woodType() {return woodType;}

		@Override
		boolean upgradeStorage(@Nullable Player player, BlockPos pos, Level level, BlockState state, B be) {
			if (player == null || !be.canOpen(player)) {
				return false;
			}
			Component customName = be.getCustomName();
			NonNullList<ItemStack> items = NonNullList.create();
			for (int slot = 0; slot < be.getContainerSize(); slot++) {
				items.add(slot, be.getItem(slot));
			}

			BlockState newBlockState = getBlockState(state);
			StorageBlockEntity newBlockEntity = newBlock().newBlockEntity(pos, newBlockState);
			//noinspection ConstantConditions - all storage blocks create a block entity so no chancde of null here
			setStorageItemsNameAndWoodType(newBlockEntity, customName, items, woodType());
			newBlockEntity.setUpdateBlockRender();
			replaceBlockAndBlockEntity(newBlockState, newBlockEntity, pos, level);
			newBlockEntity.tryToAddToController();
			WorldHelper.notifyBlockUpdate(newBlockEntity);
			return true;
		}

		private void replaceBlockAndBlockEntity(BlockState newBlockState, BlockEntity newBlockEntity, BlockPos pos, Level level) {
			level.removeBlockEntity(pos);
			level.removeBlock(pos, false);

			level.setBlock(pos, newBlockState, 3);
			level.setBlockEntity(newBlockEntity);
		}

		private void setStorageItemsNameAndWoodType(StorageBlockEntity newBe, @Nullable Component customName, NonNullList<ItemStack> items, @Nullable WoodType woodType) {
			if (customName != null) {
				newBe.setCustomName(customName);
			}
			StorageWrapper storageWrapper = newBe.getStorageWrapper();
			InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
			if (inventoryHandler.getSlots() < items.size()) {
				inventoryHandler.setSize(items.size());
			}

			for (int slot = 0; slot < items.size(); slot++) {
				inventoryHandler.setStackInSlot(slot, items.get(slot));
			}

			if (woodType != null && newBe instanceof WoodStorageBlockEntity wbe) {
				wbe.setWoodType(woodType);
			}

			if (color > -1) {
				storageWrapper.setMainColor(color);
				storageWrapper.setAccentColor(color);
			}
		}
	}

	private abstract static class TierUpgradeDefinition<B extends BlockEntity> {
		private final List<Property<?>> propertiesToCopy;
		private final Class<B> blockEntityClass;
		private final Predicate<B> isUpgradingBlocked;
		private final StorageBlockBase newBlock;

		private TierUpgradeDefinition(Class<B> blockEntityClass, Predicate<B> isUpgradingBlocked, StorageBlockBase newBlock, Property<?>... propertiesToCopy) {
			this.propertiesToCopy = Arrays.stream(propertiesToCopy).toList();
			this.blockEntityClass = blockEntityClass;
			this.isUpgradingBlocked = isUpgradingBlocked;
			this.newBlock = newBlock;
		}

		public List<Property<?>> getPropertiesToCopy() {return propertiesToCopy;}

		public Class<B> blockEntityClass() {return blockEntityClass;}

		public Predicate<B> isUpgradingBlocked() {return isUpgradingBlocked;}

		public StorageBlockBase newBlock() {return newBlock;}

		abstract boolean upgradeStorage(@Nullable Player player, BlockPos pos, Level level, BlockState state, B b);

		protected BlockState getBlockState(BlockState state) {
			BlockState newBlockState = newBlock().defaultBlockState();
			for (Property<?> property : getPropertiesToCopy()) {
				newBlockState = setProperty(newBlockState, state, property);
			}
			return newBlockState;
		}

		private <T extends Comparable<T>> BlockState setProperty(BlockState newBlockState, BlockState state, Property<T> property) {
			return newBlockState.setValue(property, state.getValue(property));
		}
	}

	public enum TierUpgrade {
		BASIC,
		BASIC_TO_IRON,
		IRON_TO_GOLD,
		GOLD_TO_DIAMOND,
		DIAMOND_TO_NETHERITE
	}
}
