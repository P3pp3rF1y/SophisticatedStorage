package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemContentsStorage extends SavedData {
	private static final SavedDataType<ItemContentsStorage> TYPE = new SavedDataType<>(SophisticatedStorage.MOD_ID, ItemContentsStorage::new,
			RecordCodecBuilder.create(builder -> builder.group(Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), CompoundTag.CODEC)
					.fieldOf("storageContents").forGetter(storage -> storage.storageContents)).apply(builder, ItemContentsStorage::new)));

	private final Map<UUID, CompoundTag> storageContents = new HashMap<>();
	private static final ItemContentsStorage clientStorageCopy = new ItemContentsStorage();

	private ItemContentsStorage(Map<UUID, CompoundTag> storageContents) {
		this.storageContents.putAll(storageContents);
	}

	private ItemContentsStorage() {
	}

	public static ItemContentsStorage get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				// noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(TYPE);
			}
		}
		return clientStorageCopy;
	}

	public boolean has(UUID storageUuid) {
		return storageContents.containsKey(storageUuid);
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
