package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class ControllerBlockEntity extends ControllerBlockEntityBase {
	private long lastDepositTime = -100;

	public ControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.CONTROLLER_BLOCK_ENTITY_TYPE.get(), pos, state);
	}

	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(ControllerBlockEntityBase.SEARCH_RANGE);
	}

	public void depositPlayerItems(Player player, InteractionHand hand) {
		if (getLevel() == null) {
			return;
		}
		long gameTime = getLevel().getGameTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;
		if (doubleClick) {
			player.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(
					playerInventory -> getCapability(ForgeCapabilities.ITEM_HANDLER, null)
							.ifPresent(controllerInventory -> InventoryHelper.transfer(playerInventory, controllerInventory, s -> {}, this::hasStack))
			);
			return;
		}

		ItemStack itemInHand = player.getItemInHand(hand);
		if (!itemInHand.isEmpty() && hasStack(itemInHand)) {
			player.setItemInHand(hand, insertItem(0, itemInHand, false));
		}
	}
}
