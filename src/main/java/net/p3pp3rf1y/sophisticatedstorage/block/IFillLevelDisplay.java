package net.p3pp3rf1y.sophisticatedstorage.block;

import java.util.List;

public interface IFillLevelDisplay {
	boolean shouldShowFillLevels();
	void toggleFillLevelVisibility();
	List<Float> getSlotFillLevels();
}
