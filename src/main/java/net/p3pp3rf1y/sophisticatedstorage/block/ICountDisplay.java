package net.p3pp3rf1y.sophisticatedstorage.block;

import java.util.List;

public interface ICountDisplay {
	boolean shouldShowCounts();
	void toggleCountVisibility();
	List<Integer> getSlotCounts();
}
