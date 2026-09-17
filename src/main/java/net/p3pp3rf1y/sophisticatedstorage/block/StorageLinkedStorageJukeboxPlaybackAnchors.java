package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxPlaybackLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StorageLinkedStorageJukeboxPlaybackAnchors {
	private static final Map<UUID, BlockAnchor> ANCHORS = new HashMap<>();

	private StorageLinkedStorageJukeboxPlaybackAnchors() {
	}

	public static void refreshBlockAnchor(ServerLevel level, StorageBlockEntity storage) {
		LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
		if (endpoint != null && LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
			ANCHORS.put(endpoint.groupId(), new BlockAnchor(level.dimension(), storage.getBlockPos(), endpoint.endpointId()));
		}
	}

	public static void removeBlockAnchor(ServerLevel level, StorageBlockEntity storage) {
		LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
		if (endpoint != null) {
			ANCHORS.computeIfPresent(endpoint.groupId(), (groupId, anchor) -> anchor.dimension.equals(level.dimension())
					&& anchor.pos.equals(storage.getBlockPos()) && anchor.endpointId.equals(endpoint.endpointId()) ? null : anchor);
		}
	}

	public static Optional<JukeboxPlaybackLocation> getPlaybackLocation(ServerLevel initiatingLevel, UUID groupId) {
		BlockAnchor anchor = ANCHORS.get(groupId);
		if (anchor == null) {
			return Optional.empty();
		}

		Optional<JukeboxPlaybackLocation> location = anchor.resolve(initiatingLevel.getServer(), groupId);
		if (location.isEmpty()) {
			ANCHORS.remove(groupId, anchor);
		}
		return location;
	}

	private record BlockAnchor(ResourceKey<Level> dimension, BlockPos pos, UUID endpointId) {
		private Optional<JukeboxPlaybackLocation> resolve(MinecraftServer server, UUID groupId) {
			ServerLevel level = server.getLevel(dimension);
			if (level == null || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof StorageBlockEntity storage)) {
				return Optional.empty();
			}

			LinkedStorageEndpointData endpoint = storage.getLinkedStorageEndpointData();
			return endpoint != null && endpoint.groupId().equals(groupId) && endpoint.endpointId().equals(endpointId)
					&& LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(groupId, endpointId)
							? Optional.of(JukeboxPlaybackLocation.forBlock(level, pos))
							: Optional.empty();
		}
	}
}
