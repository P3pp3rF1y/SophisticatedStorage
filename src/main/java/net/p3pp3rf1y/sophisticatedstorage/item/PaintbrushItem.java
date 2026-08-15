package net.p3pp3rf1y.sophisticatedstorage.item;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.*;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.util.DecorationHelper;
import net.p3pp3rf1y.sophisticatedstorage.util.SimpleMaterialHelper;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class PaintbrushItem extends ItemBase {
	public static final Codec<Map<Identifier, Integer>> REMAINING_PARTS_CODEC = Codec.unboundedMap(Identifier.CODEC, ExtraCodecs.POSITIVE_INT);

	public static final StreamCodec<FriendlyByteBuf, Map<Identifier, Integer>> REMAINING_PARTS_STREAM_CODEC = ByteBufCodecs.map(HashMap::new,
			Identifier.STREAM_CODEC, ByteBufCodecs.INT, 64);

	public PaintbrushItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	public static void setBarrelMaterials(ItemStack paintbrush, Map<BarrelMaterial, Identifier> materials) {
		paintbrush.set(ModDataComponents.BARREL_MATERIALS, Map.copyOf(materials));
		resetMainColor(paintbrush);
		resetAccentColor(paintbrush);
	}

	public static Optional<ItemRequirements> getItemRequirements(ItemStack paintbrush, Player player, Level level, BlockPos lookingAtPos) {
		Map<BarrelMaterial, Identifier> materialsToApply = new HashMap<>(getBarrelMaterials(paintbrush));
		BlockEntity be = level.getBlockEntity(lookingAtPos);
		if (be == null) {
			return Optional.empty();
		}

		if (!materialsToApply.isEmpty()) {
			return getMaterialItemRequirements(paintbrush, player, be, materialsToApply);
		} else {
			return getDyeItemRequirements(paintbrush, player, be);
		}
	}

	private static Optional<ItemRequirements> getMaterialItemRequirements(ItemStack paintbrush, Player player, BlockEntity be,
			Map<BarrelMaterial, Identifier> materialsToApply) {
		Map<Identifier, Integer> allPartsNeeded = new HashMap<>();
		if (be instanceof ControllerBlockEntity controllerBe) {
			if (player.isCrouching()) {
				addSimpleMaterialPartsNeeded(materialsToApply, controllerBe, allPartsNeeded);
			} else {
				for (BlockPos storagePosition : controllerBe.getStoragePositions()) {
					addStorageMaterialPartsNeeded(materialsToApply, controllerBe, storagePosition, allPartsNeeded);
				}
			}
		} else if (be instanceof ISimpleMaterialHolder simpleMaterialHolder) {
			addSimpleMaterialPartsNeeded(materialsToApply, simpleMaterialHolder, allPartsNeeded);
		} else if (be instanceof IMaterialHolder materialHolder) {
			allPartsNeeded = getMaterialHolderPartsNeeded(materialsToApply, materialHolder);
		}

		if (allPartsNeeded.isEmpty()) {
			return Optional.empty();
		}
		return getItemRequirements(paintbrush, player, allPartsNeeded);
	}

	private static void addSimpleMaterialPartsNeeded(Map<BarrelMaterial, Identifier> materialsToApply, ISimpleMaterialHolder simpleMaterialHolder,
			Map<Identifier, Integer> allPartsNeeded) {
		Optional<Identifier> material = SimpleMaterialHelper.getSingleMaterial(materialsToApply);
		if (material.isPresent()) {
			allPartsNeeded.putAll(DecorationHelper.getSimpleMaterialPartsNeeded(simpleMaterialHolder.getMaterial(), material.get()));
		}
	}

	public static Optional<ItemRequirements> getItemRequirements(ItemStack paintbrush, Player player, Map<Identifier, Integer> allPartsNeeded) {
		Map<Identifier, Integer> remainingParts = new HashMap<>(getRemainingParts(paintbrush));
		DecorationHelper.ConsumptionResult result;
		try (Transaction tx = Transaction.openRoot()) {
			SnapshotJournal<Map<Identifier, Integer>> remainingPartsJournal = createNoopRemainingPartsJournal();
			result = DecorationHelper.consumeMaterialPartsNeeded(allPartsNeeded, remainingParts, remainingPartsJournal,
					InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player), tx);
		}

		List<ItemStack> itemsPresent = new ArrayList<>();
		List<ItemStack> itemsMissing = new ArrayList<>();

		for (Map.Entry<Identifier, Integer> entry : allPartsNeeded.entrySet()) {
			Identifier part = entry.getKey();
			int count = Math.ceilDiv(entry.getValue() - remainingParts.getOrDefault(part, 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int missing = Math.ceilDiv(result.missingParts().getOrDefault(part, 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int present = count - missing;
			BuiltInRegistries.ITEM.getOptional(part).ifPresent(item -> {
				if (missing > 0) {
					itemsMissing.add(new ItemStack(item, missing));
				}
				if (present > 0) {
					itemsPresent.add(new ItemStack(item, present));
				}
			});
		}

		return Optional.of(new ItemRequirements(itemsPresent, itemsMissing));
	}

	private static void addStorageMaterialPartsNeeded(Map<BarrelMaterial, Identifier> materialsToApply, ControllerBlockEntity controllerBe,
			BlockPos storagePosition, Map<Identifier, Integer> allPartsNeeded) {
		WorldHelper.getBlockEntity(controllerBe.getLevel(), storagePosition, IMaterialHolder.class).ifPresent(materialHolder -> {
			Map<Identifier, Integer> storagePartsNeeded = getMaterialHolderPartsNeeded(materialsToApply, materialHolder);
			storagePartsNeeded.forEach((part, count) -> allPartsNeeded.merge(part, count, Integer::sum));
		});
	}

	private static Optional<ItemRequirements> getDyeItemRequirements(ItemStack paintbrush, Player player, BlockEntity be) {
		int mainColorToSet = getMainColor(paintbrush);
		int accentColorToSet = getAccentColor(paintbrush);

		Map<TagKey<Item>, Integer> allPartsNeeded = new HashMap<>();
		if (be instanceof StorageBlockEntity storageBe) {
			allPartsNeeded = getStorageDyePartsNeeded(mainColorToSet, accentColorToSet, storageBe.getStorageWrapper());
		} else if (be instanceof ControllerBlockEntity controllerBe) {
			for (BlockPos storagePosition : controllerBe.getStoragePositions()) {
				addStorageDyePartsNeeded(mainColorToSet, accentColorToSet, controllerBe, storagePosition, allPartsNeeded);
			}
		}

		if (allPartsNeeded.isEmpty()) {
			return Optional.empty();
		}
		return getDyeItemRequirements(paintbrush, player, allPartsNeeded);
	}

	public static Optional<ItemRequirements> getDyeItemRequirements(ItemStack paintbrush, Player player, Map<TagKey<Item>, Integer> allPartsNeeded) {
		Map<Identifier, Integer> remainingParts = new HashMap<>(getRemainingParts(paintbrush));

		SnapshotJournal<Map<Identifier, Integer>> remainingPartsJournal = createNoopRemainingPartsJournal();
		try (Transaction tx = Transaction.openRoot()) {
			DecorationHelper.ConsumptionResult result = DecorationHelper.consumeDyePartsNeeded(allPartsNeeded,
					InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player), remainingParts, remainingPartsJournal, tx);
			return compileDyeItemRequirements(allPartsNeeded, remainingParts, result);
		}
	}

	private static SnapshotJournal<Map<Identifier, Integer>> createNoopRemainingPartsJournal() {
		return new SnapshotJournal<>() { // just a dummy journal because the remaining parts on the stack don't get updated anywhere in here
			@Override
			protected Map<Identifier, Integer> createSnapshot() {
				return new HashMap<>();
			}

			@Override
			protected void revertToSnapshot(Map<Identifier, Integer> map) {
				// noop
			}
		};
	}

	private static void addStorageDyePartsNeeded(int mainColorToSet, int accentColorToSet, ControllerBlockEntity controllerBe, BlockPos storagePosition,
			Map<TagKey<Item>, Integer> allPartsNeeded) {
		WorldHelper.getBlockEntity(controllerBe.getLevel(), storagePosition, StorageBlockEntity.class).ifPresent(storageBe -> {
			Map<TagKey<Item>, Integer> storagePartsNeeded = getStorageDyePartsNeeded(mainColorToSet, accentColorToSet, storageBe.getStorageWrapper());
			storagePartsNeeded.forEach((part, count) -> allPartsNeeded.merge(part, count, Integer::sum));
		});
	}

	public static Map<TagKey<Item>, Integer> getStorageDyePartsNeeded(int mainColorToSet, int accentColorToSet, ITintable tintable) {
		return DecorationHelper.getDyePartsNeeded(mainColorToSet, accentColorToSet, tintable.getMainColor(), tintable.getAccentColor());
	}

	private static Optional<ItemRequirements> compileDyeItemRequirements(Map<TagKey<Item>, Integer> allPartsNeeded, Map<Identifier, Integer> remainingParts,
			DecorationHelper.ConsumptionResult result) {
		List<ItemStack> itemsPresent = new ArrayList<>();
		List<ItemStack> itemsMissing = new ArrayList<>();

		for (Map.Entry<TagKey<Item>, Integer> entry : allPartsNeeded.entrySet()) {
			TagKey<Item> part = entry.getKey();
			int count = Math.ceilDiv(entry.getValue() - remainingParts.getOrDefault(part.location(), 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int missing = Math.ceilDiv(result.missingParts().getOrDefault(part.location(), 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int present = count - missing;

			Item dyeItem;
			if (part == Tags.Items.DYES_RED) {
				dyeItem = Items.RED_DYE;
			} else if (part == Tags.Items.DYES_GREEN) {
				dyeItem = Items.GREEN_DYE;
			} else if (part == Tags.Items.DYES_BLUE) {
				dyeItem = Items.BLUE_DYE;
			} else {
				continue;
			}

			if (missing > 0) {
				itemsMissing.add(new ItemStack(dyeItem, missing));
			}
			if (present > 0) {
				itemsPresent.add(new ItemStack(dyeItem, present));
			}
		}

		return Optional.of(new ItemRequirements(itemsPresent, itemsMissing));
	}

	public static Map<Identifier, Integer> getMaterialHolderPartsNeeded(Map<BarrelMaterial, Identifier> materialsToApply, IMaterialHolder materialHolder) {
		Map<BarrelMaterial, Identifier> originalMaterials = new HashMap<>(materialHolder.getMaterials());
		BarrelBlockItem.uncompactMaterials(originalMaterials);
		return DecorationHelper.getMaterialPartsNeeded(originalMaterials, materialsToApply);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack paintbrush, UseOnContext context) {
		if (!hasMainColor(paintbrush) && !hasAccentColor(paintbrush) && !hasBarrelMaterials(paintbrush)) {
			return InteractionResult.PASS;
		}

		Level level = context.getLevel();
		BlockEntity be = level.getBlockEntity(context.getClickedPos());
		if (be instanceof ControllerBlockEntity controllerBe) {
			if (hasBarrelMaterials(paintbrush) && context.getPlayer() != null && context.getPlayer().isCrouching()) {
				if (!level.isClientSide()) {
					paintSimpleMaterialHolder(context.getPlayer(), paintbrush, controllerBe, Vec3.atCenterOf(context.getClickedPos()), context.getClickedFace(),
							level.getBlockState(context.getClickedPos()).getSoundType(level, context.getClickedPos(), context.getPlayer()).getPlaceSound());
				}
			} else if (!level.isClientSide()) {
				paintConnectedStorages(context.getPlayer(), level, paintbrush, controllerBe);
			}
			return InteractionResult.SUCCESS;
		} else if (hasBarrelMaterials(paintbrush) && be instanceof ISimpleMaterialHolder simpleMaterialHolder) {
			if (!level.isClientSide()) {
				paintSimpleMaterialHolder(context.getPlayer(), paintbrush, simpleMaterialHolder, Vec3.atCenterOf(context.getClickedPos()),
						context.getClickedFace(),
						level.getBlockState(context.getClickedPos()).getSoundType(level, context.getClickedPos(), context.getPlayer()).getPlaceSound());
			}
			return InteractionResult.SUCCESS;
		} else if (be instanceof StorageBlockEntity storageBe) {
			if (!level.isClientSide()) {
				paintStorage(context.getPlayer(), paintbrush, storageBe, 1);
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private static void paintSimpleMaterialHolder(@Nullable Player player, ItemStack paintbrush, ISimpleMaterialHolder simpleMaterialHolder,
			Vec3 successEffectPos, Direction effectOffsetDirection, SoundEvent placeSound) {
		if (player == null) {
			return;
		}

		if (applySimpleMaterial(player, paintbrush, simpleMaterialHolder)) {
			playSoundAndParticles(player.level(), successEffectPos, 1f, placeSound, effectOffsetDirection);
		}
	}

	private void paintConnectedStorages(@Nullable Player player, Level level, ItemStack paintbrush, ControllerBlockEntity controllerBe) {
		if (player == null) {
			return;
		}

		for (BlockPos pos : controllerBe.getStoragePositions()) {
			WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(storageBe -> paintStorage(player, paintbrush, storageBe, 0.6f));
		}
	}

	private static void paintStorage(@Nullable Player player, ItemStack paintbrush, StorageBlockEntity storageBe, float soundVolume) {
		if (player == null) {
			return;
		}
		StorageBlockEntity renderUpdateBe = storageBe;
		ITintable tintable = storageBe.getStorageWrapper();
		IMaterialHolder materialHolder = storageBe instanceof IMaterialHolder ? (IMaterialHolder) storageBe : null;

		if (storageBe instanceof ChestBlockEntity chestBe) {
			tintable = chestBe.getMainStorageWrapper();
			storageBe = chestBe.getMainChestBlockEntity();
			renderUpdateBe = storageBe;
		}

		BlockState state = storageBe.getBlockState();
		Direction effectOffsetDirection = state.getBlock() instanceof StorageBlockBase storageBlock ? storageBlock.getFacing(state) : Direction.UP;
		if (paint(player, paintbrush, soundVolume, materialHolder, tintable, Vec3.atCenterOf(storageBe.getBlockPos()), effectOffsetDirection,
				state.getSoundType(player.level(), storageBe.getBlockPos(), null).getPlaceSound())) {
			renderUpdateBe.setUpdateBlockRender();
			WorldHelper.notifyBlockUpdate(renderUpdateBe);
		}
	}

	public static boolean paint(Player player, ItemStack paintbrush, @Nullable IMaterialHolder materialHolder, ITintable tintable, Vec3 successEffectPos,
			Direction effectOffsetDirection, SoundEvent placeSound) {
		return paint(player, paintbrush, 1f, materialHolder, tintable, successEffectPos, effectOffsetDirection, placeSound);
	}

	public static boolean paint(Player player, ItemStack paintbrush, float soundVolume, @Nullable IMaterialHolder materialHolder, ITintable tintable,
			Vec3 successEffectPos, Direction effectOffsetDirection, SoundEvent placeSound) {
		if (hasBarrelMaterials(paintbrush)) {
			if (materialHolder == null || !materialHolder.canHoldMaterials()) {
				return false;
			}

			if (applyMaterials(player, paintbrush, materialHolder, tintable)) {
				playSoundAndParticles(player.level(), successEffectPos, soundVolume, placeSound, effectOffsetDirection);
				return true;
			}
		} else {
			if (setColors(player, paintbrush, tintable, materialHolder)) {
				playSoundAndParticles(player.level(), successEffectPos, soundVolume, placeSound, effectOffsetDirection);
				return true;
			}
		}
		return false;
	}

	public static boolean setColors(Player player, ItemStack paintbrush, ITintable tintable, @Nullable IMaterialHolder materialHolder) {
		Map<Identifier, Integer> remainingParts = new HashMap<>(getRemainingParts(paintbrush));
		List<ResourceHandler<ItemResource>> itemHandlers = InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player);
		int mainColorToSet = getMainColor(paintbrush);
		int accentColorToSet = getAccentColor(paintbrush);

		int originalMainColor = tintable.getMainColor();
		int originalAccentColor = tintable.getAccentColor();

		if (originalMainColor == mainColorToSet && originalAccentColor == accentColorToSet) {
			return false;
		}

		try (Transaction tx = Transaction.openRoot()) {
			SnapshotJournal<Map<Identifier, Integer>> remainingPartsJournal = createRemainingPartsJournal(paintbrush);
			if (!DecorationHelper.consumeDyes(mainColorToSet, accentColorToSet, remainingParts, remainingPartsJournal, itemHandlers, originalMainColor,
					originalAccentColor, tx)) {
				return false;
			}
			tx.commit();
		}

		tintable.setColors(mainColorToSet, accentColorToSet);

		if (materialHolder != null) {
			materialHolder.setMaterials(Collections.emptyMap());
		}

		setRemainingParts(paintbrush, remainingParts);
		return true;
	}

	private static SnapshotJournal<Map<Identifier, Integer>> createRemainingPartsJournal(ItemStack paintbrush) {
		return new SnapshotJournal<>() {
			@Override
			protected Map<Identifier, Integer> createSnapshot() {
				return new HashMap<>(getRemainingParts(paintbrush));
			}

			@Override
			protected void revertToSnapshot(Map<Identifier, Integer> snapshot) {
				setRemainingParts(paintbrush, snapshot);
			}
		};
	}

	private static boolean applyMaterials(Player player, ItemStack paintbrush, IMaterialHolder materialHolder, ITintable tintable) {
		List<ResourceHandler<ItemResource>> itemHandlers = InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player);
		Map<Identifier, Integer> remainingParts = new HashMap<>(getRemainingParts(paintbrush));

		Map<BarrelMaterial, Identifier> originalMaterials = new HashMap<>(materialHolder.getMaterials());
		Map<BarrelMaterial, Identifier> materialsToApply = new HashMap<>(getBarrelMaterials(paintbrush));
		if (originalMaterials.equals(materialsToApply)) {
			return false;
		}

		BarrelBlockItem.uncompactMaterials(originalMaterials);

		try (Transaction tx = Transaction.openRoot()) {
			SnapshotJournal<Map<Identifier, Integer>> remainingPartsJournal = createRemainingPartsJournal(paintbrush);
			if (!DecorationHelper.consumeMaterials(remainingParts, remainingPartsJournal, itemHandlers, originalMaterials, materialsToApply, tx)) {
				return false;
			}
			tx.commit();
		}

		setRemainingParts(paintbrush, remainingParts);

		tintable.setColors(-1, -1);

		BarrelBlockItem.compactMaterials(materialsToApply);
		materialHolder.setMaterials(materialsToApply);
		return true;
	}

	private static boolean applySimpleMaterial(Player player, ItemStack paintbrush, ISimpleMaterialHolder simpleMaterialHolder) {
		Optional<Identifier> material = SimpleMaterialHelper.getSingleMaterial(getBarrelMaterials(paintbrush));
		if (material.isEmpty() || simpleMaterialHolder.getMaterial().filter(material.get()::equals).isPresent()) {
			return false;
		}

		List<ResourceHandler<ItemResource>> itemHandlers = InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player);
		Map<Identifier, Integer> remainingParts = new HashMap<>(getRemainingParts(paintbrush));
		try (Transaction tx = Transaction.openRoot()) {
			SnapshotJournal<Map<Identifier, Integer>> remainingPartsJournal = createRemainingPartsJournal(paintbrush);
			if (!DecorationHelper.consumeSimpleMaterial(remainingParts, remainingPartsJournal, itemHandlers, simpleMaterialHolder.getMaterial(), material.get(),
					tx)) {
				return false;
			}
			tx.commit();
		}

		setRemainingParts(paintbrush, remainingParts);
		simpleMaterialHolder.setMaterial(material.get());
		return true;
	}

	private static void playSoundAndParticles(Level level, Vec3 pos, float soundVolume, SoundEvent placeSound, Direction effectOffsetDirection) {
		level.playSound(null, pos.x(), pos.y(), pos.z(), placeSound, SoundSource.BLOCKS, soundVolume, 1);

		if (level instanceof ServerLevel serverLevel) {
			double x = pos.x() + 0.5D + effectOffsetDirection.getStepX() * 0.6D;
			double y = pos.y() + 0.5D + effectOffsetDirection.getStepY() * 0.6D;
			double z = pos.z() + 0.5D + effectOffsetDirection.getStepZ() * 0.6D;
			double xOffset;
			double yOffset;
			double zOffset;
			if (effectOffsetDirection.getAxis().isVertical()) {
				xOffset = 0.4D;
				yOffset = 0.1D;
				zOffset = 0.4D;
			} else {
				xOffset = 0.1D + effectOffsetDirection.getStepZ() * 0.3D;
				yOffset = 0.4D;
				zOffset = 0.1D + effectOffsetDirection.getStepX() * 0.3D;
			}

			serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 4, xOffset, yOffset, zOffset, 1f);
		}
	}

	private static void resetBarrelMaterials(ItemStack paintbrush) {
		paintbrush.remove(ModDataComponents.BARREL_MATERIALS);
	}

	public static void resetMainColor(ItemStack paintbrush) {
		paintbrush.remove(ModCoreDataComponents.MAIN_COLOR);
	}

	public static void resetAccentColor(ItemStack paintbrush) {
		paintbrush.remove(ModCoreDataComponents.ACCENT_COLOR);
	}

	public static void setMainColor(ItemStack paintbrush, int mainColor) {
		paintbrush.set(ModCoreDataComponents.MAIN_COLOR, mainColor);
		resetBarrelMaterials(paintbrush);
	}

	public static void setAccentColor(ItemStack paintbrush, int secondaryColor) {
		paintbrush.set(ModCoreDataComponents.ACCENT_COLOR, secondaryColor);
		resetBarrelMaterials(paintbrush);
	}

	public static void setRemainingParts(ItemStack paintbrush, Map<Identifier, Integer> remainingParts) {
		paintbrush.set(ModDataComponents.REMAINING_PARTS, remainingParts);
	}

	public static Map<Identifier, Integer> getRemainingParts(ItemStack paintbrush) {
		return paintbrush.getOrDefault(ModDataComponents.REMAINING_PARTS, Collections.emptyMap());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
		StorageTranslationHelper.INSTANCE
				.getTranslatedLines(StorageTranslationHelper.INSTANCE.translItemTooltip(stack.getItem()), null, ChatFormatting.DARK_GRAY).forEach(tooltipAdder);

		if (hasBarrelMaterials(stack)) {
			tooltipAdder.accept(
					Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("paintbrush") + ".materials").withStyle(ChatFormatting.GRAY));
			Map<BarrelMaterial, Identifier> barrelMaterials = getBarrelMaterials(stack);
			barrelMaterials.forEach((barrelMaterial, blockName) -> {
				BuiltInRegistries.BLOCK.getOptional(blockName).ifPresent(block -> {
					tooltipAdder.accept(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("paintbrush") + ".material",
							Component.translatable(StorageTranslationHelper.INSTANCE.translGui("barrel_part." + barrelMaterial.getSerializedName())),
							block.getName().withStyle(ChatFormatting.DARK_AQUA)).withStyle(ChatFormatting.GRAY));
				});
			});
		}

		if (hasMainColor(stack)) {
			int mainColor = getMainColor(stack);
			tooltipAdder
					.accept(Component
							.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("paintbrush") + ".main_color",
									Component.literal(ColorHelper.getHexColor(mainColor)).withStyle(Style.EMPTY.withColor(mainColor)))
							.withStyle(ChatFormatting.GRAY));
		}

		if (hasAccentColor(stack)) {
			int accentColor = getAccentColor(stack);
			tooltipAdder.accept(Component
					.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("paintbrush") + ".accent_color",
							Component.literal(ColorHelper.getHexColor(accentColor)).withStyle(Style.EMPTY.withColor(accentColor)))
					.withStyle(ChatFormatting.GRAY));
		}
	}

	private static boolean hasMainColor(ItemStack paintbrush) {
		return paintbrush.has(ModCoreDataComponents.MAIN_COLOR);
	}

	private static boolean hasAccentColor(ItemStack paintbrush) {
		return paintbrush.has(ModCoreDataComponents.ACCENT_COLOR);
	}

	private static boolean hasBarrelMaterials(ItemStack paintbrush) {
		return paintbrush.has(ModDataComponents.BARREL_MATERIALS);
	}

	public static int getMainColor(ItemStack paintbrush) {
		return paintbrush.getOrDefault(ModCoreDataComponents.MAIN_COLOR, -1);
	}

	public static int getAccentColor(ItemStack paintbrush) {
		return paintbrush.getOrDefault(ModCoreDataComponents.ACCENT_COLOR, -1);
	}

	public static Map<BarrelMaterial, Identifier> getBarrelMaterials(ItemStack paintbrush) {
		return paintbrush.getOrDefault(ModDataComponents.BARREL_MATERIALS, Collections.emptyMap());
	}

	public record ItemRequirements(List<ItemStack> itemsPresent, List<ItemStack> itemsMissing) {
	}
}
