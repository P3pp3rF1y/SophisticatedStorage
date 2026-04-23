package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategoryData;
import net.p3pp3rf1y.sophisticatedcore.util.MathHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CompressionInventoryPartTest {
	private static MockedStatic<RecipeHelper> recipeHelperMock;
	private static MockedStatic<SophisticatedStorage> ss;

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		recipeHelperMock = Mockito.mockStatic(RecipeHelper.class);

		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.IRON_NUGGET), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))).thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.IRON_INGOT), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.IRON_INGOT), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))).thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.IRON_BLOCK), Collections.emptyList()));

		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(any(ItemStack.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.IRON_NUGGET))).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.IRON_INGOT))).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));

		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(any(ItemStack.class))).thenReturn(RecipeHelper.UncompactingResult.EMPTY);
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.IRON_BLOCK))).thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.IRON_INGOT), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.IRON_INGOT))).thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.IRON_NUGGET), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));

		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.REDSTONE), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))).thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.REDSTONE_BLOCK), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.REDSTONE_BLOCK))).thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.REDSTONE), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.REDSTONE))).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));

		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.QUARTZ), eq(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE))).thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.QUARTZ_BLOCK), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.QUARTZ_BLOCK), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))).thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.STICK), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(stackOf(Items.STICK), eq(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))).thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.DEAD_BUSH), Collections.emptyList()));

		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.QUARTZ))).thenReturn(Set.of(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.QUARTZ_BLOCK))).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(stackOf(Items.STICK))).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));

		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.DEAD_BUSH))).thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.STICK), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.STICK))).thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.QUARTZ_BLOCK), RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(stackOf(Items.QUARTZ_BLOCK))).thenReturn(new RecipeHelper.UncompactingResult(new ItemStack(Items.QUARTZ), RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE));

		ss = Mockito.mockStatic(SophisticatedStorage.class);
		ss.when(() -> SophisticatedStorage.getIdentifier(anyString())).thenAnswer(i -> Identifier.parse(i.getArgument(0)));
	}

	private static ItemStack stackOf(Item item) {
		return argThat(stack -> stack.getItem() == item);
	}

	@BeforeEach
	public void testSetup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	@AfterAll
	public static void tearDown() {
		recipeHelperMock.close();
		ss.close();
	}

	private InventoryHandler getFilledInventoryHandler(Map<Integer, ItemStack> slotStacks, int baseSlotLimit) {
		InventoryHandler inventoryHandler = Mockito.mock(InventoryHandler.class);
		when(inventoryHandler.getBaseCapacity(any(ItemResource.class))).thenAnswer(i -> {
			ItemResource resource = i.getArgument(0);
			int maxStackSize = resource.isEmpty() ? baseSlotLimit : resource.getMaxStackSize();
			int limit = MathHelper.intMaxCappedMultiply(maxStackSize, (baseSlotLimit / 64));
			int remainder = baseSlotLimit % 64;
			if (remainder > 0) {
				limit = MathHelper.intMaxCappedAddition(limit, remainder * maxStackSize / 64);
			}
			return limit;
		});
		when(inventoryHandler.getBaseSlotLimit()).thenReturn(baseSlotLimit);

		Map<Integer, ItemStack> internalStacks = new HashMap<>();

		doAnswer(i -> {
			internalStacks.put(i.getArgument(0), i.getArgument(1));
			return null;
		}).when(inventoryHandler).setStackInSlotInternal(anyInt(), any(ItemStack.class));

		when(inventoryHandler.getInternalStack(anyInt())).thenAnswer(i -> {
			int slot = i.getArgument(0);
			return internalStacks.containsKey(slot) ? internalStacks.get(slot) : slotStacks.get(slot);
		});

		when(inventoryHandler.getResource(anyInt())).thenAnswer(i -> {
			int slot = i.getArgument(0);
			ItemStack stack = internalStacks.containsKey(slot) ? internalStacks.get(slot) : slotStacks.get(slot);
			return ItemResource.of(stack);
		});

		when(inventoryHandler.getAmountAsInt(anyInt())).thenAnswer(i -> {
			int slot = i.getArgument(0);
			ItemStack stack = internalStacks.containsKey(slot) ? internalStacks.get(slot) : slotStacks.get(slot);
			return stack.getCount();
		});

		return inventoryHandler;
	}

	private static MemorySettingsCategory getMemorySettings(InventoryHandler invHandler, Map<Integer, ItemStack> slotFilterStacks) {
		MemorySettingsCategory memorySettingsCategory = Mockito.spy(new MemorySettingsCategory(() -> invHandler, new MemorySettingsCategoryData(Map.of(), Map.of(), false), () -> {
		}));
		when(memorySettingsCategory.getSlotFilterStack(anyInt(), anyBoolean())).thenAnswer(i -> Optional.ofNullable(slotFilterStacks.get((int) i.getArgument(0))));
		return memorySettingsCategory;
	}

	@ParameterizedTest
	@MethodSource("compactsStacksOnInitData")
	void compactsStacksOnInit(CompactsStacksOnInitParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.slotStacksInput, params.baseSlotLimit);
		int minSlot = Collections.min(params.slotStacksInput.keySet());

		initCompressionInventoryPart(params.slotStacksInput, invHandler, minSlot);

		assertInternalStacks(params.slotStacksModified, invHandler);
	}

	private CompressionInventoryPart initCompressionInventoryPart(Map<Integer, ItemStack> slotStacksInput, InventoryHandler invHandler, Supplier<MemorySettingsCategory> getMemorySettings) {
		return initCompressionInventoryPart(invHandler, new SlotRange(Collections.min(slotStacksInput.keySet()), Collections.min(slotStacksInput.keySet()) + slotStacksInput.size()), getMemorySettings);
	}

	private CompressionInventoryPart initCompressionInventoryPart(Map<Integer, ItemStack> slotStacksInput, InventoryHandler invHandler, int minSlot) {
		return initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + slotStacksInput.size()), () -> getMemorySettings(invHandler, Map.of()));
	}

	private CompressionInventoryPart initCompressionInventoryPart(InventoryHandler invHandler, SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings) {
		CompressionInventoryPart spiedPart = Mockito.mock(
				CompressionInventoryPart.class,
				Mockito.withSettings()
						.useConstructor(invHandler, slotRange, getMemorySettings)
						.defaultAnswer(Mockito.CALLS_REAL_METHODS)
		);

		doReturn(Optional.empty()).when(spiedPart).getDecompressionResultFromConfig(any(Item.class));
		spiedPart.onInit();
		return spiedPart;
	}

	private static void assertInternalStacks(Map<Integer, ItemStack> slotStacksExpected, InventoryHandler invHandler) {
		assertInternalStacks(slotStacksExpected, invHandler, true);
	}

	private static void assertInternalStacks(Map<Integer, ItemStack> slotStacksExpected, InventoryHandler invHandler, boolean verifyNumberOfCallsMatchesSize) {
		boolean matching = true;
		Map<Integer, ItemStack> updatedInternalStacks = new LinkedHashMap<>();

		ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<ItemStack> stackCaptor = ArgumentCaptor.forClass(ItemStack.class);
		if (verifyNumberOfCallsMatchesSize) {
			verify(invHandler, times(slotStacksExpected.size())).setStackInSlotInternal(intCaptor.capture(), stackCaptor.capture());
		}
		List<ItemStack> updatedStacks = stackCaptor.getAllValues();
		List<Integer> updatedSlots = intCaptor.getAllValues();
		for (int i = 0; i < updatedSlots.size(); i++) {
			ItemStack updatedStack = updatedStacks.get(i);
			updatedInternalStacks.put(i, updatedStack);
			if (!ItemStack.matches(slotStacksExpected.get(updatedSlots.get(i)), updatedStack)) {
				matching = false;
			}
		}

		if (!matching) {
			assertionFailure().message("Calculated stacks don't equal")
					.expected(slotStacksExpected)
					.actual(updatedInternalStacks)
					.buildAndThrow();
		}
	}

	public record CompactsStacksOnInitParams(
			Map<Integer, ItemStack> slotStacksInput,
			Map<Integer, ItemStack> slotStacksModified,
			int baseSlotLimit
	) {
	}

	public static List<CompactsStacksOnInitParams> compactsStacksOnInitData() {
		return List.of(
				new CompactsStacksOnInitParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 10), 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 1)),
						64
				),
				new CompactsStacksOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 20), 2, new ItemStack(Items.IRON_NUGGET, 40)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 6), 2, new ItemStack(Items.IRON_NUGGET, 4)),
						64
				),
				new CompactsStacksOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4000), 1, new ItemStack(Items.IRON_INGOT, 4000), 2, new ItemStack(Items.IRON_NUGGET, 4000)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4096), 1, new ItemStack(Items.IRON_INGOT, 3580), 2, new ItemStack(Items.IRON_NUGGET, 4)),
						64 * 64
				)
		);
	}

	@ParameterizedTest
	@MethodSource("calculatedStacksCorrectOnInitData")
	void calculatedStacksCorrectOnInit(CalculatedStacksCorrectOnInitParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.slotStacksInput, params.baseSlotLimit);
		int minSlot = Collections.min(params.slotStacksInput.keySet());

		Map<Integer, ItemStack> internalStacks = new HashMap<>();

		doAnswer(i -> {
			internalStacks.put(i.getArgument(0), i.getArgument(1));
			return null;
		}).when(invHandler).setStackInSlotInternal(anyInt(), any(ItemStack.class));

		when(invHandler.getInternalStack(anyInt())).thenAnswer(i -> {
			int slot = i.getArgument(0);
			return internalStacks.containsKey(slot) ? internalStacks.get(slot) : params.slotStacksInput.get(slot);
		});

		CompressionInventoryPart part = initCompressionInventoryPart(params.slotStacksInput, invHandler, minSlot);

		assertCalculatedStacks(params.calculatedStacks, minSlot, part);
	}

	public record CalculatedStacksCorrectOnInitParams(
			Map<Integer, ItemStack> slotStacksInput,
			Map<Integer, ItemStack> calculatedStacks,
			int baseSlotLimit
	) {
	}

	public static List<CalculatedStacksCorrectOnInitParams> calculatedStacksCorrectOnInitData() {
		return List.of(
				new CalculatedStacksCorrectOnInitParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 10), 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 10), 2, new ItemStack(Items.IRON_NUGGET, 90)),
						64
				),
				new CalculatedStacksCorrectOnInitParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.CLAY, 10), 2, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.CLAY, 10), 2, ItemStack.EMPTY),
						64
				),
				new CalculatedStacksCorrectOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 20), 2, new ItemStack(Items.IRON_NUGGET, 40)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 33), 2, new ItemStack(Items.IRON_NUGGET, 301)),
						64
				),
				new CalculatedStacksCorrectOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4000), 1, new ItemStack(Items.IRON_INGOT, 4000), 2, new ItemStack(Items.IRON_NUGGET, 4000)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4096), 1, new ItemStack(Items.IRON_INGOT, 40444), 2, new ItemStack(Items.IRON_NUGGET, 364000)),
						64 * 64
				),
				new CalculatedStacksCorrectOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4096), 1, new ItemStack(Items.IRON_INGOT, 4096), 2, new ItemStack(Items.IRON_NUGGET, 4096)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4096), 1, new ItemStack(Items.IRON_INGOT, 40960), 2, new ItemStack(Items.IRON_NUGGET, 372736)),
						64 * 64
				),
				new CalculatedStacksCorrectOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE - 64), 2, new ItemStack(Items.IRON_NUGGET, Integer.MAX_VALUE - 64)),
						64 * 64 * 64 * 64 * 64
				),
				new CalculatedStacksCorrectOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, 1073741824 - 48), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE - 48), 2, new ItemStack(Items.IRON_NUGGET, Integer.MAX_VALUE - 64)),
						64 * 64 * 64 * 64 * 64
				)
		);
	}

	@ParameterizedTest
	@MethodSource("extractUpdatesStacksData")
	void extractUpdatesStacks(ExtractUpdatesStacksParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.internalStacksBefore, params.baseSlotLimit);
		int minSlot = Collections.min(params.internalStacksBefore.keySet());

		CompressionInventoryPart part = initCompressionInventoryPart(params.internalStacksBefore, invHandler, minSlot);

		int result;
		try (Transaction tx = Transaction.openRoot()) {
			result = part.extract(params.extractSlot, ItemResource.of(params.extractStack), params.extractStack.getCount(), tx,
					(slot, resource, amount, transaction) -> invHandler.extract(slot, resource, amount, tx)
			);
			tx.commit();
		}

		assertEquals(params.extractResult, result, "Extract result doesn't match");
		assertCalculatedStacks(params.calculatedStacksAfter, minSlot, part);
		assertInternalStacks(params.internalStacksUpdated, invHandler);
	}

	public record ExtractUpdatesStacksParams(
			Map<Integer, ItemStack> internalStacksBefore,
			int baseSlotLimit,
			int extractSlot,
			ItemStack extractStack,
			int extractResult,
			Map<Integer, ItemStack> internalStacksUpdated,
			Map<Integer, ItemStack> calculatedStacksAfter
	) {
	}

	public static List<ExtractUpdatesStacksParams> extractUpdatesStacksData() {
		return List.of(
				new ExtractUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 1), 2, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.IRON_INGOT, 10),
						10,
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY)
				),
				new ExtractUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						1,
						new ItemStack(Items.IRON_INGOT, 10),
						10,
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 3)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 21), 2, new ItemStack(Items.IRON_NUGGET, 194))
				),
				new ExtractUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 61), 1, new ItemStack(Items.IRON_INGOT, 5), 2, new ItemStack(Items.IRON_NUGGET, 3)),
						64 * 8,
						0,
						new ItemStack(Items.IRON_BLOCK, 300),
						61,
						Map.of(0, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 5), 2, new ItemStack(Items.IRON_NUGGET, 48))
				),
				new ExtractUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						1,
						new ItemStack(Items.IRON_INGOT, 5),
						5,
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 26), 2, new ItemStack(Items.IRON_NUGGET, 239))
				),
				new ExtractUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 1),
						1,
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 8), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 26), 2, new ItemStack(Items.IRON_NUGGET, 242))
				),
				new ExtractUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						0,
						new ItemStack(Items.IRON_BLOCK, 4),
						3,
						Map.of(0, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 41))
				),
				new ExtractUpdatesStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						0,
						new ItemStack(Items.IRON_BLOCK, 4),
						0,
						Map.of(),
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY)
				),
				new ExtractUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT, 3), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.IRON_NUGGET, 14),
						14,
						Map.of(0, new ItemStack(Items.IRON_INGOT, 1), 1, new ItemStack(Items.IRON_NUGGET, 4)),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 1), 1, new ItemStack(Items.IRON_NUGGET, 13), 2, ItemStack.EMPTY)
				)
		);
	}

	private static void assertStackEquals(ItemStack expected, ItemStack actual, Object message) {
		if (!ItemStack.matches(expected, actual)) {
			assertionFailure().message(message)
					.expected(expected)
					.actual(actual)
					.buildAndThrow();
		}
	}

	private static void assertCalculatedStacks(Map<Integer, ItemStack> calculatedStacksAfter, int minSlot, CompressionInventoryPart part) {
		boolean matching = true;
		Map<Integer, ItemStack> actualCalculatedStacks = new LinkedHashMap<>();
		for (int slot = minSlot; slot < minSlot + calculatedStacksAfter.size(); slot++) {
			ItemStack calculatedStack = part.getStackInSlot(slot, s -> ItemStack.EMPTY);
			actualCalculatedStacks.put(slot, calculatedStack);

			if (!ItemStack.matches(calculatedStack, calculatedStacksAfter.get(slot))) {
				matching = false;
			}
		}
		if (!matching) {
			assertionFailure().message("Calculated stacks don't equal")
					.expected(calculatedStacksAfter)
					.actual(actualCalculatedStacks)
					.buildAndThrow();
		}
	}

	@ParameterizedTest
	@MethodSource("simulatedExtractDoesNotUpdateStacksData")
	void simulatedExtractDoesNotUpdateStacks(SimulatedExtractDoesNotUpdateStacksParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.internalStacksBefore, params.baseSlotLimit);
		int minSlot = Collections.min(params.internalStacksBefore.keySet());

		CompressionInventoryPart part = initCompressionInventoryPart(params.internalStacksBefore, invHandler, minSlot);

		int result;
		try (Transaction tx = Transaction.openRoot()) {
			result = part.extract(params.extractSlot, ItemResource.of(params.internalStacksBefore.get(params.extractSlot)), params.extractAmount, tx, (slot, resource, amount, transaction) -> invHandler.extract(slot, resource, amount, tx));
		}

		assertEquals(params.extractResult, result, "Extract result doesn't match");
		assertCalculatedStacks(params.calculatedStacksBefore, minSlot, part);
		assertInternalStacks(params.internalStacksBefore, invHandler, false);
	}

	public record SimulatedExtractDoesNotUpdateStacksParams(
			Map<Integer, ItemStack> internalStacksBefore,
			Map<Integer, ItemStack> calculatedStacksBefore,
			int baseSlotLimit,
			int extractSlot,
			int extractAmount,
			int extractResult
	) {
	}

	public static List<SimulatedExtractDoesNotUpdateStacksParams> simulatedExtractDoesNotUpdateStacksData() {
		return List.of(
				new SimulatedExtractDoesNotUpdateStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 1), 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 10), 2, new ItemStack(Items.IRON_NUGGET, 90)),
						64,
						1,
						10,
						10
				),
				new SimulatedExtractDoesNotUpdateStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 31), 2, new ItemStack(Items.IRON_NUGGET, 284)),
						64,
						1,
						10,
						10
				),
				new SimulatedExtractDoesNotUpdateStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 61), 1, new ItemStack(Items.IRON_INGOT, 5), 2, new ItemStack(Items.IRON_NUGGET, 3)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 61), 1, new ItemStack(Items.IRON_INGOT, 554), 2, new ItemStack(Items.IRON_NUGGET, 4989)),
						64 * 8,
						0,
						300,
						61
				)
		);
	}

	@ParameterizedTest
	@MethodSource("insertUpdatesStacksData")
	void insertUpdatesStacks(InsertItemUpdatesStacksParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.internalStacksBefore, params.baseSlotLimit);
		int minSlot = Collections.min(params.internalStacksBefore.keySet());

		CompressionInventoryPart part = initCompressionInventoryPart(params.internalStacksBefore, invHandler, minSlot);

		int result;
		try (Transaction tx = Transaction.openRoot()) {
			result = part.insert(params.insertSlot, ItemResource.of(params.stack), params.stack.getCount(), tx, (slot, resource, amount, transaction) -> params.stack.getCount());
			tx.commit();
		}

		assertEquals(params.insertResult, result, "Insert result doesn't match");
		assertCalculatedStacks(params.calculatedStacksAfter, minSlot, part);
		assertInternalStacks(params.changedInternalStacks, invHandler);
	}

	public record InsertItemUpdatesStacksParams(Map<Integer, ItemStack> internalStacksBefore, int baseSlotLimit,
												int insertSlot, ItemStack stack, int insertResult,
												Map<Integer, ItemStack> changedInternalStacks,
												Map<Integer, ItemStack> calculatedStacksAfter) {
	}

	public static List<InsertItemUpdatesStacksParams> insertUpdatesStacksData() {
		return List.of(new InsertItemUpdatesStacksParams(
						Map.of(2, ItemStack.EMPTY, 1, ItemStack.EMPTY, 0, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 100),
						100,
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 1), 1, new ItemStack(Items.IRON_INGOT, 2), 0, new ItemStack(Items.IRON_BLOCK, 1)),
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 100), 1, new ItemStack(Items.IRON_INGOT, 11), 0, new ItemStack(Items.IRON_BLOCK, 1))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(2, ItemStack.EMPTY, 1, ItemStack.EMPTY, 0, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.IRON_NUGGET, 100),
						100,
						Map.of(1, new ItemStack(Items.IRON_NUGGET, 1), 0, new ItemStack(Items.IRON_INGOT, 11)),
						Map.of(2, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_NUGGET, 100), 0, new ItemStack(Items.IRON_INGOT, 11))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 8), 1, new ItemStack(Items.IRON_INGOT, 8), 0, new ItemStack(Items.IRON_BLOCK, 63)),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 1000),
						641,
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 64), 1, new ItemStack(Items.IRON_INGOT, 64), 0, new ItemStack(Items.IRON_BLOCK, 64)),
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 5824), 1, new ItemStack(Items.IRON_INGOT, 640), 0, new ItemStack(Items.IRON_BLOCK, 64))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 5), 1, new ItemStack(Items.IRON_INGOT, 4), 0, new ItemStack(Items.IRON_BLOCK, 3)),
						64,
						1,
						new ItemStack(Items.IRON_INGOT, 32),
						32,
						Map.of(1, ItemStack.EMPTY, 0, new ItemStack(Items.IRON_BLOCK, 7)),
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 572), 1, new ItemStack(Items.IRON_INGOT, 63), 0, new ItemStack(Items.IRON_BLOCK, 7))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 8), 1, new ItemStack(Items.IRON_INGOT, 8), 0, new ItemStack(Items.IRON_BLOCK, 73741824)),
						64 * 64 * 64 * 64 * 64,
						0,
						new ItemStack(Items.IRON_BLOCK, 1_000_000_001),
						1_000_000_000,
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE - 64), 2, new ItemStack(Items.IRON_NUGGET, Integer.MAX_VALUE - 64))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, 73741824 - 48), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						64 * 64 * 64 * 64 * 64,
						1,
						new ItemStack(Items.IRON_INGOT, 1_000_000_000),
						1_000_000_000,
						Map.of(1, new ItemStack(Items.IRON_INGOT, 1073741824 - 48)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE - 48), 2, new ItemStack(Items.IRON_NUGGET, Integer.MAX_VALUE - 64))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT, 1), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.IRON_NUGGET, 9),
						9,
						Map.of(0, new ItemStack(Items.IRON_INGOT, 2)),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 2), 1, new ItemStack(Items.IRON_NUGGET, 18), 2, ItemStack.EMPTY)
				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 1),
						1,
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 9), 2, new ItemStack(Items.IRON_NUGGET, 82))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 7), 2, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 9),
						9,
						Map.of(1, new ItemStack(Items.IRON_INGOT, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 17), 2, new ItemStack(Items.IRON_NUGGET, 153))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT, 7), 1, ItemStack.EMPTY),
						64,
						0,
						new ItemStack(Items.IRON_INGOT, 64),
						57,
						Map.of(0, new ItemStack(Items.IRON_INGOT, 64)),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 64), 1, new ItemStack(Items.IRON_NUGGET, 576))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.STICK, 1), 2, new ItemStack(Items.QUARTZ_BLOCK, 1), 3, new ItemStack(Items.QUARTZ, 1)),
						64,
						0,
						new ItemStack(Items.DEAD_BUSH, 1),
						1,
						Map.of(0, new ItemStack(Items.DEAD_BUSH, 1)),
						Map.of(0, new ItemStack(Items.DEAD_BUSH, 1), 1, new ItemStack(Items.STICK, 10), 2, new ItemStack(Items.QUARTZ_BLOCK, 91), 3, new ItemStack(Items.QUARTZ, 365))
				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.STICK, 1), 2, new ItemStack(Items.QUARTZ_BLOCK, 1), 3, new ItemStack(Items.QUARTZ, 1)),
						64,
						0,
						new ItemStack(Items.DEAD_BUSH, 65),
						64,
						Map.of(0, new ItemStack(Items.DEAD_BUSH, 64)),
						Map.of(0, new ItemStack(Items.DEAD_BUSH, 64), 1, new ItemStack(Items.STICK, 577), 2, new ItemStack(Items.QUARTZ_BLOCK, 5194), 3, new ItemStack(Items.QUARTZ, 20777))

				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.DEAD_BUSH, 64), 1, new ItemStack(Items.STICK, 64), 2, new ItemStack(Items.QUARTZ_BLOCK, 64), 3, new ItemStack(Items.QUARTZ, 23)),
						64,
						3,
						new ItemStack(Items.QUARTZ, 64),
						41,
						Map.of(3, new ItemStack(Items.QUARTZ, 64)),
						Map.of(0, new ItemStack(Items.DEAD_BUSH, 64), 1, new ItemStack(Items.STICK, 640), 2, new ItemStack(Items.QUARTZ_BLOCK, 5824), 3, new ItemStack(Items.QUARTZ, 23360))

				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 8), 1, new ItemStack(Items.IRON_INGOT, 8), 2, new ItemStack(Items.IRON_NUGGET, 6), 3, ItemStack.EMPTY),
						8,
						2,
						new ItemStack(Items.IRON_NUGGET, 33),
						2,
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 8), 1, new ItemStack(Items.IRON_INGOT, 80), 2, new ItemStack(Items.IRON_NUGGET, 728), 3, ItemStack.EMPTY)
				),
				new InsertItemUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.REDSTONE_BLOCK), 1, new ItemStack(Items.REDSTONE), 2, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.REDSTONE),
						1,
						Map.of(1, new ItemStack(Items.REDSTONE, 2)),
						Map.of(0, new ItemStack(Items.REDSTONE_BLOCK), 1, new ItemStack(Items.REDSTONE, 11), 2, ItemStack.EMPTY)
				)
		);
	}

	@Test
	void insertNotMatchingStackReturnsBackWithoutChanging() {
		insertUpdatesStacks(
				new InsertItemUpdatesStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 1)),
						64, 2, new ItemStack(Items.GOLD_NUGGET, 100), 0,
						Map.of(), Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 19))
				)
		);
	}

	@Test
	void insertDecompressibleItemJustSetsItInSlotAndDoesntAffectOtherSlots() {
		insertUpdatesStacks(
				new InsertItemUpdatesStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64, 1, new ItemStack(Items.GOLD_NUGGET, 64), 64,
						Map.of(1, new ItemStack(Items.GOLD_NUGGET, 64)),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.GOLD_NUGGET, 64), 2, ItemStack.EMPTY)
				)
		);
	}

	@Test
	void extractItemAllowsDifferentToBeInsertedIfExtractedFully() {
		int originalAmount = 63;
		Map<Integer, ItemStack> slotStacksInput = Map.of(0, new ItemStack(Items.IRON_BLOCK, originalAmount), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY);
		InventoryHandler invHandler = getFilledInventoryHandler(slotStacksInput, 64);

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));
		int amountToInsert = 10;
		int insertResult;
		try (Transaction tx = Transaction.openRoot()) {
			part.extract(0, ItemResource.of(Items.IRON_BLOCK), originalAmount, tx, (s, st, amount, transaction) -> originalAmount);

			insertResult = part.insert(1, ItemResource.of(Items.GOLD_NUGGET), amountToInsert, tx, (s, st, amount, transaction) -> amountToInsert);
			tx.commit();
		}

		assertEquals(amountToInsert, insertResult);
	}

	@Test
	void extractDoesntAllowDifferentInMemorizedSlotsEvenIfExtractedFully() {
		int originalAmount = 32;
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, originalAmount), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		MemorySettingsCategory memorySettings = getMemorySettings(invHandler, Map.of());
		when(memorySettings.getSlotFilterStack(eq(0), anyBoolean())).thenReturn(Optional.of(new ItemStack(Items.IRON_BLOCK)));

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> memorySettings);
		int insertResult;
		try (Transaction tx = Transaction.openRoot()) {
			part.extract(0, ItemResource.of(Items.IRON_BLOCK), originalAmount, tx, (s, st, amount, transaction) -> originalAmount);
			insertResult = part.insert(0, ItemResource.of(Items.GOLD_BLOCK), originalAmount, tx, (s, st, amount, transaction) -> originalAmount);
			tx.commit();
		}

		assertEquals(0, insertResult, "Insert result does not equal");
	}

	@Test
	void properlyInitializesItemsBasedOnMemorizedSlots() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		MemorySettingsCategory memorySettings = getMemorySettings(invHandler, Map.of());
		when(memorySettings.getSlotFilterStack(1, true)).thenReturn(Optional.of(new ItemStack(Items.IRON_BLOCK)));

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> memorySettings);

		int firstResult;
		int secondResult;
		try (Transaction tx = Transaction.openRoot()) {
			firstResult = part.insert(1, ItemResource.of(Items.GOLD_BLOCK), 32, tx, (s, st, amount, transaction) -> amount);
			secondResult = part.insert(1, ItemResource.of(Items.IRON_BLOCK), 32, tx, (s, st, amount, transaction) -> amount);
		}


		assertEquals(0, firstResult, "Insert result does not equal");
		assertEquals(32, secondResult, "Insert result does not equal");
	}

	@Test
	void insertingIntoEmptyCompressionPartRefreshesParentAfterCalculatedStacksExist() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		MemorySettingsCategory memorySettings = getMemorySettings(invHandler, Map.of());
		Supplier<MemorySettingsCategory> memorySettingsSupplier = () -> memorySettings;

		CompressionInventoryPart part = new CompressionInventoryPart(invHandler, new SlotRange(0, 3), memorySettingsSupplier) {
			@Override
			Optional<RecipeHelper.UncompactingResult> getDecompressionResultFromConfig(Item currentItem) {
				return Optional.empty();
			}
		};
		part.onInit();
		clearInvocations(invHandler);

		try (Transaction tx = Transaction.openRoot()) {
			part.insert(0, ItemResource.of(Items.IRON_BLOCK), 1, tx, (slot, resource, amount, transaction) -> amount);
			tx.commit();
		}

		InOrder inOrder = inOrder(invHandler);
		inOrder.verify(invHandler, atLeastOnce()).setStackInSlotInternal(anyInt(), any(ItemStack.class));
		inOrder.verify(invHandler).onFilterItemsChanged();
		assertCalculatedStacks(Map.of(
				0, new ItemStack(Items.IRON_BLOCK, 1),
				1, new ItemStack(Items.IRON_INGOT, 9),
				2, new ItemStack(Items.IRON_NUGGET, 81)
		), 0, part);
	}

	@Test
	void insertingIntoRememberedCompressionSlotRefreshesParentAfterCalculatedStacksExist() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		MemorySettingsCategory memorySettings = getMemorySettings(invHandler, Map.of());
		when(memorySettings.getSlotFilterStack(1, true)).thenReturn(Optional.of(new ItemStack(Items.IRON_BLOCK)));

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> memorySettings);
		clearInvocations(invHandler);

		try (Transaction tx = Transaction.openRoot()) {
			part.insert(1, ItemResource.of(Items.IRON_BLOCK), 1, tx, (slot, resource, amount, transaction) -> amount);
			tx.commit();
		}

		InOrder inOrder = inOrder(invHandler);
		inOrder.verify(invHandler, atLeastOnce()).setStackInSlotInternal(anyInt(), any(ItemStack.class));
		inOrder.verify(invHandler).onFilterItemsChanged();
		assertCalculatedStacks(Map.of(
				0, ItemStack.EMPTY,
				1, new ItemStack(Items.IRON_BLOCK, 1),
				2, new ItemStack(Items.IRON_INGOT, 9)
		), 0, part);
	}

	@Test
	void insertingWithoutChangingControllerVisibleCompressionStateDoesNotRefreshParent() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));
		clearInvocations(invHandler);

		try (Transaction tx = Transaction.openRoot()) {
			part.insert(0, ItemResource.of(Items.IRON_BLOCK), 1, tx, (slot, resource, amount, transaction) -> amount);
			tx.commit();
		}

		verify(invHandler, never()).onFilterItemsChanged();
		assertCalculatedStacks(Map.of(
				0, new ItemStack(Items.IRON_BLOCK, 2),
				1, new ItemStack(Items.IRON_INGOT, 18),
				2, new ItemStack(Items.IRON_NUGGET, 162)
		), 0, part);
	}

	@Test
	void insertingAcrossCompressionCapacityBoundaryRefreshesParent() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, 63), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));
		clearInvocations(invHandler);

		try (Transaction tx = Transaction.openRoot()) {
			part.insert(0, ItemResource.of(Items.IRON_BLOCK), 1, tx, (slot, resource, amount, transaction) -> amount);
			tx.commit();
		}

		verify(invHandler).onFilterItemsChanged();
		assertCalculatedStacks(Map.of(
				0, new ItemStack(Items.IRON_BLOCK, 64),
				1, new ItemStack(Items.IRON_INGOT, 576),
				2, new ItemStack(Items.IRON_NUGGET, 5184)
		), 0, part);
	}

	@Test
	void extractingNuggetFromBlockAndInsertingItBackKeepsCompressionStateStable() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));
		clearInvocations(invHandler);

		int extracted;
		try (Transaction tx = Transaction.openRoot()) {
			extracted = part.extract(2, ItemResource.of(Items.IRON_NUGGET), 1, tx, (slot, resource, amount, transaction) -> amount);
			tx.commit();
		}

		assertEquals(1, extracted, "Extract result doesn't match");
		verify(invHandler).onFilterItemsChanged();
		assertCalculatedStacks(Map.of(
				0, ItemStack.EMPTY,
				1, new ItemStack(Items.IRON_INGOT, 8),
				2, new ItemStack(Items.IRON_NUGGET, 80)
		), 0, part);
		assertInternalStacks(Map.of(
				0, ItemStack.EMPTY,
				1, new ItemStack(Items.IRON_INGOT, 8),
				2, new ItemStack(Items.IRON_NUGGET, 8)
		), invHandler);

		clearInvocations(invHandler);

		int inserted;
		try (Transaction tx = Transaction.openRoot()) {
			inserted = part.insert(2, ItemResource.of(Items.IRON_NUGGET), 1, tx, (slot, resource, amount, transaction) -> amount);
			tx.commit();
		}

		assertEquals(1, inserted, "Insert result doesn't match");
		verify(invHandler).onFilterItemsChanged();
		assertCalculatedStacks(Map.of(
				0, new ItemStack(Items.IRON_BLOCK, 1),
				1, new ItemStack(Items.IRON_INGOT, 9),
				2, new ItemStack(Items.IRON_NUGGET, 81)
		), 0, part);
		assertInternalStacks(Map.of(
				0, new ItemStack(Items.IRON_BLOCK, 1),
				1, ItemStack.EMPTY,
				2, ItemStack.EMPTY
		), invHandler);
	}

	@Test
	void simulatedInsertDoesNotRefreshParent() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));
		clearInvocations(invHandler);

		try (Transaction tx = Transaction.openRoot()) {
			part.insert(0, ItemResource.of(Items.IRON_BLOCK), 1, tx, (slot, resource, amount, transaction) -> amount);
		}

		verify(invHandler, never()).onFilterItemsChanged();
		assertCalculatedStacks(Map.of(
				0, ItemStack.EMPTY,
				1, ItemStack.EMPTY,
				2, ItemStack.EMPTY
		), 0, part);
	}

	@Test
	void simulatedExtractDoesNotRefreshParent() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));
		clearInvocations(invHandler);

		try (Transaction tx = Transaction.openRoot()) {
			part.extract(0, ItemResource.of(Items.IRON_BLOCK), 1, tx, (slot, resource, amount, transaction) -> amount);
		}

		verify(invHandler, never()).onFilterItemsChanged();
		assertCalculatedStacks(Map.of(
				0, new ItemStack(Items.IRON_BLOCK, 1),
				1, new ItemStack(Items.IRON_INGOT, 9),
				2, new ItemStack(Items.IRON_NUGGET, 81)
		), 0, part);
	}

	@Test
	void fillingMiddleCompressionSlotAndReloadingKeepsCalculatedStacksStable() {
		Map<Integer, ItemStack> slotStacksInput = Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY);
		InventoryHandler invHandler = getFilledInventoryHandler(slotStacksInput, 640);
		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));

		part.set(1, ItemResource.of(Items.IRON_INGOT), 6400, (slot, resource, amount) -> {
		});

		Map<Integer, ItemStack> expectedCalculatedStacks = Map.of(
				0, new ItemStack(Items.IRON_BLOCK, 640),
				1, new ItemStack(Items.IRON_INGOT, 6400),
				2, new ItemStack(Items.IRON_NUGGET, 57600)
		);
		assertCalculatedStacks(expectedCalculatedStacks, 0, part);

		CompressionInventoryPart reloadedPart = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));
		assertCalculatedStacks(expectedCalculatedStacks, 0, reloadedPart);
	}

	@Test
	void initializingFromFullMiddleSlotCompactsIntoBlockSlotUsingBlockLimit() {
		InventoryHandler invHandler = getFilledInventoryHandler(
				Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 6400), 2, ItemStack.EMPTY),
				640
		);

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));

		assertCalculatedStacks(Map.of(
				0, new ItemStack(Items.IRON_BLOCK, 640),
				1, new ItemStack(Items.IRON_INGOT, 6400),
				2, new ItemStack(Items.IRON_NUGGET, 57600)
		), 0, part);
		assertStackEquals(new ItemStack(Items.IRON_BLOCK, 640), invHandler.getInternalStack(0), "Block slot internal stack doesn't match");
		assertStackEquals(new ItemStack(Items.IRON_INGOT, 640), invHandler.getInternalStack(1), "Ingot slot internal stack doesn't match");
		assertStackEquals(ItemStack.EMPTY, invHandler.getInternalStack(2), "Nugget slot internal stack doesn't match");
	}

	@ParameterizedTest
	@MethodSource("setUpdatesStacksData")
	void setUpdatesStacks(SetUpdatesStacksParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.internalStacksBefore, params.baseSlotLimit);
		int minSlot = Collections.min(params.internalStacksBefore.keySet());

		CompressionInventoryPart part = initCompressionInventoryPart(params.internalStacksBefore, invHandler, minSlot);

		part.set(params.insertSlot, ItemResource.of(params.stack), params.stack.getCount(), (slot, res, amount) -> {
		});

		assertCalculatedStacks(params.calculatedStacksAfter, minSlot, part);
		assertInternalStacks(params.internalStacksAfter, invHandler);
	}

	public record SetUpdatesStacksParams(
			Map<Integer, ItemStack> internalStacksBefore,
			int baseSlotLimit,
			int insertSlot,
			ItemStack stack,
			Map<Integer, ItemStack> internalStacksAfter,
			Map<Integer, ItemStack> calculatedStacksAfter
	) {
	}

	public static List<SetUpdatesStacksParams> setUpdatesStacksData() {
		return List.of(
				new SetUpdatesStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 100),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 11), 2, new ItemStack(Items.IRON_NUGGET, 100))
				),
				new SetUpdatesStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.IRON_NUGGET, 100),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 11), 1, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 11), 1, new ItemStack(Items.IRON_NUGGET, 100), 2, ItemStack.EMPTY)
				),
				new SetUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 63), 1, new ItemStack(Items.IRON_INGOT, 8), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 1000),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 12), 1, new ItemStack(Items.IRON_INGOT, 3), 2, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 12), 1, new ItemStack(Items.IRON_INGOT, 111), 2, new ItemStack(Items.IRON_NUGGET, 1000))
				),
				new SetUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						1,
						new ItemStack(Items.IRON_INGOT, 1),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 1)),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 1), 2, new ItemStack(Items.IRON_NUGGET, 14))
				)
		);
	}

	@ParameterizedTest
	@MethodSource("setStackInSlotUpdatesStacksData")
	void setStackInSlotUpdatesStacks(SetStackInSlotUpdatesStacksParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.internalStacksBefore, params.baseSlotLimit);
		int minSlot = Collections.min(params.internalStacksBefore.keySet());
		CompressionInventoryPart part = initCompressionInventoryPart(params.internalStacksBefore, invHandler, minSlot);

		part.setStackInSlot(params.setSlot, params.setStack, invHandler::setStackInSlot);

		assertCalculatedStacks(params.calculatedStacksAfter, minSlot, part);
		assertInternalStacks(params.internalStacksUpdated, invHandler, false);
	}

	public record SetStackInSlotUpdatesStacksParams(
			Map<Integer, ItemStack> internalStacksBefore,
			int baseSlotLimit,
			int setSlot,
			ItemStack setStack,
			Map<Integer, ItemStack> internalStacksUpdated,
			Map<Integer, ItemStack> calculatedStacksAfter
	) {
	}

	public static List<SetStackInSlotUpdatesStacksParams> setStackInSlotUpdatesStacksData() {
		return List.of(
				new SetStackInSlotUpdatesStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 100),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 11), 2, new ItemStack(Items.IRON_NUGGET, 100))
				),
				new SetStackInSlotUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 63), 1, new ItemStack(Items.IRON_INGOT, 8), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 1000),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 12), 1, new ItemStack(Items.IRON_INGOT, 3), 2, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 12), 1, new ItemStack(Items.IRON_INGOT, 111), 2, new ItemStack(Items.IRON_NUGGET, 1000))
				),
				new SetStackInSlotUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						1,
						ItemStack.EMPTY,
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, new ItemStack(Items.IRON_NUGGET, 5)),
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, new ItemStack(Items.IRON_NUGGET, 5))
				),
				new SetStackInSlotUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.REDSTONE_BLOCK, 7), 1, new ItemStack(Items.REDSTONE, 1), 2, ItemStack.EMPTY),
						64,
						1,
						ItemStack.EMPTY,
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY)

				),
				new SetStackInSlotUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.REDSTONE_BLOCK, 2), 1, new ItemStack(Items.REDSTONE, 7), 2, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.REDSTONE, 26),
						Map.of(0, new ItemStack(Items.REDSTONE_BLOCK, 2), 1, new ItemStack(Items.REDSTONE, 8), 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.REDSTONE_BLOCK, 2), 1, new ItemStack(Items.REDSTONE, 26), 2, ItemStack.EMPTY)
				),
				new SetStackInSlotUpdatesStacksParams(
						Map.of(0, new ItemStack(Items.REDSTONE, 32), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						0,
						new ItemStack(Items.REDSTONE, 64),
						Map.of(0, new ItemStack(Items.REDSTONE, 64), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.REDSTONE, 64), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY)
				)
		);
	}

	@Test
	void puttingDamagedDecompressibleItemInDoesntHealIt() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + 3), () -> getMemorySettings(invHandler, Map.of()));

		ItemStack damagedItem = new ItemStack(Items.NETHERITE_AXE);
		damagedItem.setDamageValue(10);
		try (Transaction tx = Transaction.openRoot()) {
			ItemResource resource = ItemResource.of(damagedItem);
			part.insert(1, resource, damagedItem.getCount(), tx, (s, res, amount, transaction) -> damagedItem.getCount());
			tx.commit();
		}

		assertStackEquals(damagedItem, part.getStackInSlot(1, s -> ItemStack.EMPTY), "Damaged item doesn't match");
	}

	@Test
	void initializingWithDamagedDecompressibleItemDoesntHealIt() {
		ItemStack damagedItem = new ItemStack(Items.NETHERITE_AXE);
		damagedItem.setDamageValue(10);

		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, damagedItem, 2, ItemStack.EMPTY), 64);
		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));

		assertStackEquals(damagedItem, part.getStackInSlot(1, s -> ItemStack.EMPTY), "Damaged item doesn't match");
	}

	@Test
	void extractingDecompressibleItemWorks() {
		ItemStack damagedItem = new ItemStack(Items.NETHERITE_AXE);
		damagedItem.setDamageValue(10);

		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, damagedItem, 2, ItemStack.EMPTY), 64);
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + 3), () -> getMemorySettings(invHandler, Map.of()));

		ItemStack damagedItemToMatch = new ItemStack(Items.NETHERITE_AXE);
		damagedItemToMatch.setDamageValue(10);

		int result;
		try (Transaction tx = Transaction.openRoot()) {
			ItemResource resource = ItemResource.of(damagedItemToMatch);
			result = part.extract(1, resource, damagedItemToMatch.getCount(), tx, (s, res, amount, transaction) -> damagedItemToMatch.getCount());
			tx.commit();
		}

		assertEquals(1, result, "Extracted amount doesn't match");
	}

	@Test
	void extractingPartOfDecompressibleStackCorrectlyLeavesTheRestIn() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.COBBLESTONE, 10), 2, ItemStack.EMPTY), 64);
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + 3), () -> getMemorySettings(invHandler, Map.of()));

		int result;
		try (Transaction tx = Transaction.openRoot()) {
			result = part.extract(1, ItemResource.of(Items.COBBLESTONE), 1, tx, (s, res, amount, transaction) -> 1);
			tx.commit();
		}

		assertEquals(1, result, "Extracted item doesn't match");
		assertStackEquals(new ItemStack(Items.COBBLESTONE, 9), part.getStackInSlot(1, s -> ItemStack.EMPTY), "Item left in slot doesn't match");
	}

	@ParameterizedTest
	@MethodSource("stackLimitsAreSetCorrectlyOnInitData")
	void stackLimitsAreSetCorrectlyOnInit(StackLimitsAreSetCorrectlyOnInitParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		params.expectedLimits().forEach((slot, stackLimit) -> assertEquals(stackLimit.getRight(), part.getCapacity(slot, ItemResource.of(stackLimit.getLeft())), "Stack limit doesn't match"));
	}

	private record StackLimitsAreSetCorrectlyOnInitParams(Map<Integer, ItemStack> stacks, int baseLimit,
														  Map<Integer, Pair<ItemStack, Integer>> expectedLimits) {
	}

	private static List<StackLimitsAreSetCorrectlyOnInitParams> stackLimitsAreSetCorrectlyOnInitData() {
		return List.of(
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						Map.of(0, ImmutablePair.of(new ItemStack(Items.IRON_BLOCK), 64), 1, ImmutablePair.of(new ItemStack(Items.IRON_INGOT), 9 * 64 + 64), 2, ImmutablePair.of(new ItemStack(Items.IRON_NUGGET), 9 * 9 * 64 + 9 * 64 + 64))
				),
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT), 1, ItemStack.EMPTY),
						64,
						Map.of(0, ImmutablePair.of(new ItemStack(Items.IRON_INGOT), 64), 1, ImmutablePair.of(new ItemStack(Items.IRON_NUGGET), 9 * 64 + 64))
				),
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT), 1, ItemStack.EMPTY),
						Integer.MAX_VALUE,
						Map.of(0, ImmutablePair.of(new ItemStack(Items.IRON_INGOT), Integer.MAX_VALUE), 1, ImmutablePair.of(new ItemStack(Items.IRON_NUGGET), Integer.MAX_VALUE))
				),
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_SWORD), 1, ItemStack.EMPTY),
						64,
						Map.of(0, ImmutablePair.of(new ItemStack(Items.IRON_SWORD), 1), 1, ImmutablePair.of(new ItemStack(Items.IRON_SWORD), 0))
				),
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.DEAD_BUSH), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY, 3, ItemStack.EMPTY),
						64,
						Map.of(
								0, ImmutablePair.of(new ItemStack(Items.DEAD_BUSH), 64), 1, ImmutablePair.of(new ItemStack(Items.STICK), 9 * 64 + 64),
								2, ImmutablePair.of(new ItemStack(Items.QUARTZ_BLOCK), 9 * 9 * 64 + 9 * 64 + 64), 3, ImmutablePair.of(new ItemStack(Items.QUARTZ), 9 * 9 * 4 * 64 + 9 * 4 * 64 + 4 * 64 + 64)
						)
				)
		);
	}

	@ParameterizedTest
	@MethodSource("insertingAdditionalUncompressibleItemsProperlyCalculatesCountData")
	void insertingAdditionalUncompressibleItemsProperlyCalculatesCount(InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		try (Transaction tx = Transaction.openRoot()) {
			ItemStack stack = params.insertedStack.getRight();
			part.insert(params.insertedStack.getLeft(), ItemResource.of(stack), stack.getCount(), tx, (slot, resource, amount, transaction) -> stack.getCount());
			tx.commit();
		}

		assertCalculatedStacks(params.expectedStacksSet(), 0, part);
		assertInternalStacks(params.expectedStacksSet(), invHandler);
	}

	private record InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams(Map<Integer, ItemStack> stacks,
																					   int baseLimit,
																					   Pair<Integer, ItemStack> insertedStack,
																					   Map<Integer, ItemStack> expectedStacksSet) {
	}

	private static List<InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams> insertingAdditionalUncompressibleItemsProperlyCalculatesCountData() {
		return List.of(
				new InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams(
						Map.of(0, new ItemStack(Items.SAND, 23), 1, ItemStack.EMPTY),
						64,
						ImmutablePair.of(0, new ItemStack(Items.SAND, 41)),
						Map.of(0, new ItemStack(Items.SAND, 64))
				),
				new InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams(
						Map.of(0, new ItemStack(Items.SAND, 23), 1, ItemStack.EMPTY),
						256,
						ImmutablePair.of(0, new ItemStack(Items.SAND, 128)),
						Map.of(0, new ItemStack(Items.SAND, 151))
				),
				new InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY),
						256,
						ImmutablePair.of(0, new ItemStack(Items.SAND, 256)),
						Map.of(0, new ItemStack(Items.SAND, 256))
				)
		);
	}

	@ParameterizedTest
	@MethodSource("extractingFromFullyFilledSlotsProperlyCalculatesCountsData")
	void extractingFromFullyFilledSlotsProperlyCalculatesCounts(ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		try (Transaction tx = Transaction.openRoot()) {
			ItemStack stack = params.stacks().get(params.extractedStack.getLeft());
			part.extract(params.extractedStack.getLeft(), ItemResource.of(stack), params.extractedStack.getRight(), tx, (s, res, amount, transaction) -> params.extractedStack.getRight());
			tx.commit();
		}

		assertCalculatedStacks(params.expectedCalculatedStacks(), 0, part);
	}

	private record ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(Map<Integer, ItemStack> stacks,
																				int baseLimit,
																				Pair<Integer, Integer> extractedStack,
																				Map<Integer, ItemStack> expectedCalculatedStacks) {
	}

	private static List<ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams> extractingFromFullyFilledSlotsProperlyCalculatesCountsData() {
		return List.of(
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256)),
						256,
						ImmutablePair.of(1, 64),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2496))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256)),
						256,
						ImmutablePair.of(1, 256),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2496))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256)),
						256,
						ImmutablePair.of(1, 0),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2560))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256), 2, new ItemStack(Items.IRON_NUGGET, 1)),
						256,
						ImmutablePair.of(2, 64),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2553), 2, new ItemStack(Items.IRON_NUGGET, 22977))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256), 2, new ItemStack(Items.IRON_NUGGET, 256)),
						256,
						ImmutablePair.of(2, 256 + 10 * 9),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2560), 2, new ItemStack(Items.IRON_NUGGET, 23232)) // the extract gets maxed to 64
				)
		);
	}

	@ParameterizedTest
	@MethodSource("initializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksData")
	void initializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacks(InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		assertCalculatedStacks(params.calculatedStacks(), 0, part);
	}

	private record InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams(
			Map<Integer, ItemStack> stacks, int baseLimit, Map<Integer, ItemStack> calculatedStacks) {
	}

	private static List<InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams> initializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksData() {
		return List.of(
				new InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_AXE), 2, new ItemStack(Items.IRON_INGOT), 3, ItemStack.EMPTY),
						64,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_AXE), 2, new ItemStack(Items.IRON_INGOT), 3, ItemStack.EMPTY)
				),
				new InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_AXE), 2, new ItemStack(Items.IRON_INGOT, 4), 3, new ItemStack(Items.IRON_NUGGET, 3)),
						256,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_AXE), 2, new ItemStack(Items.IRON_INGOT, 4), 3, new ItemStack(Items.IRON_NUGGET, 3))
				),
				new InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT, 4), 1, new ItemStack(Items.IRON_NUGGET, 3), 2, ItemStack.EMPTY, 3, new ItemStack(Items.IRON_AXE)),
						256,
						Map.of(0, new ItemStack(Items.IRON_INGOT, 4), 1, new ItemStack(Items.IRON_NUGGET, 3), 2, ItemStack.EMPTY, 3, new ItemStack(Items.IRON_AXE))
				)
		);
	}

	@ParameterizedTest
	@MethodSource("settingStackMultipleTimesResultsInCorrectCalculatedStacksData")
	void settingStackMultipleTimesResultsInCorrectCalculatedStacks(SettingStackMultipleTimesResultsInCorrectCalculatedStacksParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		for (ItemStack stack : params.stacksToSet) {
			part.set(params.slot, ItemResource.of(stack), stack.getCount(), (slot, resource, amount) -> {
				invHandler.setStackInSlot(slot, resource.toStack(amount));
			});
		}

		assertCalculatedStacks(params.expectedCalculatedStacks, minSlot, part);
	}

	private record SettingStackMultipleTimesResultsInCorrectCalculatedStacksParams(
			Map<Integer, ItemStack> stacks,
			int baseLimit,
			Map<Integer, ItemStack> expectedCalculatedStacks, int slot, ItemStack... stacksToSet) {
	}

	private static List<SettingStackMultipleTimesResultsInCorrectCalculatedStacksParams> settingStackMultipleTimesResultsInCorrectCalculatedStacksData() {
		return List.of(
				new SettingStackMultipleTimesResultsInCorrectCalculatedStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 11), 2, new ItemStack(Items.IRON_NUGGET, 100)),
						2,
						new ItemStack(Items.IRON_NUGGET, 100)
				),
				new SettingStackMultipleTimesResultsInCorrectCalculatedStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 22), 2, new ItemStack(Items.IRON_NUGGET, 200)),
						2,
						new ItemStack(Items.IRON_NUGGET, 100), new ItemStack(Items.IRON_NUGGET, 200)
				),
				new SettingStackMultipleTimesResultsInCorrectCalculatedStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 2), 2, ItemStack.EMPTY),
						64,
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 18), 2, new ItemStack(Items.IRON_NUGGET, 162)),
						1,
						new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.IRON_INGOT, 18)
				),
				new SettingStackMultipleTimesResultsInCorrectCalculatedStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 2), 2, ItemStack.EMPTY),
						64,
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 18), 2, new ItemStack(Items.IRON_NUGGET, 162)),
						1,
						new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.IRON_INGOT, 18), new ItemStack(Items.IRON_INGOT, 36), new ItemStack(Items.IRON_INGOT, 54), new ItemStack(Items.IRON_INGOT, 18)
				)
		);
	}

	@Test
	public void extractFromIncorrectSlotReturnsZero() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 3)), 64);
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + 3), () -> getMemorySettings(invHandler, Map.of()));

		int result;
		try (Transaction tx = Transaction.openRoot()) {
			result = part.extract(3, ItemResource.of(Items.IRON_BLOCK), 1, tx, (s, res, amount, transaction) -> 1);
			tx.commit();
		}

		assertEquals(0, result, "Extracted amount doesn't match");
	}

	@Test
	public void extractFromSlotAfterLastExtractedReturnsEmpty() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 3)), 64);
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new SlotRange(minSlot, minSlot + 3), () -> getMemorySettings(invHandler, Map.of()));

		int result;
		try (Transaction tx = Transaction.open(null)) {
			part.extract(0, ItemResource.of(Items.IRON_BLOCK), 1, tx, (s, res, amount, transaction) -> 1);
			part.extract(1, ItemResource.of(Items.IRON_INGOT), 2, tx, (s, res, amount, transaction) -> 2);
			part.extract(2, ItemResource.of(Items.IRON_NUGGET), 3, tx, (s, res, amount, transaction) -> 3);

			result = part.extract(2, ItemResource.of(Items.IRON_NUGGET), 1, tx, (s, res, amount, transaction) -> 1);
			tx.commit();
		}

		assertEquals(0, result, "Extracted amount doesn't match");
	}
}
