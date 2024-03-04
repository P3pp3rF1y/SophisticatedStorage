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
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class StorageTierUpgradeItem extends ItemBase {

	private final TierUpgrade tier;
	private final boolean hasTooltip;

	public StorageTierUpgradeItem(TierUpgrade tier) {
		this(tier, false);
	}

	public StorageTierUpgradeItem(TierUpgrade tier, boolean hasTooltip) {
		super(new Properties());
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
		Player player = context.getPlayer();
		return tryUpgradeStorage(stack, level, pos, state, player);
	}

	public InteractionResult tryUpgradeStorage(ItemStack stack, Level level, BlockPos pos, BlockState state, @Nullable Player player) {
		return tier.getBlockUpgradeDefinition(state.getBlock()).map(def ->
				WorldHelper.getBlockEntity(level, pos, def.blockEntityClass()).map(be -> tryUpgradeStorage(stack, pos, level, state, def, be, player)).
						orElse(InteractionResult.PASS)).orElse(InteractionResult.PASS);
	}

	private <B extends BlockEntity> InteractionResult tryUpgradeStorage(ItemStack stack, BlockPos pos, Level level, BlockState state, TierUpgradeDefinition<B> def, BlockEntity blockEntity, @Nullable Player player) {
		B be = def.blockEntityClass().cast(blockEntity);
		if (def.isUpgradingBlocked().test(be)) {
			return InteractionResult.PASS;
		}

		if (player == null) {
			return InteractionResult.PASS;
		}

		int countRequired = def.getCountRequired(state);
		if (countRequired > stack.getCount()) {
			player.displayClientMessage(Component.translatable(StorageTranslationHelper.INSTANCE.translGui("status.too_low_tier_upgrade_count"), countRequired, stack.getHoverName()), true);
			return InteractionResult.FAIL;
		}

		if (!def.upgradeStorage(player, pos, level, state, be)) {
			return InteractionResult.PASS;
		}

		if (!player.getAbilities().instabuild) {
			stack.shrink(countRequired);
		}
		return InteractionResult.SUCCESS;
	}

	private static class StorageTierUpgradeDefinition extends TierUpgradeDefinition<StorageBlockEntity> {
		private StorageTierUpgradeDefinition(StorageBlockBase newBlock, Property<?>... propertiesToCopy) {
			super(StorageBlockEntity.class, storageBlockEntity -> storageBlockEntity.isOpen() || (storageBlockEntity instanceof WoodStorageBlockEntity wbe && wbe.isPacked()), newBlock, propertiesToCopy);
		}

		@Override
		boolean upgradeStorage(@Nullable Player player, BlockPos pos, Level level, BlockState state, StorageBlockEntity blockEntity) {
			if (blockEntity instanceof net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity chestBlockEntity && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
				return upgradeDoubleChest(pos, level, state, chestBlockEntity);
			}

			BlockState newBlockState = getBlockState(state);
			if (!(newBlockState.getBlock() instanceof StorageBlockBase newStorageBlock)) {
				return false;
			}
			upgradeStorageBlock(pos, level, blockEntity, newBlockState, newStorageBlock.getNumberOfInventorySlots(), newStorageBlock.getNumberOfUpgradeSlots()).setBeingUpgraded(false);
			return true;
		}

		private StorageBlockEntity upgradeStorageBlock(BlockPos pos, Level level, StorageBlockEntity blockEntity, BlockState newBlockState, int newInventorySize, int newUpgradeSize) {
			CompoundTag beTag = new CompoundTag();
			blockEntity.saveAdditional(beTag);

			StorageBlockEntity newBlockEntity = newBlock().newBlockEntity(pos, newBlockState);
			//noinspection ConstantConditions - all storage blocks create a block entity so no chancde of null here
			newBlockEntity.setBeingUpgraded(true);
			newBlockEntity.load(beTag);

			blockEntity.setBeingUpgraded(true);
			level.removeBlockEntity(pos);
			level.removeBlock(pos, false);

			level.setBlock(pos, newBlockState, 3);
			level.setBlockEntity(newBlockEntity);
			newBlockEntity.changeStorageSize(newInventorySize - newBlockEntity.getStorageWrapper().getInventoryHandler().getSlots(), newUpgradeSize - newBlockEntity.getStorageWrapper().getUpgradeHandler().getSlots());
			WorldHelper.notifyBlockUpdate(newBlockEntity);
			return newBlockEntity;
		}

		private boolean upgradeDoubleChest(BlockPos pos, Level level, BlockState state, ChestBlockEntity chestBlockEntity) {
			BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
			ChestBlockEntity otherBlockEntity = WorldHelper.getBlockEntity(level, otherPos, ChestBlockEntity.class).orElse(null);
			BlockState otherBlockState = getBlockState(level.getBlockState(otherPos));
			if (otherBlockEntity == null || !(otherBlockState.getBlock() instanceof StorageBlockBase storageBlock)) {
				return false;
			}

			chestBlockEntity.setBeingUpgraded(true);
			otherBlockEntity.setBeingUpgraded(true);
			if (chestBlockEntity.isMainChest()) {
				StorageBlockEntity newMainBE = upgradeStorageBlock(pos, level, chestBlockEntity, getBlockState(state), storageBlock.getNumberOfInventorySlots() * 2, storageBlock.getNumberOfUpgradeSlots());
				upgradeStorageBlock(otherPos, level, otherBlockEntity, otherBlockState, storageBlock.getNumberOfInventorySlots(), storageBlock.getNumberOfUpgradeSlots()).setBeingUpgraded(false);
				newMainBE.setBeingUpgraded(false);
			} else {
				StorageBlockEntity newOtherBE = upgradeStorageBlock(pos, level, chestBlockEntity, getBlockState(state), storageBlock.getNumberOfInventorySlots(), storageBlock.getNumberOfUpgradeSlots());
				upgradeStorageBlock(otherPos, level, otherBlockEntity, otherBlockState, storageBlock.getNumberOfInventorySlots() * 2, storageBlock.getNumberOfUpgradeSlots()).setBeingUpgraded(false);
				newOtherBE.setBeingUpgraded(false);
			}
			otherBlockState.updateNeighbourShapes(level, otherPos, 3);

			return true;
		}

		@Override
		public int getCountRequired(BlockState state) {
			if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
				return 2;
			}

			return super.getCountRequired(state);
		}
	}

	private static class LimitedBarrelTierUpgradeDefinition extends StorageTierUpgradeDefinition {
		private LimitedBarrelTierUpgradeDefinition(StorageBlockBase newBlock) {
			super(newBlock, LimitedBarrelBlock.HORIZONTAL_FACING, LimitedBarrelBlock.VERTICAL_FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP);
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

		@Override
		public int getCountRequired(BlockState state) {
			if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) {
				return 2;
			}

			return super.getCountRequired(state);
		}

		public @Nullable WoodType woodType() {
			return woodType;
		}

		@Override
		boolean upgradeStorage(@Nullable Player player, BlockPos pos, Level level, BlockState state, B be) {
			if (player == null || !be.canOpen(player)) {
				return false;
			}
			BlockState otherState;
			if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
				otherState = level.getBlockState(pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state)));
			} else {
				otherState = null;
			}

			StorageBlockEntity upgradedBe = upgradeStorage(pos, level, state, be);
			upgradedBe.tryToAddToController();
			if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
				BlockPos otherPos = pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
				B otherBE = WorldHelper.getBlockEntity(level, otherPos, blockEntityClass()).orElse(null);
				if (otherBE == null) {
					return true;
				}
				upgradeStorage(otherPos, level, otherState, otherBE);
				level.getBlockEntity(otherPos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get())
						.ifPresent(otherChestBE -> {
							level.getBlockEntity(pos, ModBlocks.CHEST_BLOCK_ENTITY_TYPE.get()).ifPresent(mainBE -> {
								if (state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE) == ChestType.LEFT) {
									mainBE.joinWithChest(otherChestBE);
								} else {
									otherChestBE.joinWithChest(mainBE);
								}
							});
							WorldHelper.notifyBlockUpdate(otherChestBE);
						});
				level.getBlockState(otherPos).updateNeighbourShapes(level, otherPos, 3);
			}
			return true;
		}

		private StorageBlockEntity upgradeStorage(BlockPos pos, Level level, BlockState state, B be) {
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
			WorldHelper.notifyBlockUpdate(newBlockEntity);
			return newBlockEntity;
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

		public List<Property<?>> getPropertiesToCopy() {
			return propertiesToCopy;
		}

		public Class<B> blockEntityClass() {
			return blockEntityClass;
		}

		public Predicate<B> isUpgradingBlocked() {
			return isUpgradingBlocked;
		}

		public StorageBlockBase newBlock() {
			return newBlock;
		}

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

		public int getCountRequired(BlockState state) {
			return 1;
		}
	}

	private static Map<Block, TierUpgradeDefinition<?>> getVanillaShulkerBoxTierUpgradeDefinitions(net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock toShulkerBox) {
		return new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(Blocks.SHULKER_BOX, new VanillaTierUpgradeDefinition<>(ShulkerBoxBlockEntity.class, shulkerBoxBlockEntity -> shulkerBoxBlockEntity.openCount > 0, toShulkerBox, null, ShulkerBoxBlock.FACING))
				.put(Blocks.WHITE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.WHITE, toShulkerBox))
				.put(Blocks.ORANGE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.ORANGE, toShulkerBox))
				.put(Blocks.MAGENTA_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.MAGENTA, toShulkerBox))
				.put(Blocks.LIGHT_BLUE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIGHT_BLUE, toShulkerBox))
				.put(Blocks.YELLOW_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.YELLOW, toShulkerBox))
				.put(Blocks.LIME_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIME, toShulkerBox))
				.put(Blocks.PINK_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.PINK, toShulkerBox))
				.put(Blocks.GRAY_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.GRAY, toShulkerBox))
				.put(Blocks.LIGHT_GRAY_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.LIGHT_GRAY, toShulkerBox))
				.put(Blocks.CYAN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.CYAN, toShulkerBox))
				.put(Blocks.PURPLE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.PURPLE, toShulkerBox))
				.put(Blocks.BLUE_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BLUE, toShulkerBox))
				.put(Blocks.BROWN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BROWN, toShulkerBox))
				.put(Blocks.GREEN_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.GREEN, toShulkerBox))
				.put(Blocks.RED_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.RED, toShulkerBox))
				.put(Blocks.BLACK_SHULKER_BOX, new VanillaTintedShulkerBoxTierUpgradeDefinition(DyeColor.BLACK, toShulkerBox))
				.build();
	}

	public enum TierUpgrade {
		BASIC(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.BARREL.get(), WoodType.SPRUCE, BlockStateProperties.FACING))
				.put(Blocks.CHEST, new VanillaTierUpgradeDefinition<>(net.minecraft.world.level.block.entity.ChestBlockEntity.class, chestBlockEntity -> chestBlockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.CHEST.get(), WoodType.OAK, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.putAll(getVanillaShulkerBoxTierUpgradeDefinitions(ModBlocks.SHULKER_BOX.get()))
				.build())),
		BASIC_TO_COPPER(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.COPPER_BARREL.get(), WoodType.SPRUCE, BlockStateProperties.FACING))
				.put(Blocks.CHEST, new VanillaTierUpgradeDefinition<>(net.minecraft.world.level.block.entity.ChestBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.COPPER_CHEST.get(), WoodType.OAK, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.putAll(getVanillaShulkerBoxTierUpgradeDefinitions(ModBlocks.COPPER_SHULKER_BOX.get()))
				.put(ModBlocks.BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.COPPER_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.COPPER_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.COPPER_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_1.get()))
				.put(ModBlocks.LIMITED_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_2.get()))
				.put(ModBlocks.LIMITED_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_3.get()))
				.put(ModBlocks.LIMITED_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_COPPER_BARREL_4.get()))
				.build())),
		BASIC_TO_IRON(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.IRON_BARREL.get(), WoodType.SPRUCE, BlockStateProperties.FACING))
				.put(Blocks.CHEST, new VanillaTierUpgradeDefinition<>(net.minecraft.world.level.block.entity.ChestBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.IRON_CHEST.get(), WoodType.OAK, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.putAll(getVanillaShulkerBoxTierUpgradeDefinitions(ModBlocks.IRON_SHULKER_BOX.get()))
				.put(ModBlocks.BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_1.get()))
				.put(ModBlocks.LIMITED_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_2.get()))
				.put(ModBlocks.LIMITED_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_3.get()))
				.put(ModBlocks.LIMITED_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_4.get()))
				.build())),
		BASIC_TO_GOLD(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.GOLD_BARREL.get(), WoodType.SPRUCE, BlockStateProperties.FACING))
				.put(Blocks.CHEST, new VanillaTierUpgradeDefinition<>(net.minecraft.world.level.block.entity.ChestBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.GOLD_CHEST.get(), WoodType.OAK, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.putAll(getVanillaShulkerBoxTierUpgradeDefinitions(ModBlocks.GOLD_SHULKER_BOX.get()))
				.put(ModBlocks.BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1.get()))
				.put(ModBlocks.LIMITED_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2.get()))
				.put(ModBlocks.LIMITED_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3.get()))
				.put(ModBlocks.LIMITED_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4.get()))
				.build()
		)),
		BASIC_TO_DIAMOND(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.DIAMOND_BARREL.get(), WoodType.SPRUCE, BlockStateProperties.FACING))
				.put(Blocks.CHEST, new VanillaTierUpgradeDefinition<>(net.minecraft.world.level.block.entity.ChestBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.DIAMOND_CHEST.get(), WoodType.OAK, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.putAll(getVanillaShulkerBoxTierUpgradeDefinitions(ModBlocks.DIAMOND_SHULKER_BOX.get()))
				.put(ModBlocks.BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()))
				.put(ModBlocks.LIMITED_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()))
				.put(ModBlocks.LIMITED_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()))
				.put(ModBlocks.LIMITED_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4.get()))
				.build()
		)),
		BASIC_TO_NETHERITE(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.NETHERITE_BARREL.get(), WoodType.SPRUCE, BlockStateProperties.FACING))
				.put(Blocks.CHEST, new VanillaTierUpgradeDefinition<>(net.minecraft.world.level.block.entity.ChestBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.NETHERITE_CHEST.get(), WoodType.OAK, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.putAll(getVanillaShulkerBoxTierUpgradeDefinitions(ModBlocks.NETHERITE_SHULKER_BOX.get()))
				.put(ModBlocks.BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()))
				.put(ModBlocks.LIMITED_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()))
				.put(ModBlocks.LIMITED_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()))
				.put(ModBlocks.LIMITED_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4.get()))
				.build()
		)),
		COPPER_TO_IRON(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(ModBlocks.COPPER_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.COPPER_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.COPPER_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.IRON_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_COPPER_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_1.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_2.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_3.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_IRON_BARREL_4.get()))
				.build()
		)),
		COPPER_TO_GOLD(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(ModBlocks.COPPER_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.COPPER_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.COPPER_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_COPPER_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4.get()))
				.build()
		)),
		COPPER_TO_DIAMOND(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(ModBlocks.COPPER_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.COPPER_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.COPPER_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_COPPER_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4.get()))
				.build()
		)),
		COPPER_TO_NETHERITE(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(ModBlocks.COPPER_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.COPPER_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.COPPER_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_COPPER_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()))
				.put(ModBlocks.LIMITED_COPPER_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4.get()))
				.build()
		)),
		IRON_TO_GOLD(Map.of(
				ModBlocks.IRON_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP),
				ModBlocks.IRON_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE),
				ModBlocks.IRON_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.GOLD_SHULKER_BOX.get(), BlockStateProperties.FACING),
				ModBlocks.LIMITED_IRON_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_1.get()),
				ModBlocks.LIMITED_IRON_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_2.get()),
				ModBlocks.LIMITED_IRON_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_3.get()),
				ModBlocks.LIMITED_IRON_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_GOLD_BARREL_4.get())
		)),
		IRON_TO_DIAMOND(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(ModBlocks.IRON_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.IRON_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.IRON_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_IRON_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()))
				.put(ModBlocks.LIMITED_IRON_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()))
				.put(ModBlocks.LIMITED_IRON_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()))
				.put(ModBlocks.LIMITED_IRON_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4.get()))
				.build()
		)),
		IRON_TO_NETHERITE(new HashMap<>(new ImmutableMap.Builder<Block, TierUpgradeDefinition<?>>()
				.put(ModBlocks.IRON_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP))
				.put(ModBlocks.IRON_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE))
				.put(ModBlocks.IRON_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX.get(), BlockStateProperties.FACING))
				.put(ModBlocks.LIMITED_IRON_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()))
				.put(ModBlocks.LIMITED_IRON_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()))
				.put(ModBlocks.LIMITED_IRON_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()))
				.put(ModBlocks.LIMITED_IRON_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4.get()))
				.build()
		)),
		GOLD_TO_DIAMOND(Map.of(
				ModBlocks.GOLD_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP),
				ModBlocks.GOLD_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE),
				ModBlocks.GOLD_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.DIAMOND_SHULKER_BOX.get(), BlockStateProperties.FACING),
				ModBlocks.LIMITED_GOLD_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_1.get()),
				ModBlocks.LIMITED_GOLD_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_2.get()),
				ModBlocks.LIMITED_GOLD_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_3.get()),
				ModBlocks.LIMITED_GOLD_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_DIAMOND_BARREL_4.get())
		)),
		GOLD_TO_NETHERITE(Map.of(
				ModBlocks.GOLD_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP),
				ModBlocks.GOLD_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE),
				ModBlocks.GOLD_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX.get(), BlockStateProperties.FACING),
				ModBlocks.LIMITED_GOLD_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()),
				ModBlocks.LIMITED_GOLD_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()),
				ModBlocks.LIMITED_GOLD_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()),
				ModBlocks.LIMITED_GOLD_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4.get())
		)),
		DIAMOND_TO_NETHERITE(Map.of(
				ModBlocks.DIAMOND_BARREL.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_BARREL.get(), BlockStateProperties.FACING, StorageBlockBase.TICKING, BarrelBlock.FLAT_TOP),
				ModBlocks.DIAMOND_CHEST.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_CHEST.get(), BlockStateProperties.HORIZONTAL_FACING, StorageBlockBase.TICKING, BlockStateProperties.WATERLOGGED, BlockStateProperties.CHEST_TYPE),
				ModBlocks.DIAMOND_SHULKER_BOX.get(), new StorageTierUpgradeDefinition(ModBlocks.NETHERITE_SHULKER_BOX.get(), BlockStateProperties.FACING),
				ModBlocks.LIMITED_DIAMOND_BARREL_1.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_1.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_2.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_2.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_3.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_3.get()),
				ModBlocks.LIMITED_DIAMOND_BARREL_4.get(), new LimitedBarrelTierUpgradeDefinition(ModBlocks.LIMITED_NETHERITE_BARREL_4.get())
		));

		private final Map<Block, TierUpgradeDefinition<?>> blockUpgradeDefinitions;

		TierUpgrade(Map<Block, TierUpgradeDefinition<?>> blockUpgradeDefinitions) {
			this.blockUpgradeDefinitions = blockUpgradeDefinitions;
		}

		public void addTierUpgradeDefinition(Block block, StorageTierUpgradeItem.VanillaTierUpgradeDefinition<?> tierUpgradeDefinition) {
			blockUpgradeDefinitions.put(block, tierUpgradeDefinition);
		}

		private Optional<TierUpgradeDefinition<?>> getBlockUpgradeDefinition(Block block) {
			return Optional.ofNullable(blockUpgradeDefinitions.get(block));
		}
	}
}
