package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ItemContentsStorage extends SavedData {
	private static final String SAVED_DATA_NAME = SophisticatedStorage.MOD_ID;

	private final Map<UUID, CompoundTag> storageContents = new HashMap<>();
	private static final ItemContentsStorage clientStorageCopy = new ItemContentsStorage();

	private ItemContentsStorage() {}

	public static ItemContentsStorage get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				//noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(ItemContentsStorage::load, ItemContentsStorage::new, SAVED_DATA_NAME);
			}
		}
		return clientStorageCopy;
	}

	public static ItemContentsStorage load(CompoundTag nbt) {
		ItemContentsStorage storage = new ItemContentsStorage();
		readStorageContents(nbt, storage);
		return storage;
	}

	private static void readStorageContents(CompoundTag nbt, ItemContentsStorage storage) {
		ListTag storageContents =  nbt.getList(nbt.contains("shulkerBoxContents") ? "shulkerBoxContents" : "storageContents", Tag.TAG_COMPOUND);
		for (Tag n : storageContents) {
			CompoundTag uuidContentsPair = (CompoundTag) n;
			UUID uuid = NbtUtils.loadUUID(Objects.requireNonNull(uuidContentsPair.get("uuid")));
			CompoundTag contents = uuidContentsPair.getCompound("contents");
			storage.storageContents.put(uuid, contents);
		}
	}

	@Override
	public CompoundTag save(CompoundTag compound) {
		CompoundTag ret = new CompoundTag();
		writeStorageContents(ret);
		return ret;
	}

	private void writeStorageContents(CompoundTag ret) {
		ListTag storageContentsNbt = new ListTag();
		for (Map.Entry<UUID, CompoundTag> entry : storageContents.entrySet()) {
			CompoundTag uuidContentsPair = new CompoundTag();
			uuidContentsPair.put("uuid", NbtUtils.createUUID(entry.getKey()));
			uuidContentsPair.put("contents", entry.getValue());
			storageContentsNbt.add(uuidContentsPair);
		}
		ret.put("storageContents", storageContentsNbt);
	}

	public CompoundTag getOrCreateStorageContents(UUID storageUuid) {
		return storageContents.computeIfAbsent(storageUuid, uuid -> {
			setDirty();
			return new CompoundTag();
		});
	}

	public void removeStorageContents(UUID storageUuid) {
		storageContents.remove(storageUuid);
		setDirty();
	}

	public void setStorageContents(UUID storageUuid, CompoundTag contents) {
		storageContents.put(storageUuid, contents);
		setDirty();
	}
}
