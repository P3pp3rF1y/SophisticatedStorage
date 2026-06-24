package net.p3pp3rf1y.sophisticatedstorage.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.properties.WoodType;

public class StreamCodecs {
	public static final StreamCodec<FriendlyByteBuf, WoodType> WOOD_TYPE_STREAM_CODEC = StreamCodec.of((buf, wt) -> buf.writeUtf(wt.name()), buf -> {
		WoodType woodType = WoodType.TYPES.get(buf.readUtf());
		return woodType == null ? WoodType.OAK : woodType;
	});

	private StreamCodecs() {
		// Utility class, no instantiation
	}
}
