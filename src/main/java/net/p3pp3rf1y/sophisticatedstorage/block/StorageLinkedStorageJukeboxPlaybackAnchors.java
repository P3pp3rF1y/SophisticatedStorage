package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ILinkedStorageBlockEndpoint;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointData;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageGroupsSavedData;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxPlaybackLocation;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StorageLinkedStorageJukeboxPlaybackAnchors {
	private static final int ENTITY_ANCHOR_GRACE_TICKS = 20;
	private static final Map<UUID, PlaybackAnchor> ANCHORS = new HashMap<>();

	private StorageLinkedStorageJukeboxPlaybackAnchors() {
	}

	public static void refreshEntityAnchor(ServerLevel level, Entity entity, ILinkedStorageBlockEndpoint holder) {
		LinkedStorageEndpointData endpoint = holder.getLinkedStorageEndpointData();
		if (entity.isAlive() && endpoint != null
				&& LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
			ANCHORS.put(endpoint.groupId(), new EntityAnchor(level.dimension(), new WeakReference<>(entity), new WeakReference<>(holder), endpoint.endpointId(),
					level.getServer().getTickCount()));
		}
	}

	public static void refreshBlockAnchor(ServerLevel level, StorageBlockEntity storageBlockEntity) {
		LinkedStorageEndpointData endpoint = storageBlockEntity.getLinkedStorageEndpointData();
		if (endpoint != null && LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(endpoint.groupId(), endpoint.endpointId())) {
			ANCHORS.put(endpoint.groupId(), new BlockAnchor(level.dimension(), storageBlockEntity.getBlockPos(), endpoint.endpointId()));
		}
	}

	public static Optional<JukeboxPlaybackLocation> getPlaybackLocation(ServerLevel initiatingLevel, UUID groupId) {
		PlaybackAnchor anchor = ANCHORS.get(groupId);
		if (anchor == null) {
			return Optional.empty();
		}

		Optional<JukeboxPlaybackLocation> location = anchor.resolve(initiatingLevel.getServer(), groupId);
		if (location.isEmpty()) {
			ANCHORS.remove(groupId, anchor);
		}
		return location;
	}

	private interface PlaybackAnchor {
		Optional<JukeboxPlaybackLocation> resolve(MinecraftServer server, UUID groupId);
	}

	private record BlockAnchor(ResourceKey<Level> dimension, BlockPos pos, UUID endpointId) implements PlaybackAnchor {
		@Override
		public Optional<JukeboxPlaybackLocation> resolve(MinecraftServer server, UUID groupId) {
			ServerLevel level = server.getLevel(dimension);
			if (level == null || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof StorageBlockEntity storageBlockEntity)) {
				return Optional.empty();
			}

			LinkedStorageEndpointData endpoint = storageBlockEntity.getLinkedStorageEndpointData();
			return endpoint != null && endpoint.groupId().equals(groupId) && endpoint.endpointId().equals(endpointId)
					&& LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(groupId, endpointId)
							? Optional.of(JukeboxPlaybackLocation.forBlock(level, pos))
							: Optional.empty();
		}
	}

	private record EntityAnchor(ResourceKey<Level> dimension, WeakReference<Entity> entity, WeakReference<ILinkedStorageBlockEndpoint> holder, UUID endpointId,
			int lastSeenTick) implements PlaybackAnchor {
		@Override
		public Optional<JukeboxPlaybackLocation> resolve(MinecraftServer server, UUID groupId) {
			if (lastSeenTick < server.getTickCount() - ENTITY_ANCHOR_GRACE_TICKS) {
				return Optional.empty();
			}
			ServerLevel level = server.getLevel(dimension);
			Entity source = entity.get();
			ILinkedStorageBlockEndpoint endpointHolder = holder.get();
			if (level == null || source == null || !source.isAlive() || source.level() != level || endpointHolder == null
					|| !endpointHolder.isLinkedStorageLinkCandidate()) {
				return Optional.empty();
			}
			LinkedStorageEndpointData endpoint = endpointHolder.getLinkedStorageEndpointData();
			return endpoint != null && endpoint.groupId().equals(groupId) && endpoint.endpointId().equals(endpointId)
					&& LinkedStorageGroupsSavedData.get(level).manager().isPrimaryEndpoint(groupId, endpointId)
							? Optional.of(JukeboxPlaybackLocation.forEntity(source))
							: Optional.empty();
		}
	}
}
