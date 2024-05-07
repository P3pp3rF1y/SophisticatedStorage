package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ItemExistsCondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.chipped.BlockTransformationUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.crafting.ShapeBasedRecipeBuilder;
import net.p3pp3rf1y.sophisticatedcore.crafting.ShapelessBasedRecipeBuilder;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.compat.chipped.ChippedCompat;
import net.p3pp3rf1y.sophisticatedstorage.crafting.*;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.function.Consumer;

public class StorageRecipeProvider extends RecipeProvider {
	private static final String HAS_UPGRADE_BASE_CRITERION_NAME = "has_upgrade_base";
	private static final String HAS_REDSTONE_TORCH_CRITERION_NAME = "has_redstone_torch";
	private static final String HAS_SMELTING_UPGRADE_CRITERION_NAME = "has_smelting_upgrade";
	public static final String HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME = "has_base_tier_wooden_storage";
	private static final String PLANK_SUFFIX = "_plank";

	public StorageRecipeProvider(DataGenerator generator) {
		super(generator.getPackOutput());
	}

	@Override
	protected void buildRecipes(RecipeOutput recipeOutput) {
		SpecialRecipeBuilder.special(StorageDyeRecipe::new).save(recipeOutput, SophisticatedStorage.getRegistryName("storage_dye"));
		SpecialRecipeBuilder.special(FlatTopBarrelToggleRecipe::new).save(recipeOutput, SophisticatedStorage.getRegistryName("flat_top_barrel_toggle"));
		SpecialRecipeBuilder.special(BarrelMaterialRecipe::new).save(recipeOutput, SophisticatedStorage.getRegistryName("barrel_material"));

		addBarrelRecipes(recipeOutput);
		addLimitedBarrelRecipes(recipeOutput);
		addChestRecipes(recipeOutput);
		addShulkerBoxRecipes(recipeOutput);
		addControllerRelatedRecipes(recipeOutput);
		addUpgradeRecipes(recipeOutput);
		addTierUpgradeItemRecipes(recipeOutput);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PACKING_TAPE.get())
				.requires(Tags.Items.SLIMEBALLS)
				.requires(Items.PAPER)
				.unlockedBy("has_slime", has(Tags.Items.SLIMEBALLS))
				.save(recipeOutput.withConditions(new DropPackedDisabledCondition()));
	}

	private void addLimitedBarrelRecipes(RecipeOutput recipeOutput) {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach((woodType, blockFamily) -> {
			limitedWoodBarrel1Recipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB));
			limitedWoodBarrel2Recipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB));
			limitedWoodBarrel3Recipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB));
			limitedWoodBarrel4Recipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB));
		});

		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.LIMITED_BARREL_1_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get());
		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.LIMITED_BARREL_2_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get());
		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.LIMITED_BARREL_3_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get());
		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.LIMITED_BARREL_4_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get());
	}

	private void addStorageTierUpgradeRecipes(RecipeOutput recipeOutput, BlockItem baseTierItem, BlockItem copperTierItem, BlockItem ironTierItem, BlockItem goldTierItem, BlockItem diamondTierItem, BlockItem netheriteTierItem) {
		ShapeBasedRecipeBuilder.shaped(copperTierItem, StorageTierUpgradeRecipe::new)
				.pattern("CCC")
				.pattern("CSC")
				.pattern("CCC")
				.define('C', Tags.Items.INGOTS_COPPER)
				.define('S', baseTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(baseTierItem).getPath(), has(baseTierItem))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ironTierItem, StorageTierUpgradeRecipe::new)
				.pattern(" I ")
				.pattern("ISI")
				.pattern(" I ")
				.define('I', Tags.Items.INGOTS_IRON)
				.define('S', copperTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(copperTierItem).getPath(), has(copperTierItem))
				.save(recipeOutput, SophisticatedStorage.getRL(RegistryHelper.getItemKey(ironTierItem).getPath() + "_from_" + RegistryHelper.getItemKey(copperTierItem).getPath()));

		ShapeBasedRecipeBuilder.shaped(ironTierItem, StorageTierUpgradeRecipe::new)
				.pattern("III")
				.pattern("ISI")
				.pattern("III")
				.define('I', Tags.Items.INGOTS_IRON)
				.define('S', baseTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(baseTierItem).getPath(), has(baseTierItem))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(goldTierItem, StorageTierUpgradeRecipe::new)
				.pattern("GGG")
				.pattern("GSG")
				.pattern("GGG")
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('S', ironTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(ironTierItem).getPath(), has(ironTierItem))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(diamondTierItem, StorageTierUpgradeRecipe::new)
				.pattern("DDD")
				.pattern("DSD")
				.pattern("DDD")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('S', goldTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(goldTierItem).getPath(), has(goldTierItem))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(netheriteTierItem, StorageTierUpgradeShapelessRecipe::new)
				.requires(Ingredient.of(diamondTierItem))
				.requires(Tags.Items.INGOTS_NETHERITE)
				.unlockedBy("has_" + RegistryHelper.getItemKey(diamondTierItem).getPath(), has(diamondTierItem))
				.save(recipeOutput, RegistryHelper.getItemKey(netheriteTierItem));
	}

	private void addControllerRelatedRecipes(RecipeOutput recipeOutput) {
		ShapeBasedRecipeBuilder.shaped(ModBlocks.CONTROLLER_ITEM.get())
				.pattern("SCS")
				.pattern("PBP")
				.pattern("SCS")
				.define('S', Tags.Items.STONE)
				.define('C', Items.COMPARATOR)
				.define('P', ItemTags.PLANKS)
				.define('B', new BaseTierWoodenStorageIngredient())
				.unlockedBy(HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME, has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(ModBlocks.STORAGE_LINK_ITEM.get(), 3)
				.requires(ModBlocks.CONTROLLER_ITEM.get())
				.requires(Tags.Items.ENDER_PEARLS)
				.unlockedBy("has_controller", has(ModBlocks.CONTROLLER_ITEM.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("storage_link_from_controller"));

		ShapeBasedRecipeBuilder.shaped(ModBlocks.STORAGE_LINK_ITEM.get())
				.pattern("EP")
				.pattern("RS")
				.define('E', Tags.Items.ENDER_PEARLS)
				.define('P', ItemTags.PLANKS)
				.define('R', Items.REPEATER)
				.define('S', Tags.Items.STONE)
				.unlockedBy("has_repeater", has(Items.REPEATER))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.STORAGE_TOOL.get())
				.pattern(" EI")
				.pattern(" SR")
				.pattern("S  ")
				.define('E', Tags.Items.ENDER_PEARLS)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('S', Tags.Items.RODS_WOODEN)
				.define('R', Items.REDSTONE_TORCH)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.STORAGE_IO_ITEM.get())
				.pattern("SPS")
				.pattern("RBG")
				.pattern("SPS")
				.define('S', Tags.Items.STONE)
				.define('P', ItemTags.PLANKS)
				.define('R', Items.REPEATER)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('B',  new BaseTierWoodenStorageIngredient())
				.unlockedBy(HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME, has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.STORAGE_OUTPUT_ITEM.get())
				.pattern("SGS")
				.pattern("PBP")
				.pattern("SRS")
				.define('S', Tags.Items.STONE)
				.define('P', ItemTags.PLANKS)
				.define('R', Items.REPEATER)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('B',  new BaseTierWoodenStorageIngredient())
				.unlockedBy(HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME, has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.STORAGE_INPUT_ITEM.get())
				.pattern("SRS")
				.pattern("PBP")
				.pattern("SGS")
				.define('S', Tags.Items.STONE)
				.define('P', ItemTags.PLANKS)
				.define('R', Items.REPEATER)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('B',  new BaseTierWoodenStorageIngredient())
				.unlockedBy(HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME, has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(ModBlocks.STORAGE_INPUT_ITEM.get())
				.requires(ModBlocks.STORAGE_IO_ITEM.get())
				.unlockedBy("has_storage_io", has(ModBlocks.STORAGE_IO_ITEM.get()))
				.save(recipeOutput, "storage_input_from_io");

		ShapelessBasedRecipeBuilder.shapeless(ModBlocks.STORAGE_OUTPUT_ITEM.get())
				.requires(ModBlocks.STORAGE_INPUT_ITEM.get())
				.unlockedBy("has_storage_input", has(ModBlocks.STORAGE_INPUT_ITEM.get()))
				.save(recipeOutput, "storage_output_from_input");

		ShapelessBasedRecipeBuilder.shapeless(ModBlocks.STORAGE_IO_ITEM.get())
				.requires(ModBlocks.STORAGE_OUTPUT_ITEM.get())
				.unlockedBy("has_storage_output", has(ModBlocks.STORAGE_OUTPUT_ITEM.get()))
				.save(recipeOutput, "storage_io_from_output");
	}

	private void addShulkerBoxRecipes(RecipeOutput recipeOutput) {
		ShapeBasedRecipeBuilder.shaped(ModBlocks.SHULKER_BOX_ITEM.get())
				.pattern(" S")
				.pattern("RC")
				.pattern(" S")
				.define('R', Items.REDSTONE_TORCH)
				.define('S', Items.SHULKER_SHELL)
				.define('C', Tags.Items.CHESTS)
				.unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(ModBlocks.SHULKER_BOX_ITEM.get())
				.requires(Items.SHULKER_BOX).requires(Items.REDSTONE_TORCH)
				.unlockedBy("has_shulker_box", has(Items.SHULKER_BOX))
				.save(recipeOutput, "shulker_box_from_vanilla_shulker_box");

		tintedShulkerBoxRecipe(recipeOutput, Blocks.BLACK_SHULKER_BOX, DyeColor.BLACK);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.BLUE_SHULKER_BOX, DyeColor.BLUE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.BROWN_SHULKER_BOX, DyeColor.BROWN);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.CYAN_SHULKER_BOX, DyeColor.CYAN);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.GRAY_SHULKER_BOX, DyeColor.GRAY);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.GREEN_SHULKER_BOX, DyeColor.GREEN);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.LIGHT_BLUE_SHULKER_BOX, DyeColor.LIGHT_BLUE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.LIGHT_GRAY_SHULKER_BOX, DyeColor.LIGHT_GRAY);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.LIME_SHULKER_BOX, DyeColor.LIME);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.MAGENTA_SHULKER_BOX, DyeColor.MAGENTA);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.ORANGE_SHULKER_BOX, DyeColor.ORANGE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.PINK_SHULKER_BOX, DyeColor.PINK);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.PURPLE_SHULKER_BOX, DyeColor.PURPLE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.RED_SHULKER_BOX, DyeColor.RED);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.WHITE_SHULKER_BOX, DyeColor.WHITE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.YELLOW_SHULKER_BOX, DyeColor.YELLOW);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new)
				.pattern("S")
				.pattern("C")
				.pattern("S")
				.define('C', ModBlocks.CHEST_ITEM.get())
				.define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_chest", has(ModBlocks.CHEST_ITEM.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("shulker_from_chest"));

		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.SHULKER_BOX_ITEM.get(), ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), ModBlocks.IRON_SHULKER_BOX_ITEM.get(), ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get());

		ShapeBasedRecipeBuilder.shaped(ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new)
				.pattern("S")
				.pattern("C")
				.pattern("S")
				.define('C', ModBlocks.COPPER_CHEST_ITEM.get())
				.define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_copper_chest", has(ModBlocks.COPPER_CHEST_ITEM.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("copper_shulker_from_copper_chest"));


		ShapeBasedRecipeBuilder.shaped(ModBlocks.IRON_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new)
				.pattern("S")
				.pattern("C")
				.pattern("S")
				.define('C', ModBlocks.IRON_CHEST_ITEM.get())
				.define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_iron_chest", has(ModBlocks.IRON_CHEST_ITEM.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("iron_shulker_from_iron_chest"));

		ShapeBasedRecipeBuilder.shaped(ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new)
				.pattern("S")
				.pattern("C")
				.pattern("S")
				.define('C', ModBlocks.GOLD_CHEST_ITEM.get())
				.define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_gold_chest", has(ModBlocks.GOLD_CHEST_ITEM.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("gold_shulker_from_gold_chest"));

		ShapeBasedRecipeBuilder.shaped(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new)
				.pattern("S")
				.pattern("C")
				.pattern("S")
				.define('C', ModBlocks.DIAMOND_CHEST_ITEM.get())
				.define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_diamond_chest", has(ModBlocks.DIAMOND_CHEST_ITEM.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("diamond_shulker_from_diamond_chest"));

		ShapeBasedRecipeBuilder.shaped(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new)
				.pattern("S")
				.pattern("C")
				.pattern("S")
				.define('C', ModBlocks.NETHERITE_CHEST_ITEM.get())
				.define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_netherite_chest", has(ModBlocks.NETHERITE_CHEST_ITEM.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("netherite_shulker_from_netherite_chest"));
	}

	private void addTierUpgradeItemRecipes(RecipeOutput recipeOutput) {
		ShapeBasedRecipeBuilder.shaped(ModItems.BASIC_TIER_UPGRADE.get())
				.pattern(" S ")
				.pattern("SRS")
				.pattern(" S ")
				.define('R', Items.REDSTONE_TORCH)
				.define('S', Items.STICK)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.BASIC_TO_COPPER_TIER_UPGRADE.get())
				.pattern("CCC")
				.pattern("CRC")
				.pattern("CCC")
				.define('R', Items.REDSTONE_TORCH)
				.define('C', Tags.Items.INGOTS_COPPER)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.BASIC_TO_IRON_TIER_UPGRADE.get())
				.pattern("III")
				.pattern("IRI")
				.pattern("III")
				.define('R', Items.REDSTONE_TORCH)
				.define('I', Tags.Items.INGOTS_IRON)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.BASIC_TO_GOLD_TIER_UPGRADE.get())
				.pattern("GGG")
				.pattern("GTG")
				.pattern("GGG")
				.define('T', ModItems.BASIC_TO_IRON_TIER_UPGRADE.get())
				.define('G', Tags.Items.INGOTS_GOLD)
				.unlockedBy("has_basic_to_iron_tier_upgrade", has(ModItems.BASIC_TO_IRON_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get())
				.pattern("DDD")
				.pattern("DTD")
				.pattern("DDD")
				.define('T', ModItems.BASIC_TO_GOLD_TIER_UPGRADE.get())
				.define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy("has_basic_to_gold_tier_upgrade", has(ModItems.BASIC_TO_GOLD_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(ModItems.BASIC_TO_NETHERITE_TIER_UPGRADE.get())
				.requires(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get())
				.requires(Tags.Items.INGOTS_NETHERITE)
				.unlockedBy("has_basic_to_diamond_tier_upgrade", has(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.COPPER_TO_IRON_TIER_UPGRADE.get())
				.pattern(" I ")
				.pattern("IRI")
				.pattern(" I ")
				.define('R', Items.REDSTONE_TORCH)
				.define('I', Tags.Items.INGOTS_IRON)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.COPPER_TO_GOLD_TIER_UPGRADE.get())
				.pattern("GGG")
				.pattern("GTG")
				.pattern("GGG")
				.define('T', ModItems.COPPER_TO_IRON_TIER_UPGRADE.get())
				.define('G', Tags.Items.INGOTS_GOLD)
				.unlockedBy("has_copper_to_iron_tier_upgrade", has(ModItems.COPPER_TO_IRON_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.COPPER_TO_DIAMOND_TIER_UPGRADE.get())
				.pattern("DDD")
				.pattern("DTD")
				.pattern("DDD")
				.define('T', ModItems.COPPER_TO_GOLD_TIER_UPGRADE.get())
				.define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy("has_copper_to_gold_tier_upgrade", has(ModItems.COPPER_TO_GOLD_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(ModItems.COPPER_TO_NETHERITE_TIER_UPGRADE.get())
				.requires(ModItems.COPPER_TO_DIAMOND_TIER_UPGRADE.get())
				.requires(Tags.Items.INGOTS_NETHERITE)
				.unlockedBy("has_copper_to_diamond_tier_upgrade", has(ModItems.COPPER_TO_DIAMOND_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.IRON_TO_GOLD_TIER_UPGRADE.get())
				.pattern("GGG")
				.pattern("GRG")
				.pattern("GGG")
				.define('R', Items.REDSTONE_TORCH)
				.define('G', Tags.Items.INGOTS_GOLD)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.IRON_TO_DIAMOND_TIER_UPGRADE.get())
				.pattern("DDD")
				.pattern("DTD")
				.pattern("DDD")
				.define('T', ModItems.IRON_TO_GOLD_TIER_UPGRADE.get())
				.define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy("has_iron_to_gold_tier_upgrade", has(ModItems.IRON_TO_GOLD_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(ModItems.IRON_TO_NETHERITE_TIER_UPGRADE.get())
				.requires(ModItems.IRON_TO_DIAMOND_TIER_UPGRADE.get())
				.requires(Tags.Items.INGOTS_NETHERITE)
				.unlockedBy("has_iron_to_diamond_tier_upgrade", has(ModItems.IRON_TO_DIAMOND_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get())
				.pattern("DDD")
				.pattern("DRD")
				.pattern("DDD")
				.define('R', Items.REDSTONE_TORCH)
				.define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(ModItems.GOLD_TO_NETHERITE_TIER_UPGRADE.get())
				.requires(ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get())
				.requires(Tags.Items.INGOTS_NETHERITE)
				.unlockedBy("has_gold_to_diamond_tier_upgrade", has(ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(ModItems.DIAMOND_TO_NETHERITE_TIER_UPGRADE.get())
				.requires(Items.REDSTONE_TORCH)
				.requires(Tags.Items.INGOTS_NETHERITE)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(recipeOutput);
	}

	private void addUpgradeRecipes(RecipeOutput recipeOutput) {
		ShapeBasedRecipeBuilder.shaped(ModItems.UPGRADE_BASE.get())
				.pattern("PIP")
				.pattern("IPI")
				.pattern("PIP")
				.define('P', ItemTags.PLANKS)
				.define('I', Tags.Items.INGOTS_IRON)
				.unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.PICKUP_UPGRADE.get())
				.pattern(" P ")
				.pattern("LBL")
				.pattern("RRR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('L', ItemTags.PLANKS)
				.define('P', Blocks.STICKY_PISTON)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_PICKUP_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern(" D ")
				.pattern("GPG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('P', ModItems.PICKUP_UPGRADE.get())
				.unlockedBy("has_pickup_upgrade", has(ModItems.PICKUP_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.FILTER_UPGRADE.get())
				.pattern("RSR")
				.pattern("SBS")
				.pattern("RSR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('S', Tags.Items.STRING)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_FILTER_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern("GPG")
				.pattern("RRR")
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('P', ModItems.FILTER_UPGRADE.get())
				.unlockedBy("has_filter_upgrade", has(ModItems.FILTER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.MAGNET_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern("EIE")
				.pattern("IPI")
				.pattern("R L")
				.define('E', Tags.Items.ENDER_PEARLS)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('L', Tags.Items.GEMS_LAPIS)
				.define('P', ModItems.PICKUP_UPGRADE.get())
				.unlockedBy("has_pickup_upgrade", has(ModItems.PICKUP_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_MAGNET_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern("EIE")
				.pattern("IPI")
				.pattern("R L")
				.define('E', Tags.Items.ENDER_PEARLS)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('L', Tags.Items.GEMS_LAPIS)
				.define('P', ModItems.ADVANCED_PICKUP_UPGRADE.get())
				.unlockedBy("has_advanced_pickup_upgrade", has(ModItems.ADVANCED_PICKUP_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_MAGNET_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern(" D ")
				.pattern("GMG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('M', ModItems.MAGNET_UPGRADE.get())
				.unlockedBy("has_magnet_upgrade", has(ModItems.MAGNET_UPGRADE.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("advanced_magnet_upgrade_from_basic"));

		ShapeBasedRecipeBuilder.shaped(ModItems.FEEDING_UPGRADE.get())
				.pattern(" C ")
				.pattern("ABM")
				.pattern(" E ")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('C', Items.GOLDEN_CARROT)
				.define('A', Items.GOLDEN_APPLE)
				.define('M', Items.GLISTERING_MELON_SLICE)
				.define('E', Tags.Items.ENDER_PEARLS)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.COMPACTING_UPGRADE.get())
				.pattern("IPI")
				.pattern("PBP")
				.pattern("RPR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('P', Items.PISTON)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_COMPACTING_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern(" D ")
				.pattern("GCG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('C', ModItems.COMPACTING_UPGRADE.get())
				.unlockedBy("has_compacting_upgrade", has(ModItems.COMPACTING_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.VOID_UPGRADE.get())
				.pattern(" E ")
				.pattern("OBO")
				.pattern("ROR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('E', Tags.Items.ENDER_PEARLS)
				.define('O', Tags.Items.OBSIDIAN)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_VOID_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern(" D ")
				.pattern("GVG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('V', ModItems.VOID_UPGRADE.get())
				.unlockedBy("has_void_upgrade", has(ModItems.VOID_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.SMELTING_UPGRADE.get())
				.pattern("RIR")
				.pattern("IBI")
				.pattern("RFR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('F', Items.FURNACE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_SMELTING_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern("DHD")
				.pattern("RSH")
				.pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('H', Items.HOPPER)
				.define('S', ModItems.SMELTING_UPGRADE.get())
				.unlockedBy(HAS_SMELTING_UPGRADE_CRITERION_NAME, has(ModItems.SMELTING_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.CRAFTING_UPGRADE.get())
				.pattern(" T ")
				.pattern("IBI")
				.pattern(" C ")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('C', Tags.Items.CHESTS)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('T', Items.CRAFTING_TABLE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.STONECUTTER_UPGRADE.get())
				.pattern(" S ")
				.pattern("IBI")
				.pattern(" R ")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('S', Items.STONECUTTER)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_1.get())
				.pattern("LLL")
				.pattern("LBL")
				.pattern("LLL")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('L', ItemTags.LOGS)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_1_PLUS.get())
				.pattern("CCC")
				.pattern("CSC")
				.pattern("BCB")
				.define('S', ModItems.STACK_UPGRADE_TIER_1.get())
				.define('C', Tags.Items.INGOTS_COPPER)
				.define('B', Tags.Items.STORAGE_BLOCKS_COPPER)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_1.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_2.get())
				.pattern(" I ")
				.pattern("ISI")
				.pattern(" B ")
				.define('S', ModItems.STACK_UPGRADE_TIER_1_PLUS.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('B', Tags.Items.STORAGE_BLOCKS_IRON)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_1_PLUS.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("stack_upgrade_tier_2_from_tier_1_plus"));

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_2.get())
				.pattern("III")
				.pattern("ISI")
				.pattern("BIB")
				.define('S', ModItems.STACK_UPGRADE_TIER_1.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('B', Tags.Items.STORAGE_BLOCKS_IRON)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_1.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_3.get())
				.pattern("GGG")
				.pattern("GSG")
				.pattern("BGB")
				.define('S', ModItems.STACK_UPGRADE_TIER_2.get())
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('B', Tags.Items.STORAGE_BLOCKS_GOLD)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_2.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_4.get())
				.pattern("DDD")
				.pattern("DSD")
				.pattern("BDB")
				.define('S', ModItems.STACK_UPGRADE_TIER_3.get())
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('B', Tags.Items.STORAGE_BLOCKS_DIAMOND)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_3.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_5.get())
				.pattern("NNN")
				.pattern("NSN")
				.pattern("BNB")
				.define('S', ModItems.STACK_UPGRADE_TIER_4.get())
				.define('N', Tags.Items.INGOTS_NETHERITE)
				.define('B', Tags.Items.STORAGE_BLOCKS_NETHERITE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_4.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.JUKEBOX_UPGRADE.get())
				.pattern(" J ")
				.pattern("IBI")
				.pattern(" R ")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('J', Items.JUKEBOX)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_FEEDING_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern(" D ")
				.pattern("GVG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('V', ModItems.FEEDING_UPGRADE.get())
				.unlockedBy("has_feeding_upgrade", has(ModItems.FEEDING_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.SMOKING_UPGRADE.get())
				.pattern("RIR")
				.pattern("IBI")
				.pattern("RSR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('S', Items.SMOKER)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.SMOKING_UPGRADE.get())
				.pattern(" L ")
				.pattern("LSL")
				.pattern(" L ")
				.define('S', ModItems.SMELTING_UPGRADE.get())
				.define('L', ItemTags.LOGS)
				.unlockedBy(HAS_SMELTING_UPGRADE_CRITERION_NAME, has(ModItems.SMELTING_UPGRADE.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("smoking_upgrade_from_smelting_upgrade"));

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_SMOKING_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern("DHD")
				.pattern("RSH")
				.pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('H', Items.HOPPER)
				.define('S', ModItems.SMOKING_UPGRADE.get())
				.unlockedBy("has_smoking_upgrade", has(ModItems.SMOKING_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_SMOKING_UPGRADE.get())
				.pattern(" L ")
				.pattern("LSL")
				.pattern(" L ")
				.define('S', ModItems.AUTO_SMELTING_UPGRADE.get())
				.define('L', ItemTags.LOGS)
				.unlockedBy("has_auto_smelting_upgrade", has(ModItems.AUTO_SMELTING_UPGRADE.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("auto_smoking_upgrade_from_auto_smelting_upgrade"));

		ShapeBasedRecipeBuilder.shaped(ModItems.BLASTING_UPGRADE.get())
				.pattern("RIR")
				.pattern("IBI")
				.pattern("RFR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('F', Items.BLAST_FURNACE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.BLASTING_UPGRADE.get())
				.pattern("III")
				.pattern("ISI")
				.pattern("TTT")
				.define('S', ModItems.SMELTING_UPGRADE.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('T', Items.SMOOTH_STONE)
				.unlockedBy(HAS_SMELTING_UPGRADE_CRITERION_NAME, has(ModItems.SMELTING_UPGRADE.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("blasting_upgrade_from_smelting_upgrade"));

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_BLASTING_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern("DHD")
				.pattern("RSH")
				.pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('H', Items.HOPPER)
				.define('S', ModItems.BLASTING_UPGRADE.get())
				.unlockedBy("has_blasting_upgrade", has(ModItems.BLASTING_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_BLASTING_UPGRADE.get())
				.pattern("III")
				.pattern("ISI")
				.pattern("TTT")
				.define('S', ModItems.AUTO_SMELTING_UPGRADE.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('T', Items.SMOOTH_STONE)
				.unlockedBy("has_auto_smelting_upgrade", has(ModItems.AUTO_SMELTING_UPGRADE.get()))
				.save(recipeOutput, SophisticatedStorage.getRL("auto_blasting_upgrade_from_auto_smelting_upgrade"));

		ShapeBasedRecipeBuilder.shaped(ModItems.COMPRESSION_UPGRADE.get())
				.pattern(" I ")
				.pattern("PBP")
				.pattern("RIR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('P', Items.PISTON)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.HOPPER_UPGRADE.get())
				.pattern(" H ")
				.pattern("IBI")
				.pattern("RRR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('H', Items.HOPPER)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_HOPPER_UPGRADE.get(), UpgradeNextTierRecipe::new)
				.pattern(" D ")
				.pattern("GHG")
				.pattern("ROR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('O', Items.DROPPER)
				.define('H', ModItems.HOPPER_UPGRADE.get())
				.unlockedBy("has_feeding_upgrade", has(ModItems.HOPPER_UPGRADE.get()))
				.save(recipeOutput);

		addChippedUpgradeRecipes(recipeOutput);
	}

	private static void addChippedUpgradeRecipes(RecipeOutput recipeOutput) {
		addChippedUpgradeRecipe(recipeOutput, ChippedCompat.BOTANIST_WORKBENCH_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.BOTANIST_WORKBENCH.get());
		addChippedUpgradeRecipe(recipeOutput, ChippedCompat.GLASSBLOWER_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.GLASSBLOWER.get());
		addChippedUpgradeRecipe(recipeOutput, ChippedCompat.CARPENTERS_TABLE_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.CARPENTERS_TABLE.get());
		addChippedUpgradeRecipe(recipeOutput, ChippedCompat.LOOM_TABLE_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.LOOM_TABLE.get());
		addChippedUpgradeRecipe(recipeOutput, ChippedCompat.MASON_TABLE_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.MASON_TABLE.get());
		addChippedUpgradeRecipe(recipeOutput, ChippedCompat.ALCHEMY_BENCH_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.ALCHEMY_BENCH.get());
		addChippedUpgradeRecipe(recipeOutput, ChippedCompat.TINKERING_TABLE_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.TINKERING_TABLE.get());
	}

	private static void addChippedUpgradeRecipe(RecipeOutput recipeOutput, BlockTransformationUpgradeItem upgrade, Block workbench) {
		ShapeBasedRecipeBuilder.shaped(upgrade)
				.pattern(" W ")
				.pattern("IBI")
				.pattern(" R ")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('W', workbench)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput.withConditions(new ModLoadedCondition(CompatModIds.CHIPPED)));
	}

	private void addChestRecipes(RecipeOutput recipeOutput) {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach((woodType, blockFamily) -> woodChestRecipe(recipeOutput, woodType, blockFamily.getBaseBlock()));

		ShapelessBasedRecipeBuilder.shapeless(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.CHEST_ITEM.get()), WoodType.OAK))
				.requires(Blocks.CHEST)
				.requires(Blocks.REDSTONE_TORCH)
				.unlockedBy("has_vanilla_chest", has(Blocks.CHEST))
				.save(recipeOutput, SophisticatedStorage.getRL("oak_chest_from_vanilla_chest"));

		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.CHEST_ITEM.get(), ModBlocks.COPPER_CHEST_ITEM.get(), ModBlocks.IRON_CHEST_ITEM.get(), ModBlocks.GOLD_CHEST_ITEM.get(), ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.NETHERITE_CHEST_ITEM.get());

		//addQuarkChestRecipes(recipeOutput); // TODO readd with quark compat
	}

	private void addQuarkChestRecipes(RecipeOutput recipeOutput) {
		addQuarkChestRecipe(recipeOutput, "oak_chest", WoodType.OAK);
		addQuarkChestRecipe(recipeOutput, "acacia_chest", WoodType.ACACIA);
		addQuarkChestRecipe(recipeOutput, "birch_chest", WoodType.BIRCH);
		addQuarkChestRecipe(recipeOutput, "crimson_chest", WoodType.CRIMSON);
		addQuarkChestRecipe(recipeOutput, "dark_oak_chest", WoodType.DARK_OAK);
		addQuarkChestRecipe(recipeOutput, "jungle_chest", WoodType.JUNGLE);
		addQuarkChestRecipe(recipeOutput, "mangrove_chest", WoodType.MANGROVE);
		addQuarkChestRecipe(recipeOutput, "spruce_chest", WoodType.SPRUCE);
		addQuarkChestRecipe(recipeOutput, "warped_chest", WoodType.WARPED);
		addQuarkChestRecipe(recipeOutput, "bamboo_chest", WoodType.BAMBOO);
		addQuarkChestRecipe(recipeOutput, "cherry_chest", WoodType.CHERRY);
	}

	private void addQuarkChestRecipe(RecipeOutput recipeOutput, String name, WoodType woodType) {
		String chestRegistryName = "quark:" + name;
		Block chestBlock = getBlock(chestRegistryName);
		ShapelessBasedRecipeBuilder.shapeless(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.CHEST_ITEM.get()), woodType))
				.requires(chestBlock)
				.requires(Blocks.REDSTONE_TORCH)
				.save(recipeOutput.withConditions(new ItemExistsCondition(chestRegistryName)), SophisticatedStorage.getRL(woodType.name() + "_chest_from_quark_" + name));
	}

	private Block getBlock(String registryName) {
		return BuiltInRegistries.BLOCK.get(new ResourceLocation(registryName));
	}

	private void addBarrelRecipes(RecipeOutput recipeOutput) {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach((woodType, blockFamily) -> woodBarrelRecipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB)));

		ShapelessBasedRecipeBuilder.shapeless(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL_ITEM.get()), WoodType.SPRUCE))
				.requires(Blocks.BARREL)
				.requires(Blocks.REDSTONE_TORCH)
				.unlockedBy("has_vanilla_barrel", has(Blocks.BARREL))
				.save(recipeOutput, SophisticatedStorage.getRL("spruce_barrel_from_vanilla_barrel"));

		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.BARREL_ITEM.get(), ModBlocks.COPPER_BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get());
	}

	private void woodBarrelRecipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		ShapeBasedRecipeBuilder.shaped(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL_ITEM.get()), woodType))
				.pattern("PSP")
				.pattern("PRP")
				.pattern("PSP")
				.define('P', planks)
				.define('S', slab)
				.define('R', Blocks.REDSTONE_TORCH)
				.unlockedBy("has_" + woodType.name() + PLANK_SUFFIX, has(planks))
				.save(recipeOutput, SophisticatedStorage.getRL(woodType.name() + "_barrel"));
	}

	private void limitedWoodBarrelRecipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab, Consumer<ShapedRecipeBuilder> addPattern, BlockItem item) {
		ShapedRecipeBuilder builder = ShapeBasedRecipeBuilder.shaped(WoodStorageBlockItem.setWoodType(new ItemStack(item), woodType))
				.define('P', planks)
				.define('S', slab)
				.define('R', Blocks.REDSTONE_TORCH)
				.unlockedBy("has_" + woodType.name() + PLANK_SUFFIX, has(planks));
		addPattern.accept(builder);
		builder.save(recipeOutput, SophisticatedStorage.getRL(woodType.name() + "_" + RegistryHelper.getItemKey(item).getPath()));
	}

	private void limitedWoodBarrel1Recipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		limitedWoodBarrelRecipe(recipeOutput, woodType, planks, slab, builder ->
						builder.pattern("PSP")
								.pattern("PRP")
								.pattern("PPP")
				, ModBlocks.LIMITED_BARREL_1_ITEM.get());
	}

	private void limitedWoodBarrel2Recipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		limitedWoodBarrelRecipe(recipeOutput, woodType, planks, slab, builder ->
						builder.pattern("PPP")
								.pattern("SRS")
								.pattern("PPP")
				, ModBlocks.LIMITED_BARREL_2_ITEM.get());
	}

	private void limitedWoodBarrel3Recipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		limitedWoodBarrelRecipe(recipeOutput, woodType, planks, slab, builder ->
						builder.pattern("PSP")
								.pattern("PRP")
								.pattern("SPS")
				, ModBlocks.LIMITED_BARREL_3_ITEM.get());
	}

	private void limitedWoodBarrel4Recipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		limitedWoodBarrelRecipe(recipeOutput, woodType, planks, slab, builder ->
						builder.pattern("SPS")
								.pattern("PRP")
								.pattern("SPS")
				, ModBlocks.LIMITED_BARREL_4_ITEM.get());
	}

	private void woodChestRecipe(RecipeOutput recipeOutput, WoodType woodType, Block planks) {
		ShapeBasedRecipeBuilder.shaped(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.CHEST_ITEM.get()), woodType))
				.pattern("PPP")
				.pattern("PRP")
				.pattern("PPP")
				.define('P', planks)
				.define('R', Blocks.REDSTONE_TORCH)
				.unlockedBy("has_" + woodType.name() + PLANK_SUFFIX, has(planks))
				.save(recipeOutput, SophisticatedStorage.getRL(woodType.name() + "_chest"));
	}

	private void tintedShulkerBoxRecipe(RecipeOutput recipeOutput, Block vanillaShulkerBox, DyeColor dyeColor) {
		String vanillaShulkerBoxName = BuiltInRegistries.BLOCK.getKey(vanillaShulkerBox).getPath();
		ShapelessBasedRecipeBuilder.shapeless(ModBlocks.SHULKER_BOX.get().getTintedStack(dyeColor)).requires(vanillaShulkerBox).requires(Items.REDSTONE_TORCH)
				.unlockedBy("has_" + vanillaShulkerBoxName, has(vanillaShulkerBox))
				.save(recipeOutput, SophisticatedStorage.getRL(vanillaShulkerBoxName + "_to_sophisticated"));
	}
}
