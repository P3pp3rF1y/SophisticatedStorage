package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.data.recipes.UpgradeRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.crafting.ShapeBasedRecipeBuilder;
import net.p3pp3rf1y.sophisticatedcore.crafting.ShapelessBasedRecipeBuilder;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import java.util.function.Consumer;

public class StorageRecipeProvider extends RecipeProvider {
	private static final String HAS_UPGRADE_BASE_CRITERION_NAME = "has_upgrade_base";
	private static final String HAS_REDSTONE_TORCH_CRITERION_NAME = "has_redstone_torch";
	private static final String HAS_SMELTING_UPGRADE = "has_smelting_upgrade";

	public StorageRecipeProvider(DataGenerator generatorIn) {
		super(generatorIn);
	}

	@Override
	protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
		SpecialRecipeBuilder.special(ModBlocks.STORAGE_DYE_RECIPE_SERIALIZER.get()).save(consumer, SophisticatedStorage.getRegistryName("storage_dye"));

		addBarrelRecipes(consumer);
		addChestRecipes(consumer);
		addShulkerBoxRecipes(consumer);
		addUpgradeRecipes(consumer);
		addTierUpgradeRecipes(consumer);

		ShapelessBasedRecipeBuilder.shapeless(ModItems.PACKING_TAPE.get())
				.requires(Tags.Items.SLIMEBALLS)
				.requires(Items.PAPER)
				.unlockedBy("has_slime", has(Tags.Items.SLIMEBALLS))
				.save(consumer);
	}

	private void addShulkerBoxRecipes(Consumer<FinishedRecipe> consumer) {
		ShapeBasedRecipeBuilder.shaped(ModBlocks.SHULKER_BOX_ITEM.get())
				.pattern(" S")
				.pattern("RC")
				.pattern(" S")
				.define('R', Items.REDSTONE_TORCH)
				.define('S', Items.SHULKER_SHELL)
				.define('C', Tags.Items.CHESTS)
				.unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.CONTROLLER_ITEM.get())
				.pattern("SCS")
				.pattern("PBP")
				.pattern("SCS")
				.define('S', Tags.Items.STONE)
				.define('C', Items.COMPARATOR)
				.define('P', ItemTags.PLANKS)
				.define('B', ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG)
				.unlockedBy("has_base_tier_wooden_storage", has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG))
				.save(consumer);

		ShapelessBasedRecipeBuilder.shapeless(ModBlocks.SHULKER_BOX_ITEM.get())
				.requires(Items.SHULKER_BOX).requires(Items.REDSTONE_TORCH)
				.save(consumer, "shulker_box_from_vanilla_shulker_box");

		tintedShulkerBoxRecipe(consumer, Blocks.BLACK_SHULKER_BOX, DyeColor.BLACK);
		tintedShulkerBoxRecipe(consumer, Blocks.BLUE_SHULKER_BOX, DyeColor.BLUE);
		tintedShulkerBoxRecipe(consumer, Blocks.BROWN_SHULKER_BOX, DyeColor.BROWN);
		tintedShulkerBoxRecipe(consumer, Blocks.CYAN_SHULKER_BOX, DyeColor.CYAN);
		tintedShulkerBoxRecipe(consumer, Blocks.GRAY_SHULKER_BOX, DyeColor.GRAY);
		tintedShulkerBoxRecipe(consumer, Blocks.GREEN_SHULKER_BOX, DyeColor.GREEN);
		tintedShulkerBoxRecipe(consumer, Blocks.LIGHT_BLUE_SHULKER_BOX, DyeColor.LIGHT_BLUE);
		tintedShulkerBoxRecipe(consumer, Blocks.LIGHT_GRAY_SHULKER_BOX, DyeColor.LIGHT_GRAY);
		tintedShulkerBoxRecipe(consumer, Blocks.LIME_SHULKER_BOX, DyeColor.LIME);
		tintedShulkerBoxRecipe(consumer, Blocks.MAGENTA_SHULKER_BOX, DyeColor.MAGENTA);
		tintedShulkerBoxRecipe(consumer, Blocks.ORANGE_SHULKER_BOX, DyeColor.ORANGE);
		tintedShulkerBoxRecipe(consumer, Blocks.PINK_SHULKER_BOX, DyeColor.PINK);
		tintedShulkerBoxRecipe(consumer, Blocks.PURPLE_SHULKER_BOX, DyeColor.PURPLE);
		tintedShulkerBoxRecipe(consumer, Blocks.RED_SHULKER_BOX, DyeColor.RED);
		tintedShulkerBoxRecipe(consumer, Blocks.WHITE_SHULKER_BOX, DyeColor.WHITE);
		tintedShulkerBoxRecipe(consumer, Blocks.YELLOW_SHULKER_BOX, DyeColor.YELLOW);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.IRON_SHULKER_BOX_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("III")
				.pattern("ISI")
				.pattern("III")
				.define('S', ModBlocks.SHULKER_BOX_ITEM.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.unlockedBy("has_shulker_box", has(ModBlocks.SHULKER_BOX_ITEM.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("GGG")
				.pattern("GSG")
				.pattern("GGG")
				.define('S', ModBlocks.IRON_SHULKER_BOX_ITEM.get())
				.define('G', Tags.Items.INGOTS_GOLD)
				.unlockedBy("has_iron_shulker_box", has(ModBlocks.IRON_SHULKER_BOX_ITEM.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("DDD")
				.pattern("DSD")
				.pattern("DDD")
				.define('S', ModBlocks.GOLD_SHULKER_BOX_ITEM.get())
				.define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy("has_gold_shulker_box", has(ModBlocks.GOLD_SHULKER_BOX_ITEM.get()))
				.save(consumer);

		new UpgradeRecipeBuilder(ModBlocks.SMITHING_STORAGE_UPGRADE_RECIPE_SERIALIZER.get(), Ingredient.of(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()),
				Ingredient.of(Items.NETHERITE_INGOT), ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get())
				.unlocks("has_diamond_shulker_box", has(ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get()))
				.save(consumer, RegistryHelper.getItemKey(ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get()));
	}

	private void addTierUpgradeRecipes(Consumer<FinishedRecipe> consumer) {
		ShapeBasedRecipeBuilder.shaped(ModItems.BASIC_TIER_UPGRADE.get())
				.pattern(" S ")
				.pattern("SRS")
				.pattern(" S ")
				.define('R', Items.REDSTONE_TORCH)
				.define('S', Items.STICK)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.BASIC_TO_IRON_TIER_UPGRADE.get())
				.pattern("III")
				.pattern("IRI")
				.pattern("III")
				.define('R', Items.REDSTONE_TORCH)
				.define('I', Tags.Items.INGOTS_IRON)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.IRON_TO_GOLD_TIER_UPGRADE.get())
				.pattern("GGG")
				.pattern("GRG")
				.pattern("GGG")
				.define('R', Items.REDSTONE_TORCH)
				.define('G', Tags.Items.INGOTS_GOLD)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get())
				.pattern("DDD")
				.pattern("DRD")
				.pattern("DDD")
				.define('R', Items.REDSTONE_TORCH)
				.define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(consumer);

		ShapelessBasedRecipeBuilder.shapeless(ModItems.DIAMOND_TO_NETHERITE_TIER_UPGRADE.get())
				.requires(Items.REDSTONE_TORCH)
				.requires(Tags.Items.INGOTS_NETHERITE)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH))
				.save(consumer);
	}

	private void addUpgradeRecipes(Consumer<FinishedRecipe> consumer) {
		ShapeBasedRecipeBuilder.shaped(ModItems.UPGRADE_BASE.get())
				.pattern("PIP")
				.pattern("IPI")
				.pattern("PIP")
				.define('P', ItemTags.PLANKS)
				.define('I', Tags.Items.INGOTS_IRON)
				.unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.PICKUP_UPGRADE.get())
				.pattern(" P ")
				.pattern("LBL")
				.pattern("RRR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('L', ItemTags.PLANKS)
				.define('P', Blocks.STICKY_PISTON)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_PICKUP_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern(" D ")
				.pattern("GPG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('P', ModItems.PICKUP_UPGRADE.get())
				.unlockedBy("has_pickup_upgrade", has(ModItems.PICKUP_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.FILTER_UPGRADE.get())
				.pattern("RSR")
				.pattern("SBS")
				.pattern("RSR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('S', Tags.Items.STRING)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_FILTER_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern("GPG")
				.pattern("RRR")
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('P', ModItems.FILTER_UPGRADE.get())
				.unlockedBy("has_filter_upgrade", has(ModItems.FILTER_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.MAGNET_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern("EIE")
				.pattern("IPI")
				.pattern("R L")
				.define('E', Tags.Items.ENDER_PEARLS)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('L', Tags.Items.GEMS_LAPIS)
				.define('P', ModItems.PICKUP_UPGRADE.get())
				.unlockedBy("has_pickup_upgrade", has(ModItems.PICKUP_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_MAGNET_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern("EIE")
				.pattern("IPI")
				.pattern("R L")
				.define('E', Tags.Items.ENDER_PEARLS)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('L', Tags.Items.GEMS_LAPIS)
				.define('P', ModItems.ADVANCED_PICKUP_UPGRADE.get())
				.unlockedBy("has_advanced_pickup_upgrade", has(ModItems.ADVANCED_PICKUP_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_MAGNET_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern(" D ")
				.pattern("GMG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('M', ModItems.MAGNET_UPGRADE.get())
				.unlockedBy("has_magnet_upgrade", has(ModItems.MAGNET_UPGRADE.get()))
				.save(consumer, SophisticatedStorage.getRL("advanced_magnet_upgrade_from_basic"));

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
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.COMPACTING_UPGRADE.get())
				.pattern("IPI")
				.pattern("PBP")
				.pattern("RPR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('P', Items.PISTON)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_COMPACTING_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern(" D ")
				.pattern("GCG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('C', ModItems.COMPACTING_UPGRADE.get())
				.unlockedBy("has_compacting_upgrade", has(ModItems.COMPACTING_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.VOID_UPGRADE.get())
				.pattern(" E ")
				.pattern("OBO")
				.pattern("ROR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('E', Tags.Items.ENDER_PEARLS)
				.define('O', Tags.Items.OBSIDIAN)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_VOID_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern(" D ")
				.pattern("GVG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('V', ModItems.VOID_UPGRADE.get())
				.unlockedBy("has_void_upgrade", has(ModItems.VOID_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.SMELTING_UPGRADE.get())
				.pattern("RIR")
				.pattern("IBI")
				.pattern("RFR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('F', Items.FURNACE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_SMELTING_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern("DHD")
				.pattern("RSH")
				.pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('H', Items.HOPPER)
				.define('S', ModItems.SMELTING_UPGRADE.get())
				.unlockedBy(HAS_SMELTING_UPGRADE, has(ModItems.SMELTING_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.CRAFTING_UPGRADE.get())
				.pattern(" T ")
				.pattern("IBI")
				.pattern(" C ")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('C', Tags.Items.CHESTS)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('T', Items.CRAFTING_TABLE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.STONECUTTER_UPGRADE.get())
				.pattern(" S ")
				.pattern("IBI")
				.pattern(" R ")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('S', Items.STONECUTTER)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_1.get())
				.pattern("LLL")
				.pattern("LBL")
				.pattern("LLL")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('L', ItemTags.LOGS)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_2.get())
				.pattern("III")
				.pattern("ISI")
				.pattern("BIB")
				.define('S', ModItems.STACK_UPGRADE_TIER_1.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('B', Tags.Items.STORAGE_BLOCKS_IRON)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_1.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_3.get())
				.pattern("GGG")
				.pattern("GSG")
				.pattern("BGB")
				.define('S', ModItems.STACK_UPGRADE_TIER_2.get())
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('B', Tags.Items.STORAGE_BLOCKS_GOLD)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_2.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.STACK_UPGRADE_TIER_4.get())
				.pattern("DDD")
				.pattern("DSD")
				.pattern("BDB")
				.define('S', ModItems.STACK_UPGRADE_TIER_3.get())
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('B', Tags.Items.STORAGE_BLOCKS_DIAMOND)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.STACK_UPGRADE_TIER_3.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.JUKEBOX_UPGRADE.get())
				.pattern(" J ")
				.pattern("IBI")
				.pattern(" R ")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('J', Items.JUKEBOX)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.ADVANCED_FEEDING_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern(" D ")
				.pattern("GVG")
				.pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('V', ModItems.FEEDING_UPGRADE.get())
				.unlockedBy("has_feeding_upgrade", has(ModItems.FEEDING_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.SMOKING_UPGRADE.get())
				.pattern("RIR")
				.pattern("IBI")
				.pattern("RSR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('S', Items.SMOKER)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.SMOKING_UPGRADE.get())
				.pattern(" L ")
				.pattern("LSL")
				.pattern(" L ")
				.define('S', ModItems.SMELTING_UPGRADE.get())
				.define('L', ItemTags.LOGS)
				.unlockedBy(HAS_SMELTING_UPGRADE, has(ModItems.SMELTING_UPGRADE.get()))
				.save(consumer, SophisticatedStorage.getRL("smoking_upgrade_from_smelting_upgrade"));

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_SMOKING_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern("DHD")
				.pattern("RSH")
				.pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('H', Items.HOPPER)
				.define('S', ModItems.SMOKING_UPGRADE.get())
				.unlockedBy("has_smoking_upgrade", has(ModItems.SMOKING_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_SMOKING_UPGRADE.get())
				.pattern(" L ")
				.pattern("LSL")
				.pattern(" L ")
				.define('S', ModItems.AUTO_SMELTING_UPGRADE.get())
				.define('L', ItemTags.LOGS)
				.unlockedBy("has_auto_smelting_upgrade", has(ModItems.AUTO_SMELTING_UPGRADE.get()))
				.save(consumer, SophisticatedStorage.getRL("auto_smoking_upgrade_from_auto_smelting_upgrade"));

		ShapeBasedRecipeBuilder.shaped(ModItems.BLASTING_UPGRADE.get())
				.pattern("RIR")
				.pattern("IBI")
				.pattern("RFR")
				.define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('I', Tags.Items.INGOTS_IRON)
				.define('F', Items.BLAST_FURNACE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.BLASTING_UPGRADE.get())
				.pattern("III")
				.pattern("ISI")
				.pattern("TTT")
				.define('S', ModItems.SMELTING_UPGRADE.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('T', Items.SMOOTH_STONE)
				.unlockedBy(HAS_SMELTING_UPGRADE, has(ModItems.SMELTING_UPGRADE.get()))
				.save(consumer, SophisticatedStorage.getRL("blasting_upgrade_from_smelting_upgrade"));

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_BLASTING_UPGRADE.get(), ModRecipes.UPGRADE_NEXT_TIER_SERIALIZER.get())
				.pattern("DHD")
				.pattern("RSH")
				.pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('R', Tags.Items.DUSTS_REDSTONE)
				.define('H', Items.HOPPER)
				.define('S', ModItems.BLASTING_UPGRADE.get())
				.unlockedBy("has_blasting_upgrade", has(ModItems.BLASTING_UPGRADE.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModItems.AUTO_BLASTING_UPGRADE.get())
				.pattern("III")
				.pattern("ISI")
				.pattern("TTT")
				.define('S', ModItems.AUTO_SMELTING_UPGRADE.get())
				.define('I', Tags.Items.INGOTS_IRON)
				.define('T', Items.SMOOTH_STONE)
				.unlockedBy("has_auto_smelting_upgrade", has(ModItems.AUTO_SMELTING_UPGRADE.get()))
				.save(consumer, SophisticatedStorage.getRL("auto_blasting_upgrade_from_auto_smelting_upgrade"));
	}

	private void addChestRecipes(Consumer<FinishedRecipe> consumer) {
		woodChestRecipe(consumer, WoodType.ACACIA, Blocks.ACACIA_PLANKS);
		woodChestRecipe(consumer, WoodType.BIRCH, Blocks.BIRCH_PLANKS);
		woodChestRecipe(consumer, WoodType.CRIMSON, Blocks.CRIMSON_PLANKS);
		woodChestRecipe(consumer, WoodType.DARK_OAK, Blocks.DARK_OAK_PLANKS);
		woodChestRecipe(consumer, WoodType.JUNGLE, Blocks.JUNGLE_PLANKS);
		woodChestRecipe(consumer, WoodType.OAK, Blocks.OAK_PLANKS);
		woodChestRecipe(consumer, WoodType.SPRUCE, Blocks.SPRUCE_PLANKS);
		woodChestRecipe(consumer, WoodType.WARPED, Blocks.WARPED_PLANKS);

		ShapelessBasedRecipeBuilder.shapeless(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.CHEST_ITEM.get()), WoodType.OAK))
				.requires(Blocks.CHEST)
				.requires(Blocks.REDSTONE_TORCH)
				.unlockedBy("has_vanilla_chest", has(Blocks.CHEST))
				.save(consumer, SophisticatedStorage.getRL("oak_chest_from_vanilla_chest"));

		ShapeBasedRecipeBuilder.shaped(ModBlocks.IRON_CHEST_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("III")
				.pattern("ICI")
				.pattern("III")
				.define('I', Tags.Items.INGOTS_IRON)
				.define('C', ModBlocks.CHEST_ITEM.get())
				.unlockedBy("has_chest", has(ModBlocks.CHEST_ITEM.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.GOLD_CHEST_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("GGG")
				.pattern("GCG")
				.pattern("GGG")
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('C', ModBlocks.IRON_CHEST_ITEM.get())
				.unlockedBy("has_iron_chest", has(ModBlocks.IRON_CHEST_ITEM.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("DDD")
				.pattern("DCD")
				.pattern("DDD")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('C', ModBlocks.GOLD_CHEST_ITEM.get())
				.unlockedBy("has_gold_chest", has(ModBlocks.GOLD_CHEST_ITEM.get()))
				.save(consumer);

		new UpgradeRecipeBuilder(ModBlocks.SMITHING_STORAGE_UPGRADE_RECIPE_SERIALIZER.get(), Ingredient.of(ModBlocks.DIAMOND_CHEST_ITEM.get()),
				Ingredient.of(Items.NETHERITE_INGOT), ModBlocks.NETHERITE_CHEST_ITEM.get())
				.unlocks("has_diamond_chest", has(ModBlocks.DIAMOND_CHEST_ITEM.get()))
				.save(consumer, RegistryHelper.getItemKey(ModBlocks.NETHERITE_CHEST_ITEM.get()));

		addQuarkChestRecipes(consumer);
	}

	private void addQuarkChestRecipes(Consumer<FinishedRecipe> consumer) {
		addQuarkChestRecipe(consumer, "oak_chest", WoodType.OAK);
		addQuarkChestRecipe(consumer, "acacia_chest", WoodType.ACACIA);
		addQuarkChestRecipe(consumer, "birch_chest", WoodType.BIRCH);
		addQuarkChestRecipe(consumer, "crimson_chest", WoodType.CRIMSON);
		addQuarkChestRecipe(consumer, "dark_oak_chest", WoodType.DARK_OAK);
		addQuarkChestRecipe(consumer, "jungle_chest", WoodType.JUNGLE);
		addQuarkChestRecipe(consumer, "spruce_chest", WoodType.SPRUCE);
		addQuarkChestRecipe(consumer, "warped_chest", WoodType.WARPED);
	}

	private void addQuarkChestRecipe(Consumer<FinishedRecipe> consumer, String name, WoodType woodType) {
		Block chestBlock = getBlock("quark:" + name);
		ShapelessBasedRecipeBuilder.shapeless(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.CHEST_ITEM.get()), woodType))
				.requires(chestBlock)
				.requires(Blocks.REDSTONE_TORCH)
				.condition(new ModLoadedCondition("quark"))
				.save(consumer, SophisticatedStorage.getRL(woodType.name() + "_chest_from_quark_" + name));
	}

	private Block getBlock(String registryName) {
		//noinspection ConstantConditions - could only fail in dev environment and crashing is preferred here to fix issues early
		return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registryName));
	}

	private void addBarrelRecipes(Consumer<FinishedRecipe> consumer) {
		woodBarrelRecipe(consumer, WoodType.ACACIA, Blocks.ACACIA_PLANKS, Blocks.ACACIA_SLAB);
		woodBarrelRecipe(consumer, WoodType.BIRCH, Blocks.BIRCH_PLANKS, Blocks.BIRCH_SLAB);
		woodBarrelRecipe(consumer, WoodType.CRIMSON, Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_SLAB);
		woodBarrelRecipe(consumer, WoodType.DARK_OAK, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_SLAB);
		woodBarrelRecipe(consumer, WoodType.JUNGLE, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_SLAB);
		woodBarrelRecipe(consumer, WoodType.OAK, Blocks.OAK_PLANKS, Blocks.OAK_SLAB);
		woodBarrelRecipe(consumer, WoodType.SPRUCE, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB);
		woodBarrelRecipe(consumer, WoodType.WARPED, Blocks.WARPED_PLANKS, Blocks.WARPED_SLAB);

		ShapelessBasedRecipeBuilder.shapeless(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL_ITEM.get()), WoodType.SPRUCE))
				.requires(Blocks.BARREL)
				.requires(Blocks.REDSTONE_TORCH)
				.unlockedBy("has_vanilla_barrel", has(Blocks.BARREL))
				.save(consumer, SophisticatedStorage.getRL("spruce_barrel_from_vanilla_barrel"));

		ShapeBasedRecipeBuilder.shaped(ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("III")
				.pattern("IBI")
				.pattern("III")
				.define('I', Tags.Items.INGOTS_IRON)
				.define('B', ModBlocks.BARREL_ITEM.get())
				.unlockedBy("has_barrel", has(ModBlocks.BARREL_ITEM.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("GGG")
				.pattern("GBG")
				.pattern("GGG")
				.define('G', Tags.Items.INGOTS_GOLD)
				.define('B', ModBlocks.IRON_BARREL_ITEM.get())
				.unlockedBy("has_iron_barrel", has(ModBlocks.IRON_BARREL_ITEM.get()))
				.save(consumer);

		ShapeBasedRecipeBuilder.shaped(ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.STORAGE_TIER_UPGRADE_RECIPE_SERIALIZER.get())
				.pattern("DDD")
				.pattern("DBD")
				.pattern("DDD")
				.define('D', Tags.Items.GEMS_DIAMOND)
				.define('B', ModBlocks.GOLD_BARREL_ITEM.get())
				.unlockedBy("has_gold_barrel", has(ModBlocks.GOLD_BARREL_ITEM.get()))
				.save(consumer);

		new UpgradeRecipeBuilder(ModBlocks.SMITHING_STORAGE_UPGRADE_RECIPE_SERIALIZER.get(), Ingredient.of(ModBlocks.DIAMOND_BARREL_ITEM.get()),
				Ingredient.of(Items.NETHERITE_INGOT), ModBlocks.NETHERITE_BARREL_ITEM.get())
				.unlocks("has_diamond_barrel", has(ModBlocks.DIAMOND_BARREL_ITEM.get()))
				.save(consumer, RegistryHelper.getItemKey(ModBlocks.NETHERITE_BARREL_ITEM.get()));
	}

	private void woodBarrelRecipe(Consumer<FinishedRecipe> consumer, WoodType woodType, Block planks, Block slab) {
		ShapeBasedRecipeBuilder.shaped(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL_ITEM.get()), woodType))
				.pattern("PSP")
				.pattern("PRP")
				.pattern("PSP")
				.define('P', planks)
				.define('S', slab)
				.define('R', Blocks.REDSTONE_TORCH)
				.unlockedBy("has_" + woodType.name() + "_plank", has(planks))
				.save(consumer, SophisticatedStorage.getRL(woodType.name() + "_barrel"));
	}

	private void woodChestRecipe(Consumer<FinishedRecipe> consumer, WoodType woodType, Block planks) {
		ShapeBasedRecipeBuilder.shaped(WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.CHEST_ITEM.get()), woodType))
				.pattern("PPP")
				.pattern("PRP")
				.pattern("PPP")
				.define('P', planks)
				.define('R', Blocks.REDSTONE_TORCH)
				.unlockedBy("has_" + woodType.name() + "_plank", has(planks))
				.save(consumer, SophisticatedStorage.getRL(woodType.name() + "_chest"));
	}

	private void tintedShulkerBoxRecipe(Consumer<FinishedRecipe> consumer, Block vanillaShulkerBox, DyeColor dyeColor) {
		//noinspection ConstantConditions
		String vanillaShulkerBoxName = ForgeRegistries.BLOCKS.getKey(vanillaShulkerBox).getPath();
		ShapelessBasedRecipeBuilder.shapeless(ModBlocks.SHULKER_BOX.get().getTintedStack(dyeColor)).requires(vanillaShulkerBox).requires(Items.REDSTONE_TORCH)
				.unlockedBy("has_" + vanillaShulkerBoxName, has(vanillaShulkerBox))
				.save(consumer, SophisticatedStorage.getRL(vanillaShulkerBoxName + "_to_sophisticated"));
	}
}
