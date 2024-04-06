package net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper;

import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedstorage.Config;

import java.util.List;
import java.util.function.IntSupplier;

public class HopperUpgradeItem extends UpgradeItemBase<HopperUpgradeWrapper> {

	private final IntSupplier inputFilterSlotCount;
	private final IntSupplier outputFilterSlotCount;
	private final IntSupplier transferSpeedTicks;
	private final IntSupplier maxTransferStackSize;
	public static final UpgradeType<HopperUpgradeWrapper> TYPE = new UpgradeType<>(HopperUpgradeWrapper::new);

	public HopperUpgradeItem(IntSupplier inputFilterSlotCount, IntSupplier outputFilterSlotCount, IntSupplier transferSpeedTicks, IntSupplier maxTransferStackSize) {
		super(Config.SERVER.maxUpgradesPerStorage);
		this.inputFilterSlotCount = inputFilterSlotCount;
		this.outputFilterSlotCount = outputFilterSlotCount;
		this.transferSpeedTicks = transferSpeedTicks;
		this.maxTransferStackSize = maxTransferStackSize;
	}

	@Override
	public UpgradeType<HopperUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public int getInputFilterSlotCount() {
		return inputFilterSlotCount.getAsInt();
	}

	public int getOutputFilterSlotCount() {
		return outputFilterSlotCount.getAsInt();
	}

	public long getTransferSpeedTicks() {
		return transferSpeedTicks.getAsInt();
	}

	public int getMaxTransferStackSize() {
		return maxTransferStackSize.getAsInt();
	}
}
