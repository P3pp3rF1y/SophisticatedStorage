package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.event.RegistryEvent;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.data.CopyStorageDataFunction;

public class ModLoot {
	private ModLoot() {}

	public static final LootItemFunctionType COPY_STORAGE_DATA = new LootItemFunctionType(new CopyStorageDataFunction.Serializer());

	public static void registerLootFunction(RegistryEvent<Block> event) {
		Registry.register(Registry.LOOT_FUNCTION_TYPE, new ResourceLocation(SophisticatedStorage.MOD_ID, "copy_storage_data"), COPY_STORAGE_DATA);
	}
}
