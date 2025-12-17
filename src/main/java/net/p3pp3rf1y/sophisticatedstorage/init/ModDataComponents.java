package net.p3pp3rf1y.sophisticatedstorage.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterAttributes;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;
import net.p3pp3rf1y.sophisticatedstorage.util.StreamCodecs;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ModDataComponents {
	private ModDataComponents() {
	}

	private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, SophisticatedStorage.MOD_ID);
	public static final Supplier<DataComponentType<StorageToolItem.Mode>> TOOL_MODE = DATA_COMPONENT_TYPES.register("tool_mode",
			() -> new DataComponentType.Builder<StorageToolItem.Mode>().persistent(StorageToolItem.Mode.CODEC).networkSynchronized(StorageToolItem.Mode.STREAM_CODEC).build());

	public static final Supplier<DataComponentType<BlockPos>> CONTROLLER_POS = DATA_COMPONENT_TYPES.register("controller_pos",
			() -> new DataComponentType.Builder<BlockPos>().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build());

	public static final Supplier<DataComponentType<Boolean>> FLAT_TOP = DATA_COMPONENT_TYPES.register("flat_top",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<Map<BarrelMaterial, Identifier>>> BARREL_MATERIALS = DATA_COMPONENT_TYPES.register("barrel_materials",
			() -> new DataComponentType.Builder<Map<BarrelMaterial, Identifier>>().persistent(BarrelBlockItem.MATERIALS_CODEC).networkSynchronized(BarrelBlockItem.MATERIALS_STREAM_CODEC).build());

	public static final Supplier<DataComponentType<Map<Identifier, Integer>>> REMAINING_PARTS = DATA_COMPONENT_TYPES.register("remaining_parts",
			() -> new DataComponentType.Builder<Map<Identifier, Integer>>().persistent(PaintbrushItem.REMAINING_PARTS_CODEC).networkSynchronized(PaintbrushItem.REMAINING_PARTS_STREAM_CODEC).build());

	public static final Supplier<DataComponentType<Boolean>> DOUBLE_CHEST = DATA_COMPONENT_TYPES.register("double_chest",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<Boolean>> TIER_VISIBLE = DATA_COMPONENT_TYPES.register("tier_visible",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<Boolean>> LOCK_VISIBLE = DATA_COMPONENT_TYPES.register("lock_visible",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<Boolean>> UPGRADES_VISIBLE = DATA_COMPONENT_TYPES.register("upgrades_visible",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<Boolean>> COUNTS_VISIBLE = DATA_COMPONENT_TYPES.register("counts_visible",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<Boolean>> FILL_LEVELS_VISIBLE = DATA_COMPONENT_TYPES.register("fill_levels_visible",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<Boolean>> PACKED = DATA_COMPONENT_TYPES.register("packed",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<WoodType>> WOOD_TYPE = DATA_COMPONENT_TYPES.register("wood_type",
			() -> new DataComponentType.Builder<WoodType>().persistent(WoodType.CODEC).networkSynchronized(StreamCodecs.WOOD_TYPE_STREAM_CODEC).build());

	public static final Supplier<DataComponentType<Boolean>> LOCKED = DATA_COMPONENT_TYPES.register("locked",
			() -> new DataComponentType.Builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	public static final Supplier<DataComponentType<Map<Integer, DyeColor>>> SLOT_COLORS = DATA_COMPONENT_TYPES.register("slot_colors",
			() -> new DataComponentType.Builder<Map<Integer, DyeColor>>().persistent(LimitedBarrelBlockEntity.SLOT_COLORS_CODEC).networkSynchronized(LimitedBarrelBlockEntity.SLOT_COLORS_STREAM_CODEC).build());

	public static final Supplier<DataComponentType<Integer>> FIRST_INVENTORY_SLOT = DATA_COMPONENT_TYPES.register("first_inventory_slot",
			() -> new DataComponentType.Builder<Integer>().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());

	private static final Codec<Set<Direction>> DIRECTION_SET_CODEC = CodecHelper.setOf(Direction.CODEC);

	private static final StreamCodec<FriendlyByteBuf, Set<Direction>> DIRECTION_SET_STREAM_CODEC = new StreamCodec<>() {
		@Override
		public Set<Direction> decode(FriendlyByteBuf buf) {
			return buf.readCollection(HashSet::new, b -> b.readEnum(Direction.class));
		}

		@Override
		public void encode(FriendlyByteBuf buf, Set<Direction> directions) {
			buf.writeCollection(directions, FriendlyByteBuf::writeEnum);
		}
	};

	public static final Supplier<DataComponentType<Set<Direction>>> PULL_DIRECTIONS = DATA_COMPONENT_TYPES.register("pull_directions",
			() -> new DataComponentType.Builder<Set<Direction>>().persistent(DIRECTION_SET_CODEC).networkSynchronized(DIRECTION_SET_STREAM_CODEC).build());

	public static final Supplier<DataComponentType<Set<Direction>>> PUSH_DIRECTIONS = DATA_COMPONENT_TYPES.register("push_directions",
			() -> new DataComponentType.Builder<Set<Direction>>().persistent(DIRECTION_SET_CODEC).networkSynchronized(DIRECTION_SET_STREAM_CODEC).build());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterAttributes>> OUTPUT_FILTER_ATTRIBUTES = DATA_COMPONENT_TYPES.register("output_filter_attributes",
			() -> new DataComponentType.Builder<FilterAttributes>().persistent(FilterAttributes.CODEC).networkSynchronized(FilterAttributes.STREAM_CODEC).build());

	public static void register(IEventBus modBus) {
		DATA_COMPONENT_TYPES.register(modBus);
	}

}
