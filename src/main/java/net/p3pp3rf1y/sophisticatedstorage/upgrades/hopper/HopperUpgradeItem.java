package net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper;

import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.function.IntSupplier;

public class HopperUpgradeItem extends UpgradeItemBase<HopperUpgradeWrapper> {

	private final IntSupplier inputFilterSlotCount;
	private final IntSupplier outputFilterSlotCount;
	private IntSupplier transferSpeedTicks;
	private IntSupplier maxTransferStackSize;
	public static final UpgradeType<HopperUpgradeWrapper> TYPE = new UpgradeType<>(HopperUpgradeWrapper::new);

	public HopperUpgradeItem(IntSupplier inputFilterSlotCount, IntSupplier outputFilterSlotCount, IntSupplier transferSpeedTicks, IntSupplier maxTransferStackSize) {
		super();
		this.inputFilterSlotCount = inputFilterSlotCount;
		this.outputFilterSlotCount = outputFilterSlotCount;
		this.transferSpeedTicks = transferSpeedTicks;
		this.maxTransferStackSize = maxTransferStackSize;
	}

	@Override
	public UpgradeType<HopperUpgradeWrapper> getType() {
		return TYPE;
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
