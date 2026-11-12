package net.p3pp3rf1y.sophisticatedstorage.block;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

final class StorageTankTestBootstrap {
	private static boolean registered;
	private static final AtomicInteger NEXT_TEST_ITEM = new AtomicInteger();
	private StorageTankTestBootstrap() {
	}

	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		// CapabilityToken is transformed by Forge only in a game runtime.
		try (MockedStatic<CapabilityManager> manager = mockStatic(CapabilityManager.class)) {
			manager.when(() -> CapabilityManager.get(any(CapabilityToken.class))).thenAnswer(ignored -> createCapability());
			assertNotNull(ForgeCapabilities.ITEM_HANDLER);
			assertNotNull(ForgeCapabilities.FLUID_HANDLER);
			assertNotNull(ForgeCapabilities.FLUID_HANDLER_ITEM);
		}
		CommentedConfig config = CommentedConfig.inMemory();
		Config.SERVER_SPEC.correct(config);
		Config.SERVER_SPEC.acceptConfig(config);
		registerTestItems();
		initializeStorageSettingsHandler();
	}

	static Capability<?> createCapability() throws ReflectiveOperationException {
		Constructor<Capability> constructor = Capability.class.getDeclaredConstructor(String.class);
		constructor.setAccessible(true);
		return constructor.newInstance("test_capability");
	}

	private static void initializeStorageSettingsHandler() {
		// Forge adds event constructors during transformation; a plain JVM does not have Clone's no-arg constructor.
		try (MockedStatic<EventListenerHelper> listeners = mockStatic(EventListenerHelper.class)) {
			listeners.when(() -> EventListenerHelper.getListenerList(PlayerEvent.Clone.class)).thenReturn(new ListenerList());
			Class.forName(StorageSettingsHandler.class.getName(), true, StorageSettingsHandler.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Unable to initialize Storage settings test listener", e);
		}
	}

	private static synchronized void registerTestItems() {
		if (registered) {
			return;
		}
		GameData.unfreezeData();
		try {
			unfreeze(ForgeRegistries.ITEMS);
			for (Item item : BuiltInRegistries.ITEM) {
				ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
				if (!ForgeRegistries.ITEMS.containsKey(id)) {
					ForgeRegistries.ITEMS.register(id, item);
				}
			}
			unfreeze(ForgeRegistries.FLUIDS);
			for (Fluid fluid : BuiltInRegistries.FLUID) {
				ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
				if (!ForgeRegistries.FLUIDS.containsKey(id)) {
					ForgeRegistries.FLUIDS.register(id, fluid);
				}
			}
			Field fluidTypeValue = RegistryObject.class.getDeclaredField("value");
			fluidTypeValue.setAccessible(true);
			fluidTypeValue.set(ModFluids.XP_FLUID_TYPE, new FluidType(FluidType.Properties.create()));
			registerDeferred(ModFluids.FLUIDS, ForgeRegistries.FLUIDS);
			registerDeferred(ModFluids.ITEMS, ForgeRegistries.ITEMS);
			registerDeferred(ModBlocks.BLOCKS, ForgeRegistries.BLOCKS);
			registerDeferred(ModBlocks.ITEMS, ForgeRegistries.ITEMS);
			registerDeferred(ModItems.ITEMS, ForgeRegistries.ITEMS);
			registerDeferredField(ModBlocks.class, "BLOCK_ENTITY_TYPES", ForgeRegistries.BLOCK_ENTITY_TYPES);
			registered = true;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to initialize Storage tank test registries", e);
		}
	}

	static void registerTestItem(Item item) {
		ResourceLocation id = new ResourceLocation("sophisticatedstorage", "test_addon_" + NEXT_TEST_ITEM.incrementAndGet());
		ForgeRegistries.ITEMS.register(id, item);
	}

	@SuppressWarnings("unchecked")
	private static <T> void registerDeferredField(Class<?> owner, String name, IForgeRegistry<T> registry) throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		registerDeferred((DeferredRegister<T>) field.get(null), registry);
	}

	private static void unfreeze(IForgeRegistry<?> registry) throws ReflectiveOperationException {
		Method method = registry.getClass().getDeclaredMethod("unfreeze");
		method.setAccessible(true);
		method.invoke(registry);
	}

	@SuppressWarnings("unchecked")
	private static <T> void registerDeferred(DeferredRegister<T> deferred, IForgeRegistry<T> registry) throws ReflectiveOperationException {
		unfreeze(registry);
		Field entriesField = DeferredRegister.class.getDeclaredField("entries");
		entriesField.setAccessible(true);
		Map<RegistryObject<T>, Supplier<? extends T>> entries = (Map<RegistryObject<T>, Supplier<? extends T>>) entriesField.get(deferred);
		for (Map.Entry<RegistryObject<T>, Supplier<? extends T>> entry : entries.entrySet()) {
			RegistryObject<T> object = entry.getKey();
			T value = registry.containsKey(object.getId()) ? registry.getValue(object.getId()) : entry.getValue().get();
			if (!registry.containsKey(object.getId())) {
				registry.register(object.getId(), value);
			}
			if (registry == ForgeRegistries.BLOCKS && !BuiltInRegistries.BLOCK.containsKey(object.getId())) {
				Registry.register(BuiltInRegistries.BLOCK, object.getId(), (Block) value);
			} else if (registry == ForgeRegistries.ITEMS && !BuiltInRegistries.ITEM.containsKey(object.getId())) {
				Registry.register(BuiltInRegistries.ITEM, object.getId(), (Item) value);
			} else if (registry == ForgeRegistries.FLUIDS && !BuiltInRegistries.FLUID.containsKey(object.getId())) {
				Registry.register(BuiltInRegistries.FLUID, object.getId(), (Fluid) value);
			}
			Field valueField = RegistryObject.class.getDeclaredField("value");
			valueField.setAccessible(true);
			valueField.set(object, value);
			Field holderField = RegistryObject.class.getDeclaredField("holder");
			holderField.setAccessible(true);
			holderField.set(object, registry.getHolder(object.getId()).orElse(null));
		}
	}

	static void clearConfig() {
		Config.SERVER_SPEC.acceptConfig(null);
	}
}
