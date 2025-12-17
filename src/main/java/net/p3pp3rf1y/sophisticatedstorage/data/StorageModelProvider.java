package net.p3pp3rf1y.sophisticatedstorage.data;

import com.mojang.math.Quadrant;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import net.p3pp3rf1y.sophisticatedcore.data.SophisticatedModelProvider;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.init.StorageTintSources;
import net.p3pp3rf1y.sophisticatedstorage.client.render.*;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.client.data.models.BlockModelGenerators.NOP;

public class StorageModelProvider extends SophisticatedModelProvider {
	private static final Identifier BASE_CHEST_PARTICLE = Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "block/break/acacia_chest");
	private static final Identifier BASE_SHULKER_BOX_PARTICLE = Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "block/break/shulker_box");
	private static final PropertyDispatch<VariantMutator> VERTICAL_FACING = PropertyDispatch.modify(LimitedBarrelBlock.VERTICAL_FACING)
			.select(VerticalFacing.NO, VariantMutator.X_ROT.withValue(Quadrant.R90))
			.select(VerticalFacing.DOWN, VariantMutator.X_ROT.withValue(Quadrant.R180))
			.select(VerticalFacing.UP, NOP);

	public StorageModelProvider(PackOutput output) {
		super(output, SophisticatedStorage.MOD_ID);
	}

	@Override
	protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
		generateBlockModels(blockModels);
		generateItemModels(itemModels);
	}

	private void generateBlockModels(BlockModelGenerators blockModels) {
		List<Item> barrelItems = new ArrayList<>();
		addItemClasses(barrelItems, List.of(BarrelBlockItem.class));
		barrelItems.forEach(item -> generateBarrel(blockModels, item));

		generateBlockWithCustomBlockStateModel(blockModels, "chest", ModBlocks.CHEST.get(), ModelTemplates.CHEST_INVENTORY, BASE_CHEST_PARTICLE, ChestBlock.class, new ChestItemRenderer.Unbaked(), ChestBlockStateModel.Unbaked::new);
		generateBlockWithCustomBlockStateModel(blockModels, "shulker_box", ModBlocks.SHULKER_BOX.get(), ModelTemplates.SHULKER_BOX_INVENTORY, BASE_SHULKER_BOX_PARTICLE, ShulkerBoxBlock.class, new ShulkerBoxItemRenderer.Unbaked(), ShulkerBoxBlockStateModel.Unbaked::new);
		generateCubeBottomTopReuseTopOnBottom(blockModels, ModBlocks.CONTROLLER.get());
		generateCubeBottomTopReuseTopOnBottom(blockModels, ModBlocks.STORAGE_IO.get());
		generateCubeBottomTopReuseTopOnBottom(blockModels, ModBlocks.STORAGE_INPUT.get());
		generateCubeBottomTopReuseTopOnBottom(blockModels, ModBlocks.STORAGE_OUTPUT.get());
		ModBlocks.STORAGE_CONNECTOR_BLOCKS.values().stream().map(Supplier::get).forEach(blockModels::createTrivialCube);

		generateCustomModelBlock(blockModels, ModBlocks.DECORATION_TABLE.get(), BlockModelGenerators.ROTATION_HORIZONTAL_FACING);
		generateCustomModelBlock(blockModels, ModBlocks.STORAGE_LINK.get(), BlockModelGenerators.ROTATIONS_COLUMN_WITH_FACING);
	}

	private void generateBlockWithCustomBlockStateModel(BlockModelGenerators blockModels, String loaderName, Block baseBlock, ModelTemplate itemModelTemplate, Identifier baseParticle, Class<? extends Block> blockClass, SpecialModelRenderer.Unbaked unbakedSpecialRenderer, Supplier<CustomUnbakedBlockStateModel> unbakedBlockStateModelSupplier) {
		TexturedModel.Provider provider = TexturedModel.createDefault(b -> new TextureMapping(),
				ExtendedModelTemplateBuilder.builder().customLoader(() -> createSimpleCustomLoaderBuilder(loaderName), loader -> {
				}).build()
		);
		Identifier itemModel = itemModelTemplate.create(baseBlock.asItem(), TextureMapping.particle(baseParticle), blockModels.modelOutput);
		BuiltInRegistries.BLOCK.entrySet().stream()
				.filter(entry -> entry.getKey().identifier().getNamespace().equals(modId)
						&& blockClass.isAssignableFrom(entry.getValue().getClass()))
				.forEach(entry -> {
					Block block = entry.getValue();
					generateForCustomBlockStateModelBlock(blockModels, block, itemModel, unbakedBlockStateModelSupplier, unbakedSpecialRenderer);
				});
	}

	private CustomLoaderBuilder createSimpleCustomLoaderBuilder(String name) {
		return new CustomLoaderBuilder(Identifier.fromNamespaceAndPath(modId, name), false) {
			@Override
			protected CustomLoaderBuilder copyInternal() {
				return createSimpleCustomLoaderBuilder(name);
			}
		};
	}

	private void generateForCustomBlockStateModelBlock(BlockModelGenerators blockModels, Block block, Identifier itemModel, Supplier<CustomUnbakedBlockStateModel> unbakedBlockStateModelSupplier, SpecialModelRenderer.Unbaked unbakedSpecialRenderer) {
		blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, MultiVariant.of(new UnitBlockStateModelBuilder() {
			@Override
			public CustomUnbakedBlockStateModel toUnbaked() {
				return unbakedBlockStateModelSupplier.get();
			}
		})));
		Item item = block.asItem();
		ItemModel.Unbaked unbakedItemModel = ItemModelUtils.specialModel(itemModel, unbakedSpecialRenderer);
		blockModels.itemModelOutput.accept(item, unbakedItemModel);
	}

	private void generateBarrel(BlockModelGenerators blockModels, Item item) {
		if (item instanceof BarrelBlockItem barrelBlockItem) {
			Block block = barrelBlockItem.getBlock();
			Identifier blockModelId = ModelLocationUtils.getModelLocation(block);
			Identifier flatTopBlockModelId = getFlatTopModelLocation(block);
			MultiVariantGenerator multiVariantGenerator = MultiVariantGenerator.dispatch(block)
					.with(
							PropertyDispatch.initial(BarrelBlock.FLAT_TOP)
									.select(false, MultiVariant.of(new BarrelBlockStateModelBuilder(blockModelId)))
									.select(true, MultiVariant.of(new BarrelBlockStateModelBuilder(flatTopBlockModelId)))
					);
			if (block instanceof LimitedBarrelBlock) {
				multiVariantGenerator = multiVariantGenerator.with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING).with(VERTICAL_FACING);
			} else {
				multiVariantGenerator = multiVariantGenerator.with(BlockModelGenerators.ROTATIONS_COLUMN_WITH_FACING);
			}
			blockModels.blockStateOutput.accept(multiVariantGenerator);

			blockModels.itemModelOutput.accept(item, new BarrelItemModel.Unbaked(
					blockModelId,
					flatTopBlockModelId,
					List.of(new StorageTintSources.Main(-1), new StorageTintSources.Accent(-1))
			));
		}
	}

	private static void generateCustomModelBlock(BlockModelGenerators blockModels, Block block, PropertyDispatch<VariantMutator> facingPropertyDispatch) {
		Identifier blockModelId = ModelLocationUtils.getModelLocation(block);
		blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(blockModelId)).with(facingPropertyDispatch));
		blockModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(blockModelId));
	}

	private static Identifier getFlatTopModelLocation(Block block) {
		Identifier identifier = BuiltInRegistries.BLOCK.getKey(block);
		return identifier.withPrefix("block/flat/");
	}

	private void generateItemModels(ItemModelGenerators itemModels) {
		List<Item> flatItems = new ArrayList<>();

		addItemClasses(flatItems, List.of(UpgradeItemBase.class, StorageTierUpgradeItem.class));
		flatItems.add(ModItems.DEBUG_TOOL.get());
		flatItems.add(ModItems.PACKING_TAPE.get());
		flatItems.add(ModItems.SUPER_PACKING_TAPE.get());
		flatItems.add(ModItems.UPGRADE_BASE.get());
		flatItems.add(ModItems.INACCESSIBLE_SLOT.get());

		flatItems.forEach(item -> itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM));

		itemModels.generateFlatItem(ModItems.STORAGE_TOOL.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
		itemModels.generateFlatItem(ModItems.PAINTBRUSH.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
	}
}
