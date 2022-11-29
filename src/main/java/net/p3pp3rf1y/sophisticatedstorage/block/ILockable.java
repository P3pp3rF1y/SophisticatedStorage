package net.p3pp3rf1y.sophisticatedstorage.block;

public interface ILockable {
	void toggleLock();
	boolean isLocked();
	boolean shouldShowLock();
	void toggleLockVisibility();
}
