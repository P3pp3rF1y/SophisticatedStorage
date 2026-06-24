package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.RegisteredCondition;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedcore.crafting.ShapeBasedRecipeBuilder;
import net.p3pp3rf1y.sophisticatedcore.crafting.ShapelessBasedRecipeBuilder;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.crafting.*;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import javax.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class StorageRecipeProvider extends RecipeProvider {
	private static final String HAS_UPGRADE_BASE_CRITERION_NAME = "has_upgrade_base";
	private static final String HAS_LEVER_CRITERION_NAME = "has_lever";
	private static final String HAS_REDSTONE_TORCH_CRITERION_NAME = "has_redstone_torch";
	private static final String HAS_SMELTING_UPGRADE_CRITERION_NAME = "has_smelting_upgrade";
	public static final String HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME = "has_base_tier_wooden_storage";
	private static final String PLANK_SUFFIX = "_plank";

	private final HolderGetter<Item> items;

	public StorageRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
		super(provider, recipeOutput);
		items = provider.lookupOrThrow(Registries.ITEM);
	}

	@Override
	protected void buildRecipes() {
		SpecialRecipeBuilder.special(() -> StorageDyeRecipe.INSTANCE).save(output, SophisticatedStorage.getRegistryName("storage_dye"));
		SpecialRecipeBuilder.special(() -> FlatTopBarrelToggleRecipe.INSTANCE).save(output, SophisticatedStorage.getRegistryName("flat_top_barrel_toggle"));
		SpecialRecipeBuilder.special(() -> BarrelMaterialRecipe.INSTANCE).save(output, SophisticatedStorage.getRegistryName("barrel_material"));

		addBarrelRecipes(output);
		addLimitedBarrelRecipes(output);
		addChestRecipes(output);
		addShulkerBoxRecipes(output);
		addControllerRelatedRecipes(output);
		addUpgradeRecipes(output);
		addTierUpgradeItemRecipes(output);
		addBackpackUpgradeConversionRecipes(output);

		ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, ModItems.PACKING_TAPE.get()).requires(Tags.Items.SLIME_BALLS).requires(Items.PAPER)
				.unlockedBy("has_slime", has(Tags.Items.SLIME_BALLS)).save(output.withConditions(new DropPackedDisabledCondition()));

		ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, ModItems.SUPER_PACKING_TAPE.get()).requires(ModItems.PACKING_TAPE.get())
				.requires(ModItems.PACKING_TAPE.get()).requires(ModItems.PACKING_TAPE.get()).requires(ModItems.PACKING_TAPE.get())
				.unlockedBy("has_packing_tape", has(ModItems.PACKING_TAPE.get())).save(output.withConditions(new DropPackedDisabledCondition()));

		ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModBlocks.DECORATION_TABLE_ITEM.get()).pattern("LLL").pattern("PBP").pattern("P P")
				.define('L', ItemTags.LOGS).define('P', ItemTags.PLANKS).define('B', ModItems.UPGRADE_BASE.get())
				.unlockedBy("has_upgrade_base", has(ModItems.UPGRADE_BASE.get())).save(output);

		ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.PAINTBRUSH.get()).pattern(" W ").pattern(" SW").pattern("S  ")
				.define('S', Tags.Items.RODS_WOODEN).define('W', ItemTags.WOOL)
				.unlockedBy("has_base_tier_wooden_storage", has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG)).save(output);
	}

	private void addBackpackUpgradeConversionRecipes(RecipeOutput recipeOutput) {
		RecipeOutput sbConditionalRecipeOutput = recipeOutput.withConditions(new ModLoadedCondition(SophisticatedBackpacks.MOD_ID));

		addStorageStackUpgradeFromBackpackStackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_UPGRADE_TIER_1_PLUS.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_STARTER_TIER.get());
		addStorageStackUpgradeFromBackpackStackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_UPGRADE_TIER_2.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_TIER_1.get());
		addStorageStackUpgradeFromBackpackStackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_UPGRADE_TIER_3.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_TIER_2.get());
		addStorageStackUpgradeFromBackpackStackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_UPGRADE_TIER_4.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_TIER_3.get());
		addStorageStackUpgradeFromBackpackStackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_UPGRADE_TIER_5.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_TIER_4.get());
		addStorageStackUpgradeFromBackpackStackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_UPGRADE_OMEGA_TIER.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_OMEGA_TIER.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_DOWNGRADE_TIER_1.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_DOWNGRADE_TIER_1.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_DOWNGRADE_TIER_2.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_DOWNGRADE_TIER_2.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STACK_DOWNGRADE_TIER_3.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_DOWNGRADE_TIER_3.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.PICKUP_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.PICKUP_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ADVANCED_PICKUP_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_PICKUP_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.MAGNET_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.MAGNET_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ADVANCED_MAGNET_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_MAGNET_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.FILTER_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.FILTER_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ADVANCED_FILTER_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_FILTER_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.CRAFTING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CRAFTING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.FEEDING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.FEEDING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ADVANCED_FEEDING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_FEEDING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.COMPACTING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.COMPACTING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ADVANCED_COMPACTING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_COMPACTING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.VOID_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.VOID_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ADVANCED_VOID_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_VOID_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.SMELTING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.SMELTING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.AUTO_SMELTING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.AUTO_SMELTING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.SMOKING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.SMOKING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.AUTO_SMOKING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.AUTO_SMOKING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.BLASTING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BLASTING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.AUTO_BLASTING_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.AUTO_BLASTING_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.STONECUTTER_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STONECUTTER_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.JUKEBOX_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.JUKEBOX_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ADVANCED_JUKEBOX_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_JUKEBOX_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ADVANCED_ALCHEMY_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_ALCHEMY_UPGRADE.get());
		addStorageUpgradeFromBackpackUpgradeRecipe(sbConditionalRecipeOutput, ModItems.ALCHEMY_UPGRADE.get(),
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ALCHEMY_UPGRADE.get());

		addBackpackStackUpgradeFromStorageStackUpgradeRecipe(sbConditionalRecipeOutput,
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_STARTER_TIER.get(), ModItems.STACK_UPGRADE_TIER_1_PLUS.get());
		addBackpackStackUpgradeFromStorageStackUpgradeRecipe(sbConditionalRecipeOutput,
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_TIER_1.get(), ModItems.STACK_UPGRADE_TIER_2.get());
		addBackpackStackUpgradeFromStorageStackUpgradeRecipe(sbConditionalRecipeOutput,
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_TIER_2.get(), ModItems.STACK_UPGRADE_TIER_3.get());
		addBackpackStackUpgradeFromStorageStackUpgradeRecipe(sbConditionalRecipeOutput,
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_TIER_3.get(), ModItems.STACK_UPGRADE_TIER_4.get());
		addBackpackStackUpgradeFromStorageStackUpgradeRecipe(sbConditionalRecipeOutput,
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_TIER_4.get(), ModItems.STACK_UPGRADE_TIER_5.get());
		addBackpackStackUpgradeFromStorageStackUpgradeRecipe(sbConditionalRecipeOutput,
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_UPGRADE_OMEGA_TIER.get(), ModItems.STACK_UPGRADE_OMEGA_TIER.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_DOWNGRADE_TIER_1.get(),
				ModItems.STACK_DOWNGRADE_TIER_1.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_DOWNGRADE_TIER_2.get(),
				ModItems.STACK_DOWNGRADE_TIER_2.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STACK_DOWNGRADE_TIER_3.get(),
				ModItems.STACK_DOWNGRADE_TIER_3.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.PICKUP_UPGRADE.get(),
				ModItems.PICKUP_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_PICKUP_UPGRADE.get(),
				ModItems.ADVANCED_PICKUP_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.MAGNET_UPGRADE.get(),
				ModItems.MAGNET_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_MAGNET_UPGRADE.get(),
				ModItems.ADVANCED_MAGNET_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.FILTER_UPGRADE.get(),
				ModItems.FILTER_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_FILTER_UPGRADE.get(),
				ModItems.ADVANCED_FILTER_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CRAFTING_UPGRADE.get(),
				ModItems.CRAFTING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.FEEDING_UPGRADE.get(),
				ModItems.FEEDING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_FEEDING_UPGRADE.get(),
				ModItems.ADVANCED_FEEDING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.COMPACTING_UPGRADE.get(),
				ModItems.COMPACTING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput,
				net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_COMPACTING_UPGRADE.get(), ModItems.ADVANCED_COMPACTING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.VOID_UPGRADE.get(),
				ModItems.VOID_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_VOID_UPGRADE.get(),
				ModItems.ADVANCED_VOID_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.SMELTING_UPGRADE.get(),
				ModItems.SMELTING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.AUTO_SMELTING_UPGRADE.get(),
				ModItems.AUTO_SMELTING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.SMOKING_UPGRADE.get(),
				ModItems.SMOKING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.AUTO_SMOKING_UPGRADE.get(),
				ModItems.AUTO_SMOKING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BLASTING_UPGRADE.get(),
				ModItems.BLASTING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.AUTO_BLASTING_UPGRADE.get(),
				ModItems.AUTO_BLASTING_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.STONECUTTER_UPGRADE.get(),
				ModItems.STONECUTTER_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.JUKEBOX_UPGRADE.get(),
				ModItems.JUKEBOX_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_JUKEBOX_UPGRADE.get(),
				ModItems.ADVANCED_JUKEBOX_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_ALCHEMY_UPGRADE.get(),
				ModItems.ADVANCED_ALCHEMY_UPGRADE.get());
		addBackpackUpgradeFromStorageUpgradeRecipe(sbConditionalRecipeOutput, net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ALCHEMY_UPGRADE.get(),
				ModItems.ALCHEMY_UPGRADE.get());
	}

	private void addBackpackStackUpgradeFromStorageStackUpgradeRecipe(RecipeOutput recipeOutput, StackUpgradeItem backpackStackUpgrade,
			StackUpgradeItem storageStackUpgrade) {
		ShapeBasedRecipeBuilder.shaped(items, backpackStackUpgrade).pattern("TST").pattern("SLS").pattern("T T").define('T', Tags.Items.STRINGS)
				.define('L', Tags.Items.LEATHERS).define('S', storageStackUpgrade).unlockedBy("has_storage_stack_upgrade", has(storageStackUpgrade))
				.save(recipeOutput,
						ResourceKey.create(Registries.RECIPE,
								SophisticatedStorage.getIdentifier("backpack_" + RegistryHelper.getItemKey(backpackStackUpgrade).getPath() + "_from_storage_"
										+ RegistryHelper.getItemKey(storageStackUpgrade).getPath())));
	}

	private void addBackpackUpgradeFromStorageUpgradeRecipe(RecipeOutput recipeOutput, UpgradeItemBase<?> backpackUpgrade, UpgradeItemBase<?> storageUpgrade) {
		ShapeBasedRecipeBuilder.shaped(items, backpackUpgrade).pattern("TUT").pattern(" L ").pattern("T T").define('T', Tags.Items.STRINGS)
				.define('L', Tags.Items.LEATHERS).define('U', storageUpgrade).unlockedBy("has_storage_upgrade", has(storageUpgrade))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("backpack_"
						+ RegistryHelper.getItemKey(backpackUpgrade).getPath() + "_from_storage_" + RegistryHelper.getItemKey(storageUpgrade).getPath())));
	}

	private void addStorageUpgradeFromBackpackUpgradeRecipe(RecipeOutput recipeOutput, UpgradeItemBase<?> storageUpgrade, UpgradeItemBase<?> backpackUpgrade) {
		ShapeBasedRecipeBuilder.shaped(items, storageUpgrade).pattern("PUP").pattern(" P ").pattern("P P").define('P', ItemTags.PLANKS)
				.define('U', backpackUpgrade).unlockedBy("has_backpack_upgrade", has(backpackUpgrade))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("storage_"
						+ RegistryHelper.getItemKey(storageUpgrade).getPath() + "_from_backpack_" + RegistryHelper.getItemKey(backpackUpgrade).getPath())));
	}

	private void addStorageStackUpgradeFromBackpackStackUpgradeRecipe(RecipeOutput recipeOutput, StackUpgradeItem storageStackUpgrade,
			StackUpgradeItem backpackStackUpgrade) {
		ShapeBasedRecipeBuilder.shaped(items, storageStackUpgrade, 3).pattern("PSP").pattern(" P ").pattern("P P").define('P', ItemTags.PLANKS)
				.define('S', backpackStackUpgrade).unlockedBy("has_backpack_stack_upgrade", has(backpackStackUpgrade)).save(recipeOutput,
						ResourceKey.create(Registries.RECIPE,
								SophisticatedStorage.getIdentifier("storage_" + RegistryHelper.getItemKey(storageStackUpgrade).getPath() + "_from_backpack_"
										+ RegistryHelper.getItemKey(backpackStackUpgrade).getPath())));
	}

	private void addLimitedBarrelRecipes(RecipeOutput recipeOutput) {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach((woodType, blockFamily) -> {
			limitedWoodBarrel1Recipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB));
			limitedWoodBarrel2Recipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB));
			limitedWoodBarrel3Recipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB));
			limitedWoodBarrel4Recipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB));
		});

		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.LIMITED_BARREL_1_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(),
				ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(),
				ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get());
		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.LIMITED_BARREL_2_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(),
				ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(),
				ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get());
		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.LIMITED_BARREL_3_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(),
				ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(),
				ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get());
		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.LIMITED_BARREL_4_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(),
				ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(),
				ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get());

		ShapeBasedRecipeBuilder
				.shaped(items, ModBlocks.LIMITED_BARREL_1_ITEM.get(), woodStorageTemplate(ModBlocks.LIMITED_BARREL_1_ITEM.get(), WoodType.SPRUCE),
						GenericWoodStorageRecipe::new)
				.pattern("PSP").pattern("PLP").pattern("PPP").define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('L', Blocks.LEVER)
				.unlockedBy("has " + PLANK_SUFFIX, has(ItemTags.PLANKS))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("generic_limited_barrel_1")));

		ShapeBasedRecipeBuilder
				.shaped(items, ModBlocks.LIMITED_BARREL_2_ITEM.get(), woodStorageTemplate(ModBlocks.LIMITED_BARREL_2_ITEM.get(), WoodType.SPRUCE),
						GenericWoodStorageRecipe::new)
				.pattern("PPP").pattern("SLS").pattern("PPP").define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('L', Blocks.LEVER)
				.unlockedBy("has " + PLANK_SUFFIX, has(ItemTags.PLANKS))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("generic_limited_barrel_2")));

		ShapeBasedRecipeBuilder
				.shaped(items, ModBlocks.LIMITED_BARREL_3_ITEM.get(), woodStorageTemplate(ModBlocks.LIMITED_BARREL_3_ITEM.get(), WoodType.SPRUCE),
						GenericWoodStorageRecipe::new)
				.pattern("PSP").pattern("PLP").pattern("SPS").define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('L', Blocks.LEVER)
				.unlockedBy("has " + PLANK_SUFFIX, has(ItemTags.PLANKS))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("generic_limited_barrel_3")));

		ShapeBasedRecipeBuilder
				.shaped(items, ModBlocks.LIMITED_BARREL_4_ITEM.get(), woodStorageTemplate(ModBlocks.LIMITED_BARREL_4_ITEM.get(), WoodType.SPRUCE),
						GenericWoodStorageRecipe::new)
				.pattern("SPS").pattern("PLP").pattern("SPS").define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('L', Blocks.LEVER)
				.unlockedBy("has " + PLANK_SUFFIX, has(ItemTags.PLANKS))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("generic_limited_barrel_4")));

	}

	private void addStorageTierUpgradeRecipes(RecipeOutput recipeOutput, BlockItem baseTierItem, BlockItem copperTierItem, BlockItem ironTierItem,
			BlockItem goldTierItem, BlockItem diamondTierItem, BlockItem netheriteTierItem) {
		ShapeBasedRecipeBuilder.shaped(items, copperTierItem, StorageTierUpgradeRecipe::new).pattern("CCC").pattern("CSC").pattern("CCC")
				.define('C', Tags.Items.INGOTS_COPPER).define('S', baseTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(baseTierItem).getPath(), has(baseTierItem)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ironTierItem, StorageTierUpgradeRecipe::new).pattern(" I ").pattern("ISI").pattern(" I ")
				.define('I', Tags.Items.INGOTS_IRON).define('S', copperTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(copperTierItem).getPath(), has(copperTierItem))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage
						.getIdentifier(RegistryHelper.getItemKey(ironTierItem).getPath() + "_from_" + RegistryHelper.getItemKey(copperTierItem).getPath())));

		ShapeBasedRecipeBuilder.shaped(items, ironTierItem, StorageTierUpgradeRecipe::new).pattern("III").pattern("ISI").pattern("III")
				.define('I', Tags.Items.INGOTS_IRON).define('S', baseTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(baseTierItem).getPath(), has(baseTierItem)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, goldTierItem, StorageTierUpgradeRecipe::new).pattern("GGG").pattern("GSG").pattern("GGG")
				.define('G', Tags.Items.INGOTS_GOLD).define('S', ironTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(ironTierItem).getPath(), has(ironTierItem)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, diamondTierItem, StorageTierUpgradeRecipe::new).pattern("DDD").pattern("DSD").pattern("DDD")
				.define('D', Tags.Items.GEMS_DIAMOND).define('S', goldTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(goldTierItem).getPath(), has(goldTierItem)).save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, netheriteTierItem, StorageTierUpgradeShapelessRecipe::new).requires(Ingredient.of(diamondTierItem))
				.requires(Tags.Items.INGOTS_NETHERITE).unlockedBy("has_" + RegistryHelper.getItemKey(diamondTierItem).getPath(), has(diamondTierItem))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, RegistryHelper.getItemKey(netheriteTierItem)));
	}

	private void addDoubleChestTierUpgradeRecipes(RecipeOutput recipeOutput, BlockItem baseTierItem, BlockItem copperTierItem, BlockItem ironTierItem,
			BlockItem goldTierItem, BlockItem diamondTierItem, BlockItem netheriteTierItem) {
		ShapeBasedRecipeBuilder.shaped(items, copperTierItem, DoubleChestTierUpgradeRecipe::new).pattern("CCC").pattern("CSC").pattern("CBC")
				.define('C', Tags.Items.INGOTS_COPPER).define('B', Tags.Items.STORAGE_BLOCKS_COPPER).define('S', baseTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(baseTierItem).getPath(), has(baseTierItem)).save(recipeOutput, ResourceKey
						.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("double_" + RegistryHelper.getItemKey(copperTierItem).getPath())));

		ShapeBasedRecipeBuilder.shaped(items, ironTierItem, DoubleChestTierUpgradeRecipe::new).pattern("III").pattern("ISI").pattern("III")
				.define('I', Tags.Items.INGOTS_IRON).define('S', copperTierItem)
				.unlockedBy("has_" + RegistryHelper.getItemKey(copperTierItem).getPath(), has(copperTierItem))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier(
						"double_" + RegistryHelper.getItemKey(ironTierItem).getPath() + "_from_" + RegistryHelper.getItemKey(copperTierItem).getPath())));

		ShapeBasedRecipeBuilder.shaped(items, ironTierItem, DoubleChestTierUpgradeRecipe::new).pattern("III").pattern("ISI").pattern("IBI")
				.define('I', Tags.Items.INGOTS_IRON).define('S', baseTierItem).define('B', Tags.Items.STORAGE_BLOCKS_IRON)
				.unlockedBy("has_" + RegistryHelper.getItemKey(baseTierItem).getPath(), has(baseTierItem)).save(recipeOutput, ResourceKey
						.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("double_" + RegistryHelper.getItemKey(ironTierItem).getPath())));

		ShapeBasedRecipeBuilder.shaped(items, goldTierItem, DoubleChestTierUpgradeRecipe::new).pattern("GGG").pattern("GSG").pattern("GBG")
				.define('G', Tags.Items.INGOTS_GOLD).define('S', ironTierItem).define('B', Tags.Items.STORAGE_BLOCKS_GOLD)
				.unlockedBy("has_" + RegistryHelper.getItemKey(ironTierItem).getPath(), has(ironTierItem)).save(recipeOutput, ResourceKey
						.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("double_" + RegistryHelper.getItemKey(goldTierItem).getPath())));

		ShapeBasedRecipeBuilder.shaped(items, diamondTierItem, DoubleChestTierUpgradeRecipe::new).pattern("DDD").pattern("DSD").pattern("DBD")
				.define('D', Tags.Items.GEMS_DIAMOND).define('S', goldTierItem).define('B', Tags.Items.STORAGE_BLOCKS_DIAMOND)
				.unlockedBy("has_" + RegistryHelper.getItemKey(goldTierItem).getPath(), has(goldTierItem)).save(recipeOutput, ResourceKey
						.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("double_" + RegistryHelper.getItemKey(diamondTierItem).getPath())));

		ShapelessBasedRecipeBuilder.shapeless(items, netheriteTierItem, DoubleChestTierUpgradeShapelessRecipe::new).requires(Ingredient.of(diamondTierItem))
				.requires(Tags.Items.INGOTS_NETHERITE).requires(Tags.Items.INGOTS_NETHERITE)
				.unlockedBy("has_" + RegistryHelper.getItemKey(diamondTierItem).getPath(), has(diamondTierItem)).save(recipeOutput, ResourceKey
						.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("double_" + RegistryHelper.getItemKey(netheriteTierItem).getPath())));
	}

	private void addControllerRelatedRecipes(RecipeOutput recipeOutput) {
		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.CONTROLLER_ITEM.get()).pattern("SCS").pattern("PBP").pattern("SCS").define('S', Tags.Items.STONES)
				.define('C', Items.COMPARATOR).define('P', ItemTags.PLANKS).define('B', new BaseTierWoodenStorageIngredient().toVanilla())
				.unlockedBy(HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME, has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG)).save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, ModBlocks.STORAGE_LINK_ITEM.get(), 3).requires(ModBlocks.CONTROLLER_ITEM.get())
				.requires(Tags.Items.ENDER_PEARLS).unlockedBy("has_controller", has(ModBlocks.CONTROLLER_ITEM.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("storage_link_from_controller")));

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.STORAGE_LINK_ITEM.get()).pattern("EP").pattern("RS").define('E', Tags.Items.ENDER_PEARLS)
				.define('P', ItemTags.PLANKS).define('R', Items.REPEATER).define('S', Tags.Items.STONES).unlockedBy("has_repeater", has(Items.REPEATER))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STORAGE_TOOL.get()).pattern(" EI").pattern(" SR").pattern("S  ").define('E', Tags.Items.ENDER_PEARLS)
				.define('I', Tags.Items.INGOTS_IRON).define('S', Tags.Items.RODS_WOODEN).define('R', Items.REDSTONE_TORCH)
				.unlockedBy(HAS_REDSTONE_TORCH_CRITERION_NAME, has(Items.REDSTONE_TORCH)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.STORAGE_IO_ITEM.get()).pattern("SPS").pattern("RBG").pattern("SPS").define('S', Tags.Items.STONES)
				.define('P', ItemTags.PLANKS).define('R', Items.REPEATER).define('G', Tags.Items.INGOTS_GOLD)
				.define('B', new BaseTierWoodenStorageIngredient().toVanilla())
				.unlockedBy(HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME, has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.STORAGE_OUTPUT_ITEM.get()).pattern("SGS").pattern("PBP").pattern("SRS").define('S', Tags.Items.STONES)
				.define('P', ItemTags.PLANKS).define('R', Items.REPEATER).define('G', Tags.Items.INGOTS_GOLD)
				.define('B', new BaseTierWoodenStorageIngredient().toVanilla())
				.unlockedBy(HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME, has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.STORAGE_INPUT_ITEM.get()).pattern("SRS").pattern("PBP").pattern("SGS").define('S', Tags.Items.STONES)
				.define('P', ItemTags.PLANKS).define('R', Items.REPEATER).define('G', Tags.Items.INGOTS_GOLD)
				.define('B', new BaseTierWoodenStorageIngredient().toVanilla())
				.unlockedBy(HAS_BASE_TIER_WOODEN_STORAGE_CRITERION_NAME, has(ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG)).save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, ModBlocks.STORAGE_INPUT_ITEM.get()).requires(ModBlocks.STORAGE_IO_ITEM.get())
				.unlockedBy("has_storage_io", has(ModBlocks.STORAGE_IO_ITEM.get())).save(recipeOutput, "storage_input_from_io");

		ShapelessBasedRecipeBuilder.shapeless(items, ModBlocks.STORAGE_OUTPUT_ITEM.get()).requires(ModBlocks.STORAGE_INPUT_ITEM.get())
				.unlockedBy("has_storage_input", has(ModBlocks.STORAGE_INPUT_ITEM.get())).save(recipeOutput, "storage_output_from_input");

		ShapelessBasedRecipeBuilder.shapeless(items, ModBlocks.STORAGE_IO_ITEM.get()).requires(ModBlocks.STORAGE_OUTPUT_ITEM.get())
				.unlockedBy("has_storage_output", has(ModBlocks.STORAGE_OUTPUT_ITEM.get())).save(recipeOutput, "storage_io_from_output");

		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach((woodType, blockSupplier) -> {
			Block plankBlock = blockSupplier.getBaseBlock();
			ShapeBasedRecipeBuilder.shaped(items, ModBlocks.STORAGE_CONNECTOR_BLOCKS.get(woodType).get()).pattern("SPS").pattern("PSP").pattern("SPS")
					.define('S', Items.STICK).define('P', plankBlock).unlockedBy("has_" + RegistryHelper.getBlockKey(plankBlock).getPath(), has(plankBlock))
					.save(recipeOutput);
		});
	}

	private void addShulkerBoxRecipes(RecipeOutput recipeOutput) {
		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.SHULKER_BOX_ITEM.get()).pattern(" S").pattern("LC").pattern(" S").define('L', Items.LEVER)
				.define('S', Items.SHULKER_SHELL).define('C', Tags.Items.CHESTS).unlockedBy("has_shulker_shell", has(Items.SHULKER_SHELL)).save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, ModBlocks.SHULKER_BOX_ITEM.get(), ShulkerBoxFromVanillaShapelessRecipe::new).requires(Items.SHULKER_BOX)
				.requires(Items.LEVER).unlockedBy("has_shulker_box", has(Items.SHULKER_BOX)).save(recipeOutput, "shulker_box_from_vanilla_shulker_box");

		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.black(), DyeColor.BLACK);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.blue(), DyeColor.BLUE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.brown(), DyeColor.BROWN);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.cyan(), DyeColor.CYAN);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.gray(), DyeColor.GRAY);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.green(), DyeColor.GREEN);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.lightBlue(), DyeColor.LIGHT_BLUE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.lightGray(), DyeColor.LIGHT_GRAY);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.lime(), DyeColor.LIME);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.magenta(), DyeColor.MAGENTA);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.orange(), DyeColor.ORANGE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.pink(), DyeColor.PINK);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.purple(), DyeColor.PURPLE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.red(), DyeColor.RED);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.white(), DyeColor.WHITE);
		tintedShulkerBoxRecipe(recipeOutput, Blocks.DYED_SHULKER_BOX.yellow(), DyeColor.YELLOW);

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new).pattern("S").pattern("C").pattern("S")
				.define('C', ModBlocks.CHEST_ITEM.get()).define('S', Items.SHULKER_SHELL).unlockedBy("has_chest", has(ModBlocks.CHEST_ITEM.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("shulker_from_chest")));

		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.SHULKER_BOX_ITEM.get(), ModBlocks.COPPER_SHULKER_BOX_ITEM.get(),
				ModBlocks.IRON_SHULKER_BOX_ITEM.get(), ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(),
				ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get());

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new).pattern("S").pattern("C").pattern("S")
				.define('C', ModBlocks.COPPER_CHEST_ITEM.get()).define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_copper_chest", has(ModBlocks.COPPER_CHEST_ITEM.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("copper_shulker_from_copper_chest")));

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.IRON_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new).pattern("S").pattern("C").pattern("S")
				.define('C', ModBlocks.IRON_CHEST_ITEM.get()).define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_iron_chest", has(ModBlocks.IRON_CHEST_ITEM.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("iron_shulker_from_iron_chest")));

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new).pattern("S").pattern("C").pattern("S")
				.define('C', ModBlocks.GOLD_CHEST_ITEM.get()).define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_gold_chest", has(ModBlocks.GOLD_CHEST_ITEM.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("gold_shulker_from_gold_chest")));

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new).pattern("S").pattern("C").pattern("S")
				.define('C', ModBlocks.DIAMOND_CHEST_ITEM.get()).define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_diamond_chest", has(ModBlocks.DIAMOND_CHEST_ITEM.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("diamond_shulker_from_diamond_chest")));

		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get(), ShulkerBoxFromChestRecipe::new).pattern("S").pattern("C").pattern("S")
				.define('C', ModBlocks.NETHERITE_CHEST_ITEM.get()).define('S', Items.SHULKER_SHELL)
				.unlockedBy("has_netherite_chest", has(ModBlocks.NETHERITE_CHEST_ITEM.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("netherite_shulker_from_netherite_chest")));
	}

	private void addTierUpgradeItemRecipes(RecipeOutput recipeOutput) {
		ShapeBasedRecipeBuilder.shaped(items, ModItems.BASIC_TIER_UPGRADE.get()).pattern(" S ").pattern("SLS").pattern(" S ").define('L', Items.LEVER)
				.define('S', Tags.Items.RODS_WOODEN).unlockedBy(HAS_LEVER_CRITERION_NAME, has(Items.LEVER)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.BASIC_TO_COPPER_TIER_UPGRADE.get()).pattern("CCC").pattern("CLC").pattern("CCC").define('L', Items.LEVER)
				.define('C', Tags.Items.INGOTS_COPPER).unlockedBy(HAS_LEVER_CRITERION_NAME, has(Items.LEVER)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.BASIC_TO_IRON_TIER_UPGRADE.get()).pattern("III").pattern("ILI").pattern("III").define('L', Items.LEVER)
				.define('I', Tags.Items.INGOTS_IRON).unlockedBy(HAS_LEVER_CRITERION_NAME, has(Items.LEVER)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.BASIC_TO_IRON_TIER_UPGRADE.get()).pattern(" I ").pattern("IRI").pattern(" I ")
				.define('R', ModItems.BASIC_TO_COPPER_TIER_UPGRADE.get()).define('I', Tags.Items.INGOTS_IRON)
				.unlockedBy("has_basic_to_copper_tier_upgrade", has(ModItems.BASIC_TO_COPPER_TIER_UPGRADE.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("basic_to_iron_tier_from_basic_to_copper_tier")));

		ShapeBasedRecipeBuilder.shaped(items, ModItems.BASIC_TO_GOLD_TIER_UPGRADE.get()).pattern("GGG").pattern("GTG").pattern("GGG")
				.define('T', ModItems.BASIC_TO_IRON_TIER_UPGRADE.get()).define('G', Tags.Items.INGOTS_GOLD)
				.unlockedBy("has_basic_to_iron_tier_upgrade", has(ModItems.BASIC_TO_IRON_TIER_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get()).pattern("DDD").pattern("DTD").pattern("DDD")
				.define('T', ModItems.BASIC_TO_GOLD_TIER_UPGRADE.get()).define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy("has_basic_to_gold_tier_upgrade", has(ModItems.BASIC_TO_GOLD_TIER_UPGRADE.get())).save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, ModItems.BASIC_TO_NETHERITE_TIER_UPGRADE.get()).requires(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get())
				.requires(Tags.Items.INGOTS_NETHERITE).unlockedBy("has_basic_to_diamond_tier_upgrade", has(ModItems.BASIC_TO_DIAMOND_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.COPPER_TO_IRON_TIER_UPGRADE.get()).pattern(" I ").pattern("ILI").pattern(" I ").define('L', Items.LEVER)
				.define('I', Tags.Items.INGOTS_IRON).unlockedBy(HAS_LEVER_CRITERION_NAME, has(Items.LEVER)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.COPPER_TO_GOLD_TIER_UPGRADE.get()).pattern("GGG").pattern("GTG").pattern("GGG")
				.define('T', ModItems.COPPER_TO_IRON_TIER_UPGRADE.get()).define('G', Tags.Items.INGOTS_GOLD)
				.unlockedBy("has_copper_to_iron_tier_upgrade", has(ModItems.COPPER_TO_IRON_TIER_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.COPPER_TO_DIAMOND_TIER_UPGRADE.get()).pattern("DDD").pattern("DTD").pattern("DDD")
				.define('T', ModItems.COPPER_TO_GOLD_TIER_UPGRADE.get()).define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy("has_copper_to_gold_tier_upgrade", has(ModItems.COPPER_TO_GOLD_TIER_UPGRADE.get())).save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, ModItems.COPPER_TO_NETHERITE_TIER_UPGRADE.get()).requires(ModItems.COPPER_TO_DIAMOND_TIER_UPGRADE.get())
				.requires(Tags.Items.INGOTS_NETHERITE).unlockedBy("has_copper_to_diamond_tier_upgrade", has(ModItems.COPPER_TO_DIAMOND_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.IRON_TO_GOLD_TIER_UPGRADE.get()).pattern("GGG").pattern("GLG").pattern("GGG").define('L', Items.LEVER)
				.define('G', Tags.Items.INGOTS_GOLD).unlockedBy(HAS_LEVER_CRITERION_NAME, has(Items.LEVER)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.IRON_TO_DIAMOND_TIER_UPGRADE.get()).pattern("DDD").pattern("DTD").pattern("DDD")
				.define('T', ModItems.IRON_TO_GOLD_TIER_UPGRADE.get()).define('D', Tags.Items.GEMS_DIAMOND)
				.unlockedBy("has_iron_to_gold_tier_upgrade", has(ModItems.IRON_TO_GOLD_TIER_UPGRADE.get())).save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, ModItems.IRON_TO_NETHERITE_TIER_UPGRADE.get()).requires(ModItems.IRON_TO_DIAMOND_TIER_UPGRADE.get())
				.requires(Tags.Items.INGOTS_NETHERITE).unlockedBy("has_iron_to_diamond_tier_upgrade", has(ModItems.IRON_TO_DIAMOND_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get()).pattern("DDD").pattern("DLD").pattern("DDD").define('L', Items.LEVER)
				.define('D', Tags.Items.GEMS_DIAMOND).unlockedBy(HAS_LEVER_CRITERION_NAME, has(Items.LEVER)).save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, ModItems.GOLD_TO_NETHERITE_TIER_UPGRADE.get()).requires(ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get())
				.requires(Tags.Items.INGOTS_NETHERITE).unlockedBy("has_gold_to_diamond_tier_upgrade", has(ModItems.GOLD_TO_DIAMOND_TIER_UPGRADE.get()))
				.save(recipeOutput);

		ShapelessBasedRecipeBuilder.shapeless(items, ModItems.DIAMOND_TO_NETHERITE_TIER_UPGRADE.get()).requires(Items.LEVER)
				.requires(Tags.Items.INGOTS_NETHERITE).unlockedBy(HAS_LEVER_CRITERION_NAME, has(Items.LEVER)).save(recipeOutput);
	}

	private void addUpgradeRecipes(RecipeOutput recipeOutput) {
		ShapeBasedRecipeBuilder.shaped(items, ModItems.UPGRADE_BASE.get()).pattern("PIP").pattern("IPI").pattern("PIP").define('P', ItemTags.PLANKS)
				.define('I', Tags.Items.INGOTS_IRON).unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON)).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.PICKUP_UPGRADE.get()).pattern(" P ").pattern("LBL").pattern("RRR")
				.define('B', ModItems.UPGRADE_BASE.get()).define('R', Tags.Items.DUSTS_REDSTONE).define('L', ItemTags.PLANKS).define('P', Blocks.STICKY_PISTON)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_PICKUP_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern(" D ").pattern("GPG").pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('P', ModItems.PICKUP_UPGRADE.get()).unlockedBy("has_pickup_upgrade", has(ModItems.PICKUP_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.FILTER_UPGRADE.get()).pattern("RSR").pattern("SBS").pattern("RSR")
				.define('B', ModItems.UPGRADE_BASE.get()).define('R', Tags.Items.DUSTS_REDSTONE).define('S', Tags.Items.STRINGS)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_FILTER_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern("GPG").pattern("RRR")
				.define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE).define('P', ModItems.FILTER_UPGRADE.get())
				.unlockedBy("has_filter_upgrade", has(ModItems.FILTER_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.MAGNET_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern("EIE").pattern("IPI").pattern("R L")
				.define('E', Tags.Items.ENDER_PEARLS).define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('L', Tags.Items.GEMS_LAPIS).define('P', ModItems.PICKUP_UPGRADE.get())
				.unlockedBy("has_pickup_upgrade", has(ModItems.PICKUP_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_MAGNET_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern("EIE").pattern("IPI").pattern("R L")
				.define('E', Tags.Items.ENDER_PEARLS).define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('L', Tags.Items.GEMS_LAPIS).define('P', ModItems.ADVANCED_PICKUP_UPGRADE.get())
				.unlockedBy("has_advanced_pickup_upgrade", has(ModItems.ADVANCED_PICKUP_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_MAGNET_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern(" D ").pattern("GMG").pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('M', ModItems.MAGNET_UPGRADE.get()).unlockedBy("has_magnet_upgrade", has(ModItems.MAGNET_UPGRADE.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("advanced_magnet_upgrade_from_basic")));

		ShapeBasedRecipeBuilder.shaped(items, ModItems.FEEDING_UPGRADE.get()).pattern(" C ").pattern("ABM").pattern(" E ")
				.define('B', ModItems.UPGRADE_BASE.get()).define('C', Items.GOLDEN_CARROT).define('A', Items.GOLDEN_APPLE)
				.define('M', Items.GLISTERING_MELON_SLICE).define('E', Tags.Items.ENDER_PEARLS)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.COMPACTING_UPGRADE.get()).pattern("IPI").pattern("PBP").pattern("RPR")
				.define('B', ModItems.UPGRADE_BASE.get()).define('I', Tags.Items.INGOTS_IRON).define('P', Items.PISTON).define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_COMPACTING_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern(" D ").pattern("GCG")
				.pattern("RRR").define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('C', ModItems.COMPACTING_UPGRADE.get()).unlockedBy("has_compacting_upgrade", has(ModItems.COMPACTING_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.VOID_UPGRADE.get()).pattern(" E ").pattern("OBO").pattern("ROR").define('B', ModItems.UPGRADE_BASE.get())
				.define('E', Tags.Items.ENDER_PEARLS).define('O', Tags.Items.OBSIDIANS).define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_VOID_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern(" D ").pattern("GVG").pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('V', ModItems.VOID_UPGRADE.get()).unlockedBy("has_void_upgrade", has(ModItems.VOID_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.SMELTING_UPGRADE.get()).pattern("RIR").pattern("IBI").pattern("RFR")
				.define('B', ModItems.UPGRADE_BASE.get()).define('R', Tags.Items.DUSTS_REDSTONE).define('I', Tags.Items.INGOTS_IRON).define('F', Items.FURNACE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.AUTO_SMELTING_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern("DHD").pattern("RSH").pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE).define('H', Items.HOPPER)
				.define('S', ModItems.SMELTING_UPGRADE.get()).unlockedBy(HAS_SMELTING_UPGRADE_CRITERION_NAME, has(ModItems.SMELTING_UPGRADE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.CRAFTING_UPGRADE.get()).pattern(" T ").pattern("IBI").pattern(" C ")
				.define('B', ModItems.UPGRADE_BASE.get()).define('C', Tags.Items.CHESTS).define('I', Tags.Items.INGOTS_IRON).define('T', Items.CRAFTING_TABLE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STONECUTTER_UPGRADE.get()).pattern(" S ").pattern("IBI").pattern(" R ")
				.define('B', ModItems.UPGRADE_BASE.get()).define('R', Tags.Items.DUSTS_REDSTONE).define('I', Tags.Items.INGOTS_IRON)
				.define('S', Items.STONECUTTER).unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_UPGRADE_TIER_1.get()).pattern("LLL").pattern("LBL").pattern("LLL")
				.define('B', ModItems.UPGRADE_BASE.get()).define('L', ItemTags.LOGS)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_UPGRADE_TIER_1_PLUS.get()).pattern("CCC").pattern("CSC").pattern("BCB")
				.define('S', ModItems.STACK_UPGRADE_TIER_1.get()).define('C', Tags.Items.INGOTS_COPPER).define('B', Tags.Items.STORAGE_BLOCKS_COPPER)
				.unlockedBy("has_stack_upgrade_tier_1", has(ModItems.STACK_UPGRADE_TIER_1.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_UPGRADE_TIER_2.get()).pattern(" I ").pattern("ISI").pattern(" B ")
				.define('S', ModItems.STACK_UPGRADE_TIER_1_PLUS.get()).define('I', Tags.Items.INGOTS_IRON).define('B', Tags.Items.STORAGE_BLOCKS_IRON)
				.unlockedBy("has_stack_upgrade_tier_1_plus", has(ModItems.STACK_UPGRADE_TIER_1_PLUS.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("stack_upgrade_tier_2_from_tier_1_plus")));

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_UPGRADE_TIER_2.get()).pattern("III").pattern("ISI").pattern("BIB")
				.define('S', ModItems.STACK_UPGRADE_TIER_1.get()).define('I', Tags.Items.INGOTS_IRON).define('B', Tags.Items.STORAGE_BLOCKS_IRON)
				.unlockedBy("has_stack_upgrade_tier_1", has(ModItems.STACK_UPGRADE_TIER_1.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_UPGRADE_TIER_3.get()).pattern("GGG").pattern("GSG").pattern("BGB")
				.define('S', ModItems.STACK_UPGRADE_TIER_2.get()).define('G', Tags.Items.INGOTS_GOLD).define('B', Tags.Items.STORAGE_BLOCKS_GOLD)
				.unlockedBy("has_stack_upgrade_tier_2", has(ModItems.STACK_UPGRADE_TIER_2.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_UPGRADE_TIER_4.get()).pattern("DDD").pattern("DSD").pattern("BDB")
				.define('S', ModItems.STACK_UPGRADE_TIER_3.get()).define('D', Tags.Items.GEMS_DIAMOND).define('B', Tags.Items.STORAGE_BLOCKS_DIAMOND)
				.unlockedBy("has_stack_upgrade_tier_3", has(ModItems.STACK_UPGRADE_TIER_3.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_UPGRADE_TIER_5.get()).pattern("NNN").pattern("NSN").pattern("BNB")
				.define('S', ModItems.STACK_UPGRADE_TIER_4.get()).define('N', Tags.Items.INGOTS_NETHERITE).define('B', Tags.Items.STORAGE_BLOCKS_NETHERITE)
				.unlockedBy("has_stack_upgrade_tier_4", has(ModItems.STACK_UPGRADE_TIER_4.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_UPGRADE_OMEGA_TIER.get()).pattern("SSS").pattern("SSS").pattern("SSS")
				.define('S', ModItems.STACK_UPGRADE_TIER_5.get()).unlockedBy("has_stack_upgrade_tier_5", has(ModItems.STACK_UPGRADE_TIER_5.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_DOWNGRADE_TIER_1.get()).pattern("SFS").pattern("SBS").pattern("FSF")
				.define('S', Tags.Items.RODS_WOODEN).define('F', Items.FLINT).define('B', ModItems.UPGRADE_BASE.get())
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_DOWNGRADE_TIER_2.get()).pattern("FSF").pattern("SBS").pattern("FSF")
				.define('S', Tags.Items.RODS_WOODEN).define('F', Items.FLINT).define('B', ModItems.UPGRADE_BASE.get())
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.STACK_DOWNGRADE_TIER_3.get()).pattern("SFS").pattern("FBF").pattern("FSF")
				.define('S', Tags.Items.RODS_WOODEN).define('F', Items.FLINT).define('B', ModItems.UPGRADE_BASE.get())
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.JUKEBOX_UPGRADE.get()).pattern(" J ").pattern("IBI").pattern(" R ")
				.define('B', ModItems.UPGRADE_BASE.get()).define('R', Tags.Items.DUSTS_REDSTONE).define('I', Tags.Items.INGOTS_IRON).define('J', Items.JUKEBOX)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_JUKEBOX_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern(" D ").pattern("GJG").pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('J', ModItems.JUKEBOX_UPGRADE.get()).unlockedBy("has_jukebox_upgrade", has(ModItems.JUKEBOX_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_FEEDING_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern(" D ").pattern("GVG").pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('V', ModItems.FEEDING_UPGRADE.get()).unlockedBy("has_feeding_upgrade", has(ModItems.FEEDING_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.SMOKING_UPGRADE.get()).pattern("RIR").pattern("IBI").pattern("RSR")
				.define('B', ModItems.UPGRADE_BASE.get()).define('R', Tags.Items.DUSTS_REDSTONE).define('I', Tags.Items.INGOTS_IRON).define('S', Items.SMOKER)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.SMOKING_UPGRADE.get()).pattern(" L ").pattern("LSL").pattern(" L ")
				.define('S', ModItems.SMELTING_UPGRADE.get()).define('L', ItemTags.LOGS)
				.unlockedBy(HAS_SMELTING_UPGRADE_CRITERION_NAME, has(ModItems.SMELTING_UPGRADE.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("smoking_upgrade_from_smelting_upgrade")));

		ShapeBasedRecipeBuilder.shaped(items, ModItems.AUTO_SMOKING_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern("DHD").pattern("RSH").pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE).define('H', Items.HOPPER)
				.define('S', ModItems.SMOKING_UPGRADE.get()).unlockedBy("has_smoking_upgrade", has(ModItems.SMOKING_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.AUTO_SMOKING_UPGRADE.get()).pattern(" L ").pattern("LSL").pattern(" L ")
				.define('S', ModItems.AUTO_SMELTING_UPGRADE.get()).define('L', ItemTags.LOGS)
				.unlockedBy("has_auto_smelting_upgrade", has(ModItems.AUTO_SMELTING_UPGRADE.get())).save(recipeOutput,
						ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("auto_smoking_upgrade_from_auto_smelting_upgrade")));

		ShapeBasedRecipeBuilder.shaped(items, ModItems.BLASTING_UPGRADE.get()).pattern("RIR").pattern("IBI").pattern("RFR")
				.define('B', ModItems.UPGRADE_BASE.get()).define('R', Tags.Items.DUSTS_REDSTONE).define('I', Tags.Items.INGOTS_IRON)
				.define('F', Items.BLAST_FURNACE).unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.BLASTING_UPGRADE.get()).pattern("III").pattern("ISI").pattern("TTT")
				.define('S', ModItems.SMELTING_UPGRADE.get()).define('I', Tags.Items.INGOTS_IRON).define('T', Items.SMOOTH_STONE)
				.unlockedBy(HAS_SMELTING_UPGRADE_CRITERION_NAME, has(ModItems.SMELTING_UPGRADE.get()))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("blasting_upgrade_from_smelting_upgrade")));

		ShapeBasedRecipeBuilder.shaped(items, ModItems.AUTO_BLASTING_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern("DHD").pattern("RSH").pattern("GHG")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE).define('H', Items.HOPPER)
				.define('S', ModItems.BLASTING_UPGRADE.get()).unlockedBy("has_blasting_upgrade", has(ModItems.BLASTING_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.AUTO_BLASTING_UPGRADE.get()).pattern("III").pattern("ISI").pattern("TTT")
				.define('S', ModItems.AUTO_SMELTING_UPGRADE.get()).define('I', Tags.Items.INGOTS_IRON).define('T', Items.SMOOTH_STONE)
				.unlockedBy("has_auto_smelting_upgrade", has(ModItems.AUTO_SMELTING_UPGRADE.get())).save(recipeOutput,
						ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("auto_blasting_upgrade_from_auto_smelting_upgrade")));

		ShapeBasedRecipeBuilder.shaped(items, ModItems.COMPRESSION_UPGRADE.get()).pattern(" I ").pattern("PBP").pattern("RIR")
				.define('B', ModItems.UPGRADE_BASE.get()).define('I', Tags.Items.INGOTS_IRON).define('P', Items.PISTON).define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.HOPPER_UPGRADE.get()).pattern(" H ").pattern("IBI").pattern("RRR")
				.define('B', ModItems.UPGRADE_BASE.get()).define('H', Items.HOPPER).define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_HOPPER_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern(" D ").pattern("GHG").pattern("ROR")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE).define('O', Items.DROPPER)
				.define('H', ModItems.HOPPER_UPGRADE.get()).unlockedBy("has_feeding_upgrade", has(ModItems.HOPPER_UPGRADE.get())).save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ALCHEMY_UPGRADE.get()).pattern("TGF").pattern("IBI").pattern("RPR").define('T', Items.GHAST_TEAR)
				.define('G', Items.GLASS_BOTTLE).define('F', Items.FERMENTED_SPIDER_EYE).define('R', Items.BLAZE_ROD).define('P', Items.ENDER_PEARL)
				.define('I', Tags.Items.INGOTS_IRON).define('B', ModItems.UPGRADE_BASE.get()).unlockedBy("has_upgrade_base", has(ModItems.UPGRADE_BASE.get()))
				.save(recipeOutput);

		ShapeBasedRecipeBuilder.shaped(items, ModItems.ADVANCED_ALCHEMY_UPGRADE.get(), UpgradeNextTierRecipe::new).pattern(" D ").pattern("GAG").pattern("RRR")
				.define('D', Tags.Items.GEMS_DIAMOND).define('G', Tags.Items.INGOTS_GOLD).define('R', Tags.Items.DUSTS_REDSTONE)
				.define('A', ModItems.ALCHEMY_UPGRADE.get()).unlockedBy("has_alchemy_upgrade", has(ModItems.ALCHEMY_UPGRADE.get())).save(recipeOutput);

		addCompatUpgradeRecipes(recipeOutput);
	}

	private void addCompatUpgradeRecipes(RecipeOutput recipeOutput) {
		// TODO readd when Chipped/Sawmill compat is updated
		/*
		 * addCompatUpgradeRecipe(recipeOutput, ChippedCompat.BOTANIST_WORKBENCH_UPGRADE.get(),
		 * earth.terrarium.chipped.common.registry.ModBlocks.BOTANIST_WORKBENCH.get(),
		 * net.p3pp3rf1y.sophisticatedbackpacks.compat.chipped.ChippedCompat.BOTANIST_WORKBENCH_UPGRADE.get(), CompatModIds.CHIPPED);
		 * addCompatUpgradeRecipe(recipeOutput, ChippedCompat.GLASSBLOWER_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.GLASSBLOWER.get(),
		 * net.p3pp3rf1y.sophisticatedbackpacks.compat.chipped.ChippedCompat.GLASSBLOWER_UPGRADE.get(), CompatModIds.CHIPPED);
		 * addCompatUpgradeRecipe(recipeOutput, ChippedCompat.CARPENTERS_TABLE_UPGRADE.get(),
		 * earth.terrarium.chipped.common.registry.ModBlocks.CARPENTERS_TABLE.get(),
		 * net.p3pp3rf1y.sophisticatedbackpacks.compat.chipped.ChippedCompat.CARPENTERS_TABLE_UPGRADE.get(), CompatModIds.CHIPPED);
		 * addCompatUpgradeRecipe(recipeOutput, ChippedCompat.LOOM_TABLE_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.LOOM_TABLE.get(),
		 * net.p3pp3rf1y.sophisticatedbackpacks.compat.chipped.ChippedCompat.LOOM_TABLE_UPGRADE.get(), CompatModIds.CHIPPED);
		 * addCompatUpgradeRecipe(recipeOutput, ChippedCompat.MASON_TABLE_UPGRADE.get(), earth.terrarium.chipped.common.registry.ModBlocks.MASON_TABLE.get(),
		 * net.p3pp3rf1y.sophisticatedbackpacks.compat.chipped.ChippedCompat.MASON_TABLE_UPGRADE.get(), CompatModIds.CHIPPED);
		 * addCompatUpgradeRecipe(recipeOutput, ChippedCompat.ALCHEMY_BENCH_UPGRADE.get(),
		 * earth.terrarium.chipped.common.registry.ModBlocks.ALCHEMY_BENCH.get(),
		 * net.p3pp3rf1y.sophisticatedbackpacks.compat.chipped.ChippedCompat.ALCHEMY_BENCH_UPGRADE.get(), CompatModIds.CHIPPED);
		 * addCompatUpgradeRecipe(recipeOutput, ChippedCompat.TINKERING_TABLE_UPGRADE.get(),
		 * earth.terrarium.chipped.common.registry.ModBlocks.TINKERING_TABLE.get(),
		 * net.p3pp3rf1y.sophisticatedbackpacks.compat.chipped.ChippedCompat.TINKERING_TABLE_UPGRADE.get(), CompatModIds.CHIPPED);
		 * addCompatUpgradeRecipe(recipeOutput, SawmillCompat.SAWMILL_UPGRADE.get(), SawmillMod.SAWMILL_BLOCK.get(),
		 * net.p3pp3rf1y.sophisticatedbackpacks.compat.sawmill.SawmillCompat.SAWMILL_UPGRADE.get(), ModCompat.SAWMILL_MOD_ID);
		 */
	}

	private void addCompatUpgradeRecipe(RecipeOutput recipeOutput, Item upgrade, Block workbench, Item backpackUpgrade, String modId) {
		RecipeOutput compatRecipeOutput = recipeOutput.withConditions(new ModLoadedCondition(modId));
		ShapeBasedRecipeBuilder.shaped(items, upgrade).pattern(" W ").pattern("IBI").pattern(" R ").define('B', ModItems.UPGRADE_BASE.get())
				.define('R', Tags.Items.DUSTS_REDSTONE).define('I', Tags.Items.INGOTS_IRON).define('W', workbench)
				.unlockedBy(HAS_UPGRADE_BASE_CRITERION_NAME, has(ModItems.UPGRADE_BASE.get())).save(compatRecipeOutput);

		RecipeOutput sbCompatRecipeOutput = compatRecipeOutput.withConditions(new ModLoadedCondition(SophisticatedBackpacks.MOD_ID));
		RecipeOutput sbCompatRecipeWithoutAdvancements = new RecipeOutputWithoutAdvancements(sbCompatRecipeOutput);

		// storage from backpack upgrade
		ShapeBasedRecipeBuilder.shaped(items, upgrade).pattern("PUP").pattern(" P ").pattern("P P").define('P', ItemTags.PLANKS).define('U', backpackUpgrade)
				.unlockedBy("has_backpack_upgrade", has(backpackUpgrade))
				.save(sbCompatRecipeWithoutAdvancements, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID,
						"storage_" + getCompatItemPath(upgrade) + "_from_backpack_" + getCompatItemPath(backpackUpgrade))));

		// backpack from storage upgrade
		ShapeBasedRecipeBuilder.shaped(items, backpackUpgrade).pattern("TUT").pattern(" L ").pattern("T T").define('T', Tags.Items.STRINGS)
				.define('L', Tags.Items.LEATHERS).define('U', upgrade).unlockedBy("has_storage_upgrade", has(upgrade))
				.save(sbCompatRecipeWithoutAdvancements, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(SophisticatedStorage.MOD_ID,
						"backpack_" + getCompatItemPath(backpackUpgrade) + "_from_storage_" + getCompatItemPath(upgrade))));
	}

	private static String getCompatItemPath(Item upgrade) {
		return RegistryHelper.getItemKey(upgrade).getPath().replace('/', '_');
	}

	private record RecipeOutputWithoutAdvancements(RecipeOutput delegate) implements RecipeOutput {
		@Override
		public Advancement.Builder advancement() {
			return delegate.advancement();
		}

		@Override
		public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
			delegate.accept(id, recipe, null, conditions);
		}

		@Override
		public void includeRootAdvancement() {
			delegate.includeRootAdvancement();
		}
	}

	private void addChestRecipes(RecipeOutput recipeOutput) {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach((woodType, blockFamily) -> woodChestRecipe(recipeOutput, woodType, blockFamily.getBaseBlock()));

		ShapeBasedRecipeBuilder
				.shaped(items, ModBlocks.CHEST_ITEM.get(), woodStorageTemplate(ModBlocks.CHEST_ITEM.get(), WoodType.OAK), GenericWoodStorageRecipe::new)
				.pattern("PPP").pattern("PLP").pattern("PPP").define('P', ItemTags.PLANKS).define('L', Blocks.LEVER)
				.unlockedBy("has " + PLANK_SUFFIX, has(ItemTags.PLANKS))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("generic_chest")));

		ShapelessBasedRecipeBuilder.shapeless(items, ModBlocks.CHEST_ITEM.get(), woodStorageTemplate(ModBlocks.CHEST_ITEM.get(), WoodType.OAK))
				.requires(Blocks.CHEST).requires(Blocks.LEVER).unlockedBy("has_vanilla_chest", has(Blocks.CHEST))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("oak_chest_from_vanilla_chest")));

		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.CHEST_ITEM.get(), ModBlocks.COPPER_CHEST_ITEM.get(), ModBlocks.IRON_CHEST_ITEM.get(),
				ModBlocks.GOLD_CHEST_ITEM.get(), ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.NETHERITE_CHEST_ITEM.get());
		addDoubleChestTierUpgradeRecipes(recipeOutput, ModBlocks.CHEST_ITEM.get(), ModBlocks.COPPER_CHEST_ITEM.get(), ModBlocks.IRON_CHEST_ITEM.get(),
				ModBlocks.GOLD_CHEST_ITEM.get(), ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.NETHERITE_CHEST_ITEM.get());

		// addQuarkChestRecipes(recipeOutput); // TODO Re-enable when Quark is available for this MC version and datagen can keep the generated files.
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
		ShapelessBasedRecipeBuilder.shapeless(items, ModBlocks.CHEST_ITEM.get(), woodStorageTemplate(ModBlocks.CHEST_ITEM.get(), woodType)).requires(chestBlock)
				.requires(Blocks.LEVER)
				.save(recipeOutput.withConditions(new RegisteredCondition<>(ResourceKey.create(Registries.ITEM, Identifier.parse(chestRegistryName)))),
						ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier(woodType.name() + "_chest_from_quark_" + name)));
	}

	private Block getBlock(String registryName) {
		return BuiltInRegistries.BLOCK.get(Identifier.parse(registryName)).orElseThrow().value();
	}

	private void addBarrelRecipes(RecipeOutput recipeOutput) {
		WoodStorageBlockBase.CUSTOM_TEXTURE_WOOD_TYPES.forEach(
				(woodType, blockFamily) -> woodBarrelRecipe(recipeOutput, woodType, blockFamily.getBaseBlock(), blockFamily.get(BlockFamily.Variant.SLAB)));

		ShapeBasedRecipeBuilder
				.shaped(items, ModBlocks.BARREL_ITEM.get(), woodStorageTemplate(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE), GenericWoodStorageRecipe::new)
				.pattern("PSP").pattern("PLP").pattern("PSP").define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('L', Blocks.LEVER)
				.unlockedBy("has " + PLANK_SUFFIX, has(ItemTags.PLANKS))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("generic_barrel")));

		ShapelessBasedRecipeBuilder.shapeless(items, ModBlocks.BARREL_ITEM.get(), woodStorageTemplate(ModBlocks.BARREL_ITEM.get(), WoodType.SPRUCE))
				.requires(Blocks.BARREL).requires(Blocks.LEVER).unlockedBy("has_vanilla_barrel", has(Blocks.BARREL))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier("spruce_barrel_from_vanilla_barrel")));

		addStorageTierUpgradeRecipes(recipeOutput, ModBlocks.BARREL_ITEM.get(), ModBlocks.COPPER_BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(),
				ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get());
	}

	private void woodBarrelRecipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.BARREL_ITEM.get(), woodStorageTemplate(ModBlocks.BARREL_ITEM.get(), woodType)).pattern("PSP")
				.pattern("PLP").pattern("PSP").define('P', planks).define('S', slab).define('L', Blocks.LEVER)
				.unlockedBy("has_" + woodType.name() + PLANK_SUFFIX, has(planks))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier(woodType.name() + "_barrel")));
	}

	private void limitedWoodBarrelRecipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab, Consumer<ShapeBasedRecipeBuilder> addPattern,
			BlockItem item) {
		ShapeBasedRecipeBuilder builder = ShapeBasedRecipeBuilder.shaped(items, item, woodStorageTemplate(item, woodType)).define('P', planks).define('S', slab)
				.define('L', Blocks.LEVER).unlockedBy("has_" + woodType.name() + PLANK_SUFFIX, has(planks));
		addPattern.accept(builder);
		builder.save(recipeOutput,
				ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier(woodType.name() + "_" + RegistryHelper.getItemKey(item).getPath())));
	}

	private void limitedWoodBarrel1Recipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		limitedWoodBarrelRecipe(recipeOutput, woodType, planks, slab, builder -> builder.pattern("PSP").pattern("PLP").pattern("PPP"),
				ModBlocks.LIMITED_BARREL_1_ITEM.get());
	}

	private void limitedWoodBarrel2Recipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		limitedWoodBarrelRecipe(recipeOutput, woodType, planks, slab, builder -> builder.pattern("PPP").pattern("SLS").pattern("PPP"),
				ModBlocks.LIMITED_BARREL_2_ITEM.get());
	}

	private void limitedWoodBarrel3Recipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		limitedWoodBarrelRecipe(recipeOutput, woodType, planks, slab, builder -> builder.pattern("PSP").pattern("PLP").pattern("SPS"),
				ModBlocks.LIMITED_BARREL_3_ITEM.get());
	}

	private void limitedWoodBarrel4Recipe(RecipeOutput recipeOutput, WoodType woodType, Block planks, Block slab) {
		limitedWoodBarrelRecipe(recipeOutput, woodType, planks, slab, builder -> builder.pattern("SPS").pattern("PLP").pattern("SPS"),
				ModBlocks.LIMITED_BARREL_4_ITEM.get());
	}

	private void woodChestRecipe(RecipeOutput recipeOutput, WoodType woodType, Block planks) {
		ShapeBasedRecipeBuilder.shaped(items, ModBlocks.CHEST_ITEM.get(), woodStorageTemplate(ModBlocks.CHEST_ITEM.get(), woodType)).pattern("PPP")
				.pattern("PLP").pattern("PPP").define('P', planks).define('L', Blocks.LEVER).unlockedBy("has_" + woodType.name() + PLANK_SUFFIX, has(planks))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier(woodType.name() + "_chest")));
	}

	private static ItemStackTemplate woodStorageTemplate(BlockItem item, WoodType woodType) {
		return new ItemStackTemplate(item, DataComponentPatch.builder().set(ModDataComponents.WOOD_TYPE.get(), woodType).build());
	}

	private static ItemStackTemplate tintedShulkerBoxTemplate(DyeColor color) {
		DataComponentPatch patch = DataComponentPatch.builder().set(ModCoreDataComponents.MAIN_COLOR.get(), color.getTextureDiffuseColor())
				.set(ModCoreDataComponents.ACCENT_COLOR.get(), color.getTextureDiffuseColor()).build();
		return new ItemStackTemplate(ModBlocks.SHULKER_BOX_ITEM.get(), patch);
	}

	private void tintedShulkerBoxRecipe(RecipeOutput recipeOutput, Block vanillaShulkerBox, DyeColor dyeColor) {
		String vanillaShulkerBoxName = BuiltInRegistries.BLOCK.getKey(vanillaShulkerBox).getPath();
		ShapelessBasedRecipeBuilder
				.shapeless(items, ModBlocks.SHULKER_BOX_ITEM.get(), tintedShulkerBoxTemplate(dyeColor), ShulkerBoxFromVanillaShapelessRecipe::new)
				.requires(vanillaShulkerBox).requires(Items.LEVER).unlockedBy("has_" + vanillaShulkerBoxName, has(vanillaShulkerBox))
				.save(recipeOutput, ResourceKey.create(Registries.RECIPE, SophisticatedStorage.getIdentifier(vanillaShulkerBoxName + "_to_sophisticated")));
	}

	public static class Runner extends RecipeProvider.Runner {

		protected Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
			super(packOutput, registries);
		}

		@Override
		protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
			return new StorageRecipeProvider(provider, recipeOutput);
		}

		@Override
		public String getName() {
			return "Sophisticated Storage Recipes";
		}
	}
}
