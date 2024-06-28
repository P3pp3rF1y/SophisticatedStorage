package net.p3pp3rf1y.sophisticatedstorage.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageToolItem;

import java.util.Map;
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

	public static final Supplier<Map<BarrelMaterial, ResourceLocation>> BARREL_MATERIALS = DATA_COMPONENT_TYPES.register("barrel_materials",
			() -> new DataComponentType.Builder<Map<BarrelMaterial, ResourceLocation>>().persistent(BarrelMaterial.MAP_CODEC).networkSynchronized(BarrelMaterial.MAP_STREAM_CODEC).build());
	public static void register(IEventBus modBus) {
		DATA_COMPONENT_TYPES.register(modBus);
	}

}
