package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class StorageTierUpgradeItem extends ItemBase {
	private static final Map<TierUpgrade, Map<Block, TierUpgradeDefinition<?>>> TIER_UPGRADE_DEFINITIONS = Map.of(
			TierUpgrade.BASIC, Map.of(
					Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BlockStateProperties.FACING, BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.BARREL.get(), WoodType.SPRUCE),
					Blocks.CHEST, new VanillaTierUpgradeDefinition<>(ChestBlock.FACING, ChestBlockEntity.class, chestBlockEntity -> chestBlockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.CHEST.get(), WoodType.OAK)
			),
			TierUpgrade.BASIC_TO_IRON, Map.of(
					Blocks.BARREL, new VanillaTierUpgradeDefinition<>(BlockStateProperties.FACING, BarrelBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.IRON_BARREL.get(), WoodType.SPRUCE),
					Blocks.CHEST, new VanillaTierUpgradeDefinition<>(ChestBlock.FACING, ChestBlockEntity.class, blockEntity -> blockEntity.openersCounter.getOpenerCount() > 0, ModBlocks.IRON_CHEST.get(), WoodType.OAK),
					ModBlocks.BARREL.get(), new StorageTierUpgradeDefinition(BlockStateProperties.FACING, ModBlocks.IRON_BARREL.get()),
					ModBlocks.CHEST.get(), new StorageTierUpgradeDefinition(ChestBlock.FACING, ModBlocks.IRON_CHEST.get())
			),
			TierUpgrade.IRON_TO_GOLD, Map.of(
					ModBlocks.IRON_BARREL.get(), new StorageTierUpgradeDefinition(BlockStateProperties.FACING, ModBlocks.GOLD_BARREL.get()),
					ModBlocks.IRON_CHEST.get(), new StorageTierUpgradeDefinition(ChestBlock.FACING, ModBlocks.GOLD_CHEST.get())
			),
			TierUpgrade.GOLD_TO_DIAMOND, Map.of(
					ModBlocks.GOLD_BARREL.get(), new StorageTierUpgradeDefinition(BlockStateProperties.FACING, ModBlocks.DIAMOND_BARREL.get()),
					ModBlocks.GOLD_CHEST.get(), new StorageTierUpgradeDefinition(ChestBlock.FACING, ModBlocks.DIAMOND_CHEST.get())
			),
			TierUpgrade.DIAMOND_TO_NETHERITE, Map.of(
					ModBlocks.DIAMOND_BARREL.get(), new StorageTierUpgradeDefinition(BlockStateProperties.FACING, ModBlocks.NETHERITE_BARREL.get()),
					ModBlocks.DIAMOND_CHEST.get(), new StorageTierUpgradeDefinition(ChestBlock.FACING, ModBlocks.NETHERITE_CHEST.get())
			)
	);

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
		if (def.isOpen().test(be)) {
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
		private StorageTierUpgradeDefinition(DirectionProperty facingProperty, StorageBlockBase newBlock) {
			super(facingProperty, StorageBlockEntity.class, StorageBlockEntity::isOpen, newBlock);
		}

		@Override
		boolean upgradeStorage(@Nullable Player player, BlockPos pos, Level level, BlockState state, StorageBlockEntity blockEntity) {
			CompoundTag beTag = new CompoundTag();
			blockEntity.saveAdditional(beTag);

			Direction facing = state.getValue(facingProperty());

			BlockState newBlockState = newBlock().defaultBlockState();
			newBlockState = newBlockState.setValue(facingProperty(), facing);
			StorageBlockEntity newBlockEntity = newBlock().newBlockEntity(pos, newBlockState);
			//noinspection ConstantConditions - all storage blocks create a block entity so no chancde of null here
			int newInventorySize = newBlockEntity.getStorageWrapper().getInventoryHandler().getSlots();
			int newUpgradeSize = newBlockEntity.getStorageWrapper().getUpgradeHandler().getSlots();
			newBlockEntity.load(beTag);
			newBlockEntity.getStorageWrapper().increaseSize(newInventorySize - newBlockEntity.getStorageWrapper().getInventoryHandler().getSlots(), newUpgradeSize - newBlockEntity.getStorageWrapper().getUpgradeHandler().getSlots());

			level.removeBlockEntity(pos);
			level.removeBlock(pos, false);

			level.setBlock(pos, newBlockState, 3);
			level.setBlockEntity(newBlockEntity);
			WorldHelper.notifyBlockUpdate(newBlockEntity);
			return true;
		}
	}

	private static class VanillaTierUpgradeDefinition<B extends RandomizableContainerBlockEntity> extends TierUpgradeDefinition<B> {
		private final @Nullable WoodType woodType;

		private VanillaTierUpgradeDefinition(DirectionProperty facingProperty, Class<B> blockEntityClass, Predicate<B> isOpen, StorageBlockBase newBlock, @Nullable WoodType woodType) {
			super(facingProperty, blockEntityClass, isOpen, newBlock);
			this.woodType = woodType;
		}

		public @Nullable WoodType woodType() {return woodType;}

		@Override
		boolean upgradeStorage(@Nullable Player player, BlockPos pos, Level level, BlockState state, B be) {
			if (player == null || !be.canOpen(player)) {
				return false;
			}
			Direction facing = state.getValue(facingProperty());
			Component customName = be.getCustomName();
			NonNullList<ItemStack> items = NonNullList.create();
			for (int slot = 0; slot < be.getContainerSize(); slot++) {
				items.add(slot, be.getItem(slot));
			}

			BlockState newBlockState = newBlock().defaultBlockState();
			newBlockState = newBlockState.setValue(facingProperty(), facing);
			StorageBlockEntity newBlockEntity = newBlock().newBlockEntity(pos, newBlockState);
			//noinspection ConstantConditions - all storage blocks create a block entity so no chancde of null here
			setStorageItemsNameAndWoodType(newBlockEntity, customName, items, woodType());
			newBlockEntity.setUpdateBlockRender();
			replaceBlockAndBlockEntity(newBlockState, newBlockEntity, pos, level);
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
			InventoryHandler inventoryHandler = newBe.getStorageWrapper().getInventoryHandler();
			if (inventoryHandler.getSlots() < items.size()) {
				inventoryHandler.setSize(items.size());
			}

			for (int slot = 0; slot < items.size(); slot++) {
				inventoryHandler.setStackInSlot(slot, items.get(slot));
			}

			if (woodType != null) {
				newBe.setWoodType(woodType);
			}
		}
	}

	private abstract static class TierUpgradeDefinition<B extends BlockEntity> {
		private final DirectionProperty facingProperty;
		private final Class<B> blockEntityClass;
		private final Predicate<B> isOpen;
		private final StorageBlockBase newBlock;

		private TierUpgradeDefinition(DirectionProperty facingProperty,
				Class<B> blockEntityClass, Predicate<B> isOpen,
				StorageBlockBase newBlock) {
			this.facingProperty = facingProperty;
			this.blockEntityClass = blockEntityClass;
			this.isOpen = isOpen;
			this.newBlock = newBlock;
		}

		public DirectionProperty facingProperty() {return facingProperty;}

		public Class<B> blockEntityClass() {return blockEntityClass;}

		public Predicate<B> isOpen() {return isOpen;}

		public StorageBlockBase newBlock() {return newBlock;}

		abstract boolean upgradeStorage(@Nullable Player player, BlockPos pos, Level level, BlockState state, B b);
	}

	public enum TierUpgrade {
		BASIC,
		BASIC_TO_IRON,
		IRON_TO_GOLD,
		GOLD_TO_DIAMOND,
		DIAMOND_TO_NETHERITE
	}
}
