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

public class ShulkerBoxStorage extends SavedData {
	private static final String SAVED_DATA_NAME = SophisticatedStorage.MOD_ID;

	private final Map<UUID, CompoundTag> shulkerContents = new HashMap<>();
	private static final ShulkerBoxStorage clientStorageCopy = new ShulkerBoxStorage();

	private ShulkerBoxStorage() {}

	public static ShulkerBoxStorage get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				//noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(ShulkerBoxStorage::load, ShulkerBoxStorage::new, SAVED_DATA_NAME);
			}
		}
		return clientStorageCopy;
	}

	public static ShulkerBoxStorage load(CompoundTag nbt) {
		ShulkerBoxStorage storage = new ShulkerBoxStorage();
		readShulkerContents(nbt, storage);
		return storage;
	}

	private static void readShulkerContents(CompoundTag nbt, ShulkerBoxStorage storage) {
		for (Tag n : nbt.getList("shulkerBoxContents", Tag.TAG_COMPOUND)) {
			CompoundTag uuidContentsPair = (CompoundTag) n;
			UUID uuid = NbtUtils.loadUUID(Objects.requireNonNull(uuidContentsPair.get("uuid")));
			CompoundTag contents = uuidContentsPair.getCompound("contents");
			storage.shulkerContents.put(uuid, contents);
		}
	}

	@Override
	public CompoundTag save(CompoundTag compound) {
		CompoundTag ret = new CompoundTag();
		writeShulkerContents(ret);
		return ret;
	}

	private void writeShulkerContents(CompoundTag ret) {
		ListTag shulkerBoxContentsNbt = new ListTag();
		for (Map.Entry<UUID, CompoundTag> entry : shulkerContents.entrySet()) {
			CompoundTag uuidContentsPair = new CompoundTag();
			uuidContentsPair.put("uuid", NbtUtils.createUUID(entry.getKey()));
			uuidContentsPair.put("contents", entry.getValue());
			shulkerBoxContentsNbt.add(uuidContentsPair);
		}
		ret.put("shulkerBoxContents", shulkerBoxContentsNbt);
	}

	public CompoundTag getOrCreateShulkerBoxContents(UUID shulkerBoxUuid) {
		return shulkerContents.computeIfAbsent(shulkerBoxUuid, uuid -> {
			setDirty();
			return new CompoundTag();
		});
	}

	public void removeShulkerBoxContents(UUID shulkerBoxUuid) {
		shulkerContents.remove(shulkerBoxUuid);
		setDirty();
	}

	public void setShulkerBoxContents(UUID shulkerBoxUuid, CompoundTag contents) {
		shulkerContents.put(shulkerBoxUuid, contents);
		setDirty();
	}
}
