package net.p3pp3rf1y.sophisticatedstorage.init;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.registries.RegisterEvent;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.data.CopyStorageDataFunction;

public class ModLoot {
	private ModLoot() {}

	public static final LootItemFunctionType COPY_STORAGE_DATA = new LootItemFunctionType(new CopyStorageDataFunction.Serializer());

	public static void registerLootFunction(RegisterEvent event) {
		if (!event.getRegistryKey().equals(Registry.LOOT_FUNCTION_REGISTRY)) {
			return;
		}
		Registry.register(Registry.LOOT_FUNCTION_TYPE, new ResourceLocation(SophisticatedStorage.MOD_ID, "copy_storage_data"), COPY_STORAGE_DATA);
	}
}
