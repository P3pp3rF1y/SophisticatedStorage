package net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogicContainer;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.SideIOContainer;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.IOMode;

import javax.annotation.Nullable;

public class HopperUpgradeContainer extends UpgradeContainerBase<HopperUpgradeWrapper, HopperUpgradeContainer> {
	public static final Pair<ResourceLocation, ResourceLocation> EMPTY_INPUT_FILTER_SLOT_BACKGROUND = new Pair<>(InventoryMenu.BLOCK_ATLAS, SophisticatedStorage.getRL("item/empty_input_filter_slot"));
	public static final Pair<ResourceLocation, ResourceLocation> EMPTY_OUTPUT_FILTER_SLOT_BACKGROUND = new Pair<>(InventoryMenu.BLOCK_ATLAS, SophisticatedStorage.getRL("item/empty_output_filter_slot"));
	private final ContentsFilterLogicContainer inputFilterLogicContainer;

	private final ContentsFilterLogicContainer outputFilterLogicContainer;
	private final SideIOContainer sideIOContainer;

	@Nullable
	private Direction horizontalDirection;
	@Nullable
	private VerticalFacing verticalFacing;

	public HopperUpgradeContainer(Player player, int upgradeContainerId, HopperUpgradeWrapper upgradeWrapper, UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);

		inputFilterLogicContainer = new ContentsFilterLogicContainer(upgradeWrapper::getInputFilterLogic, this, this::addInputFilterSlot);
		outputFilterLogicContainer = new ContentsFilterLogicContainer(upgradeWrapper::getOutputFilterLogic, this, this::addOutputFilterSlot);

		sideIOContainer = new SideIOContainer(this, this::getHorizontalDirection, this::getVerticalFacing, this::getDirectionIOMode, this::setDirectionIOMode, false);
	}

	private void addInputFilterSlot(Slot slot) {
		slot.setBackground(EMPTY_INPUT_FILTER_SLOT_BACKGROUND.getFirst(), EMPTY_INPUT_FILTER_SLOT_BACKGROUND.getSecond());
		slots.add(slot);
	}

	private void addOutputFilterSlot(Slot slot) {
		slot.setBackground(EMPTY_OUTPUT_FILTER_SLOT_BACKGROUND.getFirst(), EMPTY_OUTPUT_FILTER_SLOT_BACKGROUND.getSecond());
		slots.add(slot);
	}

	private Direction getHorizontalDirection() {
		if (horizontalDirection == null) {
			initBlockRotation();
		}
		return horizontalDirection;
	}

	private VerticalFacing getVerticalFacing() {
		if (verticalFacing == null) {
			initBlockRotation();
		}
		return verticalFacing;
	}

	private void initBlockRotation() {
		horizontalDirection = Direction.NORTH;
		verticalFacing = VerticalFacing.NO;
		if (player.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
			storageContainerMenu.getBlockPosition().ifPresent(pos -> {
				BlockState state = player.level().getBlockState(pos);
				if (state.getBlock() instanceof StorageBlockBase storageBlock) {
					horizontalDirection = storageBlock.getHorizontalDirection(state);
					verticalFacing = storageBlock.getVerticalFacing(state);
				}
			});
		}
	}

	private void setDirectionIOMode(Direction direction, IOMode ioMode) {
		switch (ioMode) {
			case OFF:
				upgradeWrapper.setPullingFrom(direction, false);
				upgradeWrapper.setPushingTo(direction, false);
				break;
			case PUSH:
				upgradeWrapper.setPullingFrom(direction, false);
				upgradeWrapper.setPushingTo(direction, true);
				break;
			case PULL:
				upgradeWrapper.setPullingFrom(direction, true);
				upgradeWrapper.setPushingTo(direction, false);
				break;
			case PUSH_PULL:
				upgradeWrapper.setPullingFrom(direction, true);
				upgradeWrapper.setPushingTo(direction, true);
				break;
		}
	}

	private IOMode getDirectionIOMode(Direction direction) {
		boolean pulling = upgradeWrapper.isPullingFrom(direction);
		boolean pushing = upgradeWrapper.isPushingTo(direction);
		if (pulling && pushing) {
			return IOMode.PUSH_PULL;
		} else if (pulling) {
			return IOMode.PULL;
		} else if (pushing) {
			return IOMode.PUSH;
		}

		return IOMode.OFF;
	}

	@Override
	public void handlePacket(CompoundTag data) {
		inputFilterLogicContainer.handlePacket(data);
		outputFilterLogicContainer.handlePacket(data);
		sideIOContainer.handlePacket(data);
	}

	public ContentsFilterLogicContainer getInputFilterLogicContainer() {
		return inputFilterLogicContainer;
	}

	public ContentsFilterLogicContainer getOutputFilterLogicContainer() {
		return outputFilterLogicContainer;
	}

	public SideIOContainer getSideIOContainer() {
		return sideIOContainer;
	}
}
