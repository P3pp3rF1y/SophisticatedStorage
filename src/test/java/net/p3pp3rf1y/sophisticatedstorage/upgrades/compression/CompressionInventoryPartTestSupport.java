package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedcore.inventory.ISlotTracker;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.util.MathHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

abstract class CompressionInventoryPartTestSupport {
	private static MockedStatic<RecipeHelper> recipeHelperMock;
	private static MockedStatic<SophisticatedStorage> storageMock;

	@BeforeAll
	static void setupRecipes() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		for (Item item : new Item[]{Items.IRON_BLOCK, Items.IRON_INGOT, Items.IRON_NUGGET, Items.QUARTZ, Items.QUARTZ_BLOCK, Items.STICK, Items.DEAD_BUSH,
				Items.COAL, Items.CHARCOAL, Items.DIAMOND, Items.EMERALD, Items.REDSTONE, Items.LAPIS_LAZULI, Items.AMETHYST_SHARD, Items.FLINT,
				Items.CLAY_BALL}) {
			item.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
		}
		recipeHelperMock = mockStatic(RecipeHelper.class);
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.IRON_NUGGET), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)))
				.thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.IRON_INGOT), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.IRON_INGOT), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)))
				.thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.IRON_BLOCK), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(any(ItemStack.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.IRON_NUGGET)))
				.thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.IRON_INGOT)))
				.thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(any(ItemStack.class))).thenReturn(RecipeHelper.UncompactingResult.EMPTY);
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.IRON_BLOCK)))
				.thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.IRON_INGOT), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.IRON_INGOT)))
				.thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.IRON_NUGGET), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.QUARTZ), eq(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE)))
				.thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.QUARTZ_BLOCK), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.QUARTZ_BLOCK), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)))
				.thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.STICK), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.STICK), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)))
				.thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.DEAD_BUSH), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.QUARTZ)))
				.thenReturn(Set.of(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.QUARTZ_BLOCK)))
				.thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.STICK)))
				.thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.DEAD_BUSH)))
				.thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.STICK), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.STICK)))
				.thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.QUARTZ_BLOCK), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.QUARTZ_BLOCK)))
				.thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.QUARTZ), RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE));
		storageMock = mockStatic(SophisticatedStorage.class);
		storageMock.when(() -> SophisticatedStorage.getIdentifier(anyString())).thenAnswer(i -> Identifier.parse(i.getArgument(0)));
	}

	private static ItemStack stackOf(Item item) {
		return argThat(stack -> stack.getItem() == item);
	}

	@AfterAll
	static void tearDownRecipes() {
		recipeHelperMock.close();
		storageMock.close();
	}

	static InventoryHandler getFilledInventoryHandler(Map<Integer, ItemStack> slotStacks, int baseSlotLimit) {
		InventoryHandler inventory = mock(InventoryHandler.class);
		ISlotTracker tracker = mock(ISlotTracker.class);
		when(inventory.getSlotTracker()).thenReturn(tracker);
		when(inventory.getBaseCapacity(any(ItemResource.class))).thenAnswer(i -> {
			ItemResource resource = i.getArgument(0);
			int maxStackSize = resource.isEmpty() ? baseSlotLimit : resource.getMaxStackSize();
			int limit = MathHelper.intMaxCappedMultiply(maxStackSize, baseSlotLimit / 64);
			int remainder = baseSlotLimit % 64;
			return remainder > 0 ? MathHelper.intMaxCappedAddition(limit, remainder * maxStackSize / 64) : limit;
		});
		when(inventory.getBaseSlotLimit()).thenReturn(baseSlotLimit);
		Map<Integer, ItemStack> internal = new HashMap<>();
		doAnswer(i -> {
			internal.put(i.getArgument(0), i.getArgument(1));
			return null;
		}).when(inventory).setStackInSlotInternal(anyInt(), any(ItemStack.class));
		doAnswer(i -> {
			Map<Integer, ItemStack> updates = i.getArgument(0);
			Set<Integer> affected = i.getArgument(1);
			updates.forEach(inventory::setStackInSlotInternal);
			for (int slot : affected) {
				tracker.removeAndSetSlotIndexes(inventory, slot, inventory.getStackInSlot(slot));
				inventory.triggerOnChangeListeners(slot);
			}
			return null;
		}).when(inventory).setSlotStacks(anyMap(), anySet());
		doAnswer(i -> {
			Map<Integer, ItemStack> restored = i.getArgument(0);
			restored.forEach(internal::put);
			for (int slot : restored.keySet()) {
				tracker.removeAndSetSlotIndexes(inventory, slot, inventory.getStackInSlot(slot));
			}
			return null;
		}).when(inventory).restoreSlotStacks(anyMap());
		when(inventory.getInternalStack(anyInt()))
				.thenAnswer(i -> internal.getOrDefault(i.getArgument(0), slotStacks.getOrDefault(i.getArgument(0), ItemStack.EMPTY)));
		return inventory;
	}

	static MemorySettingsCategory getMemorySettings(InventoryHandler inventory, Map<Integer, ItemStack> filters) {
		MemorySettingsCategory memory = spy(new MemorySettingsCategory(() -> inventory, new MemorySettingsCategoryData(Map.of(), Map.of(), false), () -> {
		}));
		when(memory.getSlotFilterStack(anyInt(), anyBoolean())).thenAnswer(i -> Optional.ofNullable(filters.get((int) i.getArgument(0))));
		return memory;
	}

	static CompressionInventoryPart initCompressionInventoryPart(InventoryHandler inventory, SlotRange range, Supplier<MemorySettingsCategory> memory) {
		CompressionInventoryPart part = new CompressionInventoryPart(inventory, range, memory) {
			@Override
			Optional<CompressionUpgradeConfig.DecompressionResult> getDecompressionResultFromConfig(Item item) {
				return Optional.empty();
			}

			@Override
			Optional<CompressionUpgradeConfig.CompressionResult> getCompressionResultFromConfig(ItemStack stack) {
				return Optional.empty();
			}
		};
		when(inventory.getStackInSlot(anyInt())).thenAnswer(i -> part.getStackInSlot(i.getArgument(0), inventory::getInternalStack));
		part.onInit();
		return part;
	}

	static void assertIronContents(InventoryHandler inventory, CompressionInventoryPart part, int firstSlot, int limit, long total) {
		long remaining = total;
		int calculated = 0;
		for (int index = 0; index < 3; index++) {
			int weight = index == 0 ? 81 : index == 1 ? 9 : 1;
			int expected = (int) Math.min(limit, remaining / weight);
			remaining -= (long) expected * weight;
			assertEquals(expected, inventory.getInternalStack(firstSlot + index).getCount(), "Internal slot " + index + ", total " + total);
			calculated = calculated * (index == 0 ? 1 : 9) + expected;
			assertEquals(calculated, part.getStackInSlot(firstSlot + index, inventory::getInternalStack).getCount(), "Calculated slot " + index);
		}
		assertEquals(0, remaining);
	}
}
