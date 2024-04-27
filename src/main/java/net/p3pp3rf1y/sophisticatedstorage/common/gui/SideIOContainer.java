package net.p3pp3rf1y.sophisticatedstorage.common.gui;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.IOMode;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SideIOContainer {
	private static final String DATA_SIDE_PREFIX = "side";
	private static final String DATA_IO_MODE_PREFIX = "ioMode";
	private final Supplier<Direction> horizontalDirection;
	private final Supplier<VerticalFacing> verticalFacing;
	private final Function<Direction, IOMode> getDirectionIOMode;
	private final BiConsumer<Direction, IOMode> setDirectionIOMode;
	private final boolean canSideIOBeDisabled;
	private final IServerUpdater serverUpdater;

	public SideIOContainer(IServerUpdater serverUpdater, Supplier<Direction> horizontalDirection, Supplier<VerticalFacing> verticalFacing, Function<Direction, IOMode> getDirectionIOMode, BiConsumer<Direction, IOMode> setDirectionIOMode, boolean canSideIOBeDisabled) {
		this.serverUpdater = serverUpdater;
		this.horizontalDirection = horizontalDirection;
		this.verticalFacing = verticalFacing;
		this.getDirectionIOMode = getDirectionIOMode;
		this.setDirectionIOMode = setDirectionIOMode;
		this.canSideIOBeDisabled = canSideIOBeDisabled;
	}

	public boolean handlePacket(CompoundTag data) {
		if (data.contains(DATA_IO_MODE_PREFIX)) {
			IOMode ioMode = NBTHelper.getEnumConstant(data, DATA_IO_MODE_PREFIX, IOMode::fromName).orElse(IOMode.OFF);
			BlockSide side = NBTHelper.getEnumConstant(data, DATA_SIDE_PREFIX, BlockSide::fromName).orElse(BlockSide.FRONT);
			setSideIO(side, ioMode);
			return true;
		}

		return false;
	}

	public void toggleSideIO(BlockSide side) {
		IOMode currentIO = getSideIOMode(side);
		IOMode next = currentIO.next();
		if (!canSideIOBeDisabled && next == IOMode.DISABLED) {
			next = next.next();
		}
		setSideIO(side, next);
	}

	private void setSideIO(BlockSide side, IOMode ioMode) {
		setDirectionIOMode.accept(toDirection(side), ioMode);
		serverUpdater.sendDataToServer(() -> {
			CompoundTag tag = new CompoundTag();
			NBTHelper.putEnumConstant(tag, DATA_SIDE_PREFIX, side);
			NBTHelper.putEnumConstant(tag, DATA_IO_MODE_PREFIX, ioMode);
			return tag;
		});
	}

	public Direction toDirection(BlockSide side) {
		return side.toDirection(horizontalDirection.get(), verticalFacing.get());
	}

	public IOMode getSideIOMode(BlockSide side) {
		return getDirectionIOMode.apply(toDirection(side));
	}
}
