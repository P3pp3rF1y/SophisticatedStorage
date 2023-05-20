package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BarrelMaterialRecipe extends CustomRecipe {
	public BarrelMaterialRecipe(ResourceLocation registryName) {
		super(registryName);
	}

	@Override
	public boolean matches(CraftingContainer container, Level level) {
		int barrelRow = -1;
		int barrelCol = -1;
		int minRowWithBlock = Integer.MAX_VALUE;
		int minColWithBlock = Integer.MAX_VALUE;
		int maxRowWithBlock = Integer.MIN_VALUE;
		int maxColWithBlock = Integer.MIN_VALUE;

		Map<Integer, Integer> rowCounts = new HashMap<>();

		for (int row = 0; row < container.getHeight(); row++) {
			for (int col = 0; col < container.getWidth(); col++) {
				ItemStack item = container.getItem(col + row * container.getWidth());
				if (item.isEmpty()) {
					continue;
				}

				if (item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BarrelBlock) {
					if (barrelRow < 0) {
						barrelRow = row;
						barrelCol = col;
					} else {
						return false;
					}
				} else if (isFullBlockItem(level, item.getItem())) {
					boolean isBottomMiddleAndBottomLeftHasBlock = barrelCol == col && barrelRow < row && rowCounts.getOrDefault(row, 0) > 0;
					if (isBottomMiddleAndBottomLeftHasBlock) {
						return false;
					}

					rowCounts.compute(row, (k, v) -> v == null ? 1 : v + 1);
					if (row < minRowWithBlock) {
						minRowWithBlock = row;
					}
					if (col < minColWithBlock) {
						minColWithBlock = col;
					}
					if (row > maxRowWithBlock) {
						maxRowWithBlock = row;
					}
					if (col > maxColWithBlock) {
						maxColWithBlock = col;
					}
				} else {
					return false;
				}
			}
		}

		if (barrelRow < 0 || rowCounts.isEmpty() || minRowWithBlock < barrelRow - 1 || maxRowWithBlock > barrelRow + 1 || minColWithBlock < barrelCol - 1 || maxColWithBlock > barrelCol + 1) {
			return false;
		}

		return rowCounts.getOrDefault(barrelRow - 1, 0) <= 3 && rowCounts.getOrDefault(barrelRow, 0) <= 2 && rowCounts.getOrDefault(barrelRow + 1, 0) <= 2;
	}

	@SuppressWarnings({"java:S1874", "deprecation"})
	private boolean isFullBlockItem(Level level, Item item) {
		if (!(item instanceof BlockItem blockItem)) {
			return false;
		}

		return Block.isShapeFullBlock(blockItem.getBlock().getShape(blockItem.getBlock().defaultBlockState(), level, BlockPos.ZERO, CollisionContext.empty()));
	}

	@Override
	public ItemStack assemble(CraftingContainer container) {
		int barrelColumn = -1;
		int barrelRow = -1;
		ItemStack barrelStackCopy = ItemStack.EMPTY;

		for (int row = 0; row < container.getHeight(); row++) {
			for (int col = 0; col < container.getWidth(); col++) {
				ItemStack item = container.getItem(col + row * container.getWidth());
				if (item.isEmpty()) {
					continue;
				}

				if (barrelColumn < 0 && item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BarrelBlock) {
					barrelColumn = col;
					barrelRow = row;
					barrelStackCopy = item.copy();
					barrelStackCopy.setCount(1);
				}
			}
		}

		Map<BarrelMaterial, ResourceLocation> materials = new EnumMap<>(BarrelMaterial.class);
		materials.putAll(BarrelBlockItem.getMaterials(barrelStackCopy));
		uncompactMaterials(materials);

		fillGridMaterials(container, barrelColumn, barrelRow, materials);
		fillEmptyMaterialsWithDefaults(materials);
		compactMaterials(materials);

		BarrelBlockItem.setMaterials(barrelStackCopy, materials);

		removeCoveredTints(barrelStackCopy, materials);

		return barrelStackCopy;
	}

	private void uncompactMaterials(Map<BarrelMaterial, ResourceLocation> materials) {
		if (materials.isEmpty()) {
			return;
		}

		Map<BarrelMaterial, ResourceLocation> uncompactedMaterials = new EnumMap<>(BarrelMaterial.class);
		materials.forEach((mat, texture) -> {
			for (BarrelMaterial child : mat.getChildren()) {
				uncompactedMaterials.put(child, texture);
			}
		});

		materials.clear();
		materials.putAll(uncompactedMaterials);
	}

	private static void removeCoveredTints(ItemStack barrelStackCopy, Map<BarrelMaterial, ResourceLocation> materials) {
		if (barrelStackCopy.getItem() instanceof ITintableBlockItem tintableBlockItem) {
			boolean hasMainTint = tintableBlockItem.getMainColor(barrelStackCopy).isPresent();
			boolean hasAccentTint = tintableBlockItem.getAccentColor(barrelStackCopy).isPresent();

			if (hasMainTint || hasAccentTint) {
				Set<BarrelMaterial.MaterialModelPart> materialModelParts = materials.keySet().stream().map(BarrelMaterial::getMaterialModelPart).collect(Collectors.toSet());

				if (hasMainTint && (materialModelParts.contains(BarrelMaterial.MaterialModelPart.BOTH) || materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE))) {
					tintableBlockItem.removeMainColor(barrelStackCopy);
				} else if (hasAccentTint && (materialModelParts.contains(BarrelMaterial.MaterialModelPart.BOTH) || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM))) {
					tintableBlockItem.removeAccentColor(barrelStackCopy);
				}
			}
		}
	}

	private static void compactMaterials(Map<BarrelMaterial, ResourceLocation> materials) {
		for (BarrelMaterial material : BarrelMaterial.values()) {
			if (!material.isLeaf()) {
				//if all children have the same texture remove them and convert to the parent
				ResourceLocation firstChildTexture = null;
				boolean allChildrenHaveSameTexture = true;
				for (BarrelMaterial child : material.getChildren()) {
					ResourceLocation texture = materials.get(child);
					if (texture == null || (firstChildTexture != null && !firstChildTexture.equals(texture))) {
						allChildrenHaveSameTexture = false;
						break;
					} else if (firstChildTexture == null) {
						firstChildTexture = texture;
					}
				}

				if (firstChildTexture != null && allChildrenHaveSameTexture) {
					materials.put(material, firstChildTexture);
					for (BarrelMaterial child : material.getChildren()) {
						materials.remove(child);
					}
				}
			}
		}
	}

	private static void fillEmptyMaterialsWithDefaults(Map<BarrelMaterial, ResourceLocation> materials) {
		for (BarrelMaterial material : BarrelMaterial.values()) {
			if (material.isLeaf() && !materials.containsKey(material)) {
				for (BarrelMaterial fillFromDefault : BarrelMaterial.getFillFromDefaults(material)) {
					if (materials.containsKey(fillFromDefault)) {
						materials.put(material, materials.get(fillFromDefault));
						break;
					}
				}
			}
		}
	}

	private void fillGridMaterials(CraftingContainer container, int barrelColumn, int barrelRow, Map<BarrelMaterial, ResourceLocation> materials) {
		for (int row = 0; row < container.getHeight(); row++) {
			for (int col = 0; col < container.getWidth(); col++) {
				ItemStack item = container.getItem(col + row * container.getWidth());
				if ((row == barrelRow && col == barrelColumn) || item.isEmpty()) {
					continue;
				}

				if (item.getItem() instanceof BlockItem blockItem) {
					List<BarrelMaterial> barrelMaterials = getBarrelMaterials(row, col, barrelRow, barrelColumn);
					boolean firstMaterial = true;
					for (BarrelMaterial barrelMaterial : barrelMaterials) {
						if (!materials.containsKey(barrelMaterial) || firstMaterial) {
							materials.put(barrelMaterial, ForgeRegistries.BLOCKS.getKey(blockItem.getBlock()));
						}
						firstMaterial = false;
					}
				}
			}
		}
	}

	private List<BarrelMaterial> getBarrelMaterials(int row, int col, int barrelRow, int barrelColumn) {
		if (row < barrelRow) {
			if (col < barrelColumn) {
				return List.of(BarrelMaterial.TOP);
			} else if (col == barrelColumn) {
				return List.of(BarrelMaterial.TOP_INNER_TRIM, BarrelMaterial.TOP, BarrelMaterial.TOP_TRIM);
			} else {
				return List.of(BarrelMaterial.TOP_TRIM);
			}
		} else if (row == barrelRow) {
			if (col < barrelColumn) {
				return List.of(BarrelMaterial.SIDE);
			} else {
				return List.of(BarrelMaterial.SIDE_TRIM);
			}
		} else {
			if (col < barrelColumn) {
				return List.of(BarrelMaterial.BOTTOM);
			} else if (col == barrelColumn) {
				return List.of(BarrelMaterial.BOTTOM, BarrelMaterial.BOTTOM_TRIM);
			} else {
				return List.of(BarrelMaterial.BOTTOM_TRIM);
			}
		}
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height > 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModBlocks.BARREL_MATERIAL_RECIPE_SERIALIZER.get();
	}
}
