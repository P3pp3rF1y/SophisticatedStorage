package net.p3pp3rf1y.sophisticatedstorage.compat.trashslot;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public class TrashSlotCompat implements ICompat {
	@Override
	public void setup() {
		net.p3pp3rf1y.sophisticatedcore.compat.trashslot.TrashSlotCompat.registerMenuType(ModBlocks.STORAGE_CONTAINER_TYPE.get());
	}
}
