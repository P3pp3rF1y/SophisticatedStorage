package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.Variant;
import net.minecraft.client.data.models.blockstates.VariantProperties;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import net.p3pp3rf1y.sophisticatedcore.data.SophisticatedModelProvider;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConversionItem;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.init.StorageTintSources;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelItemModel;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ChestItemRenderer;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ShulkerBoxItemRenderer;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StorageModelProvider extends SophisticatedModelProvider {
	private static final ModelTemplate EMPTY_MODEL_TEMPLATE = ModelTemplates.create();
	private static final ResourceLocation BASE_CHEST_PARTICLE = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "block/break/acacia_chest");
	private static final ResourceLocation BASE_SHULKER_BOX_PARTICLE = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID,
			"block/break/shulker_box");

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

		generateBlockWithCustomLoader(blockModels, "chest", ModBlocks.CHEST.get(), ModelTemplates.CHEST_INVENTORY, BASE_CHEST_PARTICLE, ChestBlock.class,
				new ChestItemRenderer.Unbaked());
		generateBlockWithCustomLoader(blockModels, "shulker_box", ModBlocks.SHULKER_BOX.get(), ModelTemplates.SHULKER_BOX_INVENTORY, BASE_SHULKER_BOX_PARTICLE,
				ShulkerBoxBlock.class, new ShulkerBoxItemRenderer.Unbaked());
		generateCubeBottomTopReuseTopOnBottom(blockModels, ModBlocks.CONTROLLER.get());
		generateCubeBottomTopReuseTopOnBottom(blockModels, ModBlocks.STORAGE_IO.get());
		generateCubeBottomTopReuseTopOnBottom(blockModels, ModBlocks.STORAGE_INPUT.get());
		generateCubeBottomTopReuseTopOnBottom(blockModels, ModBlocks.STORAGE_OUTPUT.get());
		ModBlocks.STORAGE_CONNECTOR_BLOCKS.values().stream().map(Supplier::get).forEach(blockModels::createTrivialCube);

		generateCustomModelBlock(blockModels, ModBlocks.DECORATION_TABLE.get(), BlockModelGenerators.createHorizontalFacingDispatch());
		generateCustomModelBlock(blockModels, ModBlocks.STORAGE_LINK.get(), blockModels.createColumnWithFacing());
	}

	private void generateBlockWithCustomLoader(BlockModelGenerators blockModels, String loaderName, Block baseBlock, ModelTemplate itemModelTemplate,
			ResourceLocation baseParticle, Class<? extends Block> blockClass, SpecialModelRenderer.Unbaked unbakedSpecialRenderer) {
		TexturedModel.Provider provider = TexturedModel.createDefault(b -> new TextureMapping(),
				ExtendedModelTemplateBuilder.builder().customLoader(() -> createSimpleCustomLoaderBuilder(loaderName), loader -> {
				}).build());
		ResourceLocation blockModel = provider.create(baseBlock, blockModels.modelOutput);
		ResourceLocation itemModel = itemModelTemplate.create(baseBlock.asItem(), TextureMapping.particle(baseParticle), blockModels.modelOutput);
		BuiltInRegistries.BLOCK.entrySet().stream()
				.filter(entry -> entry.getKey().location().getNamespace().equals(modId) && blockClass.isAssignableFrom(entry.getValue().getClass()))
				.forEach(entry -> {
					Block block = entry.getValue();
					generateForBlock(blockModels, block, itemModel, blockModel, unbakedSpecialRenderer);
				});
	}

	private CustomLoaderBuilder createSimpleCustomLoaderBuilder(String name) {
		return new CustomLoaderBuilder(ResourceLocation.fromNamespaceAndPath(modId, name), false) {
			@Override
			protected CustomLoaderBuilder copyInternal() {
				return createSimpleCustomLoaderBuilder(name);
			}
		};
	}

	private void generateForBlock(BlockModelGenerators blockModels, Block block, ResourceLocation itemModel, ResourceLocation blockModel,
			SpecialModelRenderer.Unbaked unbakedSpecialRenderer) {
		blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, blockModel));
		Item item = block.asItem();
		ItemModel.Unbaked unbakedItemModel = ItemModelUtils.specialModel(itemModel, unbakedSpecialRenderer);
		blockModels.itemModelOutput.accept(item, unbakedItemModel);
	}

	private void generateBarrel(BlockModelGenerators blockModels, Item item) {
		if (item instanceof BarrelBlockItem barrelBlockItem) {
			Block block = barrelBlockItem.getBlock();
			ResourceLocation blockModelId = ModelLocationUtils.getModelLocation(block);
			ResourceLocation flatTopBlockModelId = getFlatTopModelLocation(block);
			MultiVariantGenerator multiVariantGenerator = MultiVariantGenerator.multiVariant(block);
			if (block instanceof LimitedBarrelBlock) {
				multiVariantGenerator.with(BlockModelGenerators.createHorizontalFacingDispatch());
				multiVariantGenerator.with(createVerticalFacingDispatch());
			} else {
				multiVariantGenerator.with(blockModels.createColumnWithFacing());
			}
			blockModels.blockStateOutput.accept(multiVariantGenerator
					.with(PropertyDispatch.property(BarrelBlock.FLAT_TOP).select(false, Variant.variant().with(VariantProperties.MODEL, blockModelId))
							.select(true, Variant.variant().with(VariantProperties.MODEL, flatTopBlockModelId))));

			blockModels.itemModelOutput.accept(item, new BarrelItemModel.Unbaked(blockModelId, flatTopBlockModelId,
					List.of(new StorageTintSources.Main(-1), new StorageTintSources.Accent(-1))));
		}
	}

	private static void generateCustomModelBlock(BlockModelGenerators blockModels, Block block, PropertyDispatch facingPropertyDispatch) {
		ResourceLocation blockModelId = ModelLocationUtils.getModelLocation(block);
		blockModels.blockStateOutput
				.accept(MultiVariantGenerator.multiVariant(block, Variant.variant().with(VariantProperties.MODEL, blockModelId)).with(facingPropertyDispatch));
		blockModels.itemModelOutput.accept(block.asItem(), ItemModelUtils.plainModel(blockModelId));
	}

	private static PropertyDispatch createVerticalFacingDispatch() {
		return PropertyDispatch.property(LimitedBarrelBlock.VERTICAL_FACING)
				.select(VerticalFacing.NO, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
				.select(VerticalFacing.DOWN, Variant.variant().with(VariantProperties.X_ROT, VariantProperties.Rotation.R180))
				.select(VerticalFacing.UP, Variant.variant());
	}

	private static ResourceLocation getFlatTopModelLocation(Block block) {
		ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(block);
		return resourcelocation.withPrefix("block/flat/");
	}

	private void generateItemModels(ItemModelGenerators itemModels) {
		List<Item> flatItems = new ArrayList<>();

		addItemClasses(flatItems, List.of(UpgradeItemBase.class, StackUpgradeConversionItem.class, StorageTierUpgradeItem.class));
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
