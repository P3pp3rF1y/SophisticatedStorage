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
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
//TODO after 1.22 remove support for legacy UUID deserialization via strings
public class ItemContentsStorage extends SavedData {
	private static final SavedDataType<ItemContentsStorage> TYPE = new SavedDataType<>(SophisticatedStorage.MOD_ID, ItemContentsStorage::new,
			RecordCodecBuilder.create(
					builder -> builder.group(
							Codec.unboundedMap(CodecHelper.STRING_ENCODED_UUID, ContainerContents.CODEC)
									.fieldOf("storageContents").forGetter(storage -> storage.storageContents),
							Codec.unboundedMap(CodecHelper.STRING_ENCODED_UUID, CompoundTag.CODEC)
									.fieldOf("additionalBeData").forGetter(storage -> storage.additionalBeData)
					).apply(builder, ItemContentsStorage::new)
			));

	private final Map<UUID, ContainerContents> storageContents = new HashMap<>();
	private final Map<UUID, CompoundTag> additionalBeData = new HashMap<>();
	private static final ItemContentsStorage clientStorageCopy = new ItemContentsStorage();

	private ItemContentsStorage(Map<UUID, ContainerContents> storageContents, Map<UUID, CompoundTag> additionalBeData) {
		this.storageContents.putAll(storageContents);
		this.additionalBeData.putAll(additionalBeData);
	}

	private ItemContentsStorage() {
	}

	public static ItemContentsStorage get() {
		if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				//noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(TYPE);
			}
		}
		return clientStorageCopy;
	}

	public boolean has(UUID storageUuid) {
		return additionalBeData.containsKey(storageUuid);
	}

	public CompoundTag getOrCreateAddtionalBeData(UUID storageUuid) {
		return additionalBeData.computeIfAbsent(storageUuid, uuid -> {
			setDirty();
			return new CompoundTag();
		});
	}

	public void removeAddtionalBeData(UUID storageUuid) {
		additionalBeData.remove(storageUuid);
		setDirty();
	}

	public void setAdditionalBeData(UUID storageUuid, CompoundTag contents) {
		additionalBeData.put(storageUuid, contents);
		setDirty();
	}

	public ContainerContents getOrCreateContents(UUID storageUuid) {
		return storageContents.computeIfAbsent(storageUuid, uuid -> {
			setDirty();
			return new ContainerContents();
		});
	}

	public void removeContents(UUID storageUuid) {
		storageContents.remove(storageUuid);
		setDirty();
	}

	public void setContents(UUID storageUuid, ContainerContents contents) {
		storageContents.put(storageUuid, contents);
		setDirty();
	}
}
