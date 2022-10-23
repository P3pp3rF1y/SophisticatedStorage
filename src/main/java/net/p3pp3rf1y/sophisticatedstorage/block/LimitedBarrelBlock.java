package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class LimitedBarrelBlock extends BarrelBlock {
	private final Supplier<Integer> getBaseStackSizeMultiplier;

	public LimitedBarrelBlock(int numberOfInventorySlots, Supplier<Integer> getBaseStackSizeMultiplier, Supplier<Integer> numberOfUpgradeSlotsSupplier, Properties properties) {
		super(() -> numberOfInventorySlots, numberOfUpgradeSlotsSupplier, properties);
		this.getBaseStackSizeMultiplier = getBaseStackSizeMultiplier;
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return getBaseStackSizeMultiplier.get();
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flat) {
		tooltip.addAll(TranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY));
	}

	@Nullable
	@Override
	public StorageBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LimitedBarrelBlockEntity(pos, state);
	}

	@Override
	protected BlockEntityType<? extends StorageBlockEntity> getBlockEntityType() {
		return ModBlocks.LIMITED_BARREL_BLOCK_ENTITY_TYPE.get();
	}

	@Override
	protected boolean tryItemInteraction(Player player, InteractionHand hand, WoodStorageBlockEntity b, ItemStack stackInHand, Direction facing, BlockHitResult hitResult) {
		if (super.tryItemInteraction(player, hand, b, stackInHand, facing, hitResult)) {
			return true;
		}
		if (hitResult.getDirection() != facing || player.isShiftKeyDown()) {
			return false;
		}
		int slot = getInteractionSlot(b.getBlockPos(), facing, hitResult);
		if (b instanceof LimitedBarrelBlockEntity limitedBarrelBlockEntity) {
			limitedBarrelBlockEntity.depositItem(player, hand, stackInHand, slot);
		}
		return true;
	}

	private int getInteractionSlot(BlockPos pos, Direction facing, BlockHitResult hitResult) {
		int invSlots = getNumberOfInventorySlots();
		if (invSlots == 1) {
			return 0;
		}

		Vector3f blockCoords = new Vector3f(hitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
		blockCoords.add(-0.5f, -0.5f, -0.5f); // move to corner
		blockCoords.transform(getNorthBasedRotation(facing.getOpposite()));
		blockCoords.add(0.5f, 0.5f, 0.5f);
		boolean top = blockCoords.y() > 0.5f;
		boolean right = blockCoords.x() > 0.5f;

		if (invSlots == 2) {
			return top ? 0 : 1;
		} else if (invSlots == 3) {
			if (top) {
				return 0;
			}
			return right ? 2 : 1;
		}

		if (top) {
			return right ? 1 : 0;
		}
		return right ? 3 : 2;
	}

	@Override
	protected StorageContainerMenu instantiateContainerMenu(int w, Player pl, BlockPos pos) {
		return new LimitedBarrelContainerMenu(w, pl, pos);
	}

	public static Quaternion getNorthBasedRotation(Direction dir) {
		return switch (dir) {
			case DOWN -> {
				Quaternion quaternion = Vector3f.XP.rotationDegrees(90);
				quaternion.mul(Vector3f.YP.rotationDegrees(180));
				yield quaternion;
			}
			case UP -> {
				Quaternion quaternion = Vector3f.XP.rotationDegrees(-90);
				quaternion.mul(Vector3f.YP.rotationDegrees(180));
				yield quaternion;
			}
			case NORTH -> Quaternion.ONE.copy();
			case SOUTH -> Vector3f.YP.rotationDegrees(180.0F);
			case WEST -> Vector3f.YP.rotationDegrees(-90.0F);
			case EAST -> Vector3f.YP.rotationDegrees(90.0F);
		};
	}

	private Optional<BlockHitResult> getHitResult(Player player) {
		HitResult hitResult = player.pick(player.getReachDistance(), 0, false);
		return hitResult instanceof BlockHitResult blockHitResult ? Optional.of(blockHitResult) : Optional.empty();
	}

	@SuppressWarnings({"deprecation"})
	@Override
	public void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (level.isClientSide()) {
			return;
		}

		WorldHelper.getBlockEntity(level, pos, LimitedBarrelBlockEntity.class).ifPresent(be -> getHitResult(player).ifPresent(blockHitResult -> tryToTakeItem(state, level, pos, player, be, blockHitResult)));
	}

	private void tryToTakeItem(BlockState state, Level level, BlockPos pos, Player player, LimitedBarrelBlockEntity be, BlockHitResult blockHitResult) {
		if (!blockHitResult.getBlockPos().equals(pos) || level.getBlockState(pos) != state || blockHitResult.getDirection() != state.getValue(FACING)) {
			return;
		}

		be.tryToTakeItem(player, getInteractionSlot(pos, blockHitResult.getDirection(), blockHitResult));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		WorldHelper.getBlockEntity(level, pos, LimitedBarrelBlockEntity.class).ifPresent(be -> {
			StorageWrapper storageWrapper = be.getStorageWrapper();
			SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
			settingsHandler.getTypeCategory(ItemDisplaySettingsCategory.class).selectSlots(0, storageWrapper.getNumberOfInventorySlots());
			settingsHandler.getTypeCategory(NoSortSettingsCategory.class).selectSlots(0, storageWrapper.getNumberOfInventorySlots());
		});
	}

	@Override
	public int getDisplayItemsCount(List<RenderInfo.DisplayItem> displayItems) {
		return getNumberOfInventorySlots();
	}

	@Override
	public boolean hasFixedIndexDisplayItems() {
		return true;
	}
}
