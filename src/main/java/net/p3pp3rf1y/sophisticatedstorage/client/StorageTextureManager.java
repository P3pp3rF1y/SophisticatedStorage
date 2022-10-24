package net.p3pp3rf1y.sophisticatedstorage.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelBakedModelBase;
import net.p3pp3rf1y.sophisticatedstorage.client.render.BarrelDynamicModelBase;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageTextureManager extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	public static final StorageTextureManager INSTANCE = new StorageTextureManager();
	private static final String PARENT_TAG = "parent";
	private static final String TYPE_TAG = "type";
	private static final String TEXTURES_TAG = "textures";
	private static final WoodType defaultChestWoodType = WoodType.ACACIA;
	private static final String CHEST_SUFFIX = "_chest";
	private static final String BARREL_SUFFIX = "_barrel";
	private static final Pattern LIMITED_BARREL_PATTERN = Pattern.compile("limited_([a-z_]+)_barrel");
	private static final Map<String, Supplier<ITextureParser>> TEXTURE_PARSERS = new HashMap<>();

	static {
		TEXTURE_PARSERS.put("chest", ChestTextureParser::new);
		TEXTURE_PARSERS.put("barrel", BarrelTextureParser::new);
		TEXTURE_PARSERS.put("limited_barrel", BarrelTextureParser::new);
	}

	private final Map<WoodType, Map<ChestMaterial, Material>> woodChestMaterials = new HashMap<>();
	private final Map<WoodType, Map<BarrelFace, Map<BarrelMaterial, Material>>> barrelMaterials = new HashMap<>();
	private final Map<WoodType, Map<BarrelFace, Map<BarrelMaterial, Material>>> limitedBarrelMaterials = new HashMap<>();

	private StorageTextureManager() {
		super(GSON, "storage_texture_definitions");
	}

	@Override
	protected Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
		clear();
		BarrelBakedModelBase.BAKED_QUADS_CACHE.invalidateAll();
		BarrelDynamicModelBase.TEXTURES.clear();
		BarrelDynamicModelBase.BAKED_PART_MODELS.clear();

		Map<ResourceLocation, JsonElement> fileContents = super.prepare(pResourceManager, pProfiler);
		Map<ResourceLocation, StorageTextureDefinition> storageTextureDefinitions = new HashMap<>();
		fileContents.forEach((resourceLocation, json) -> loadDefinition(storageTextureDefinitions, resourceLocation, json, fileContents));

		storageTextureDefinitions.forEach((fileName, definition) -> {
			String type = definition.getType();
			String filePath = fileName.getPath();
			if (type.equals("chest")) {
				if (filePath.endsWith(CHEST_SUFFIX)) {
					WoodType.values().filter(wt -> wt.name().equals(filePath.substring(0, filePath.lastIndexOf(CHEST_SUFFIX)))).findFirst().ifPresent(wt -> {
						Map<ChestMaterial, Material> chestMaterials = new EnumMap<>(ChestMaterial.class);
						definition.getTextures().forEach((textureName, rl) ->
								ChestMaterial.fromString(textureName).ifPresent(cm -> chestMaterials.put(cm, new Material(Sheets.CHEST_SHEET, rl))));

						woodChestMaterials.put(wt, chestMaterials);
					});
				}
			} else if (type.equals("barrel") && filePath.endsWith(BARREL_SUFFIX)) {
				parseBarrelTextures(definition, filePath, barrelMaterials, fn -> fn.substring(0, fn.lastIndexOf(BARREL_SUFFIX)));
			} else if (type.equals("limited_barrel")) {
				Matcher matcher = LIMITED_BARREL_PATTERN.matcher(filePath);
				if (matcher.find()) {
					parseBarrelTextures(definition, filePath, limitedBarrelMaterials, fn -> matcher.group(1));
				}
			}
		});
		return fileContents;
	}

	private void parseBarrelTextures(StorageTextureDefinition definition, String filePath, Map<WoodType, Map<BarrelFace, Map<BarrelMaterial, Material>>> materialsMap, UnaryOperator<String> getWoodName) {
		WoodType.values().filter(wt -> wt.name().equals(getWoodName.apply(filePath))).findFirst()
				.ifPresent(wt -> definition.getTextureParts().forEach(part -> BarrelFace.fromString(part).ifPresent(barrelFace -> {
					Map<BarrelMaterial, Material> barrelFaceMaterials = new EnumMap<>(BarrelMaterial.class);

					definition.getTextures(part).forEach((textureName, rl) ->
							BarrelMaterial.fromString(textureName).ifPresent(cm -> barrelFaceMaterials.put(cm, new Material(InventoryMenu.BLOCK_ATLAS, rl))));

					materialsMap.computeIfAbsent(wt, k -> new EnumMap<>(BarrelFace.class)).put(barrelFace, barrelFaceMaterials);
				})));
	}

	private void clear() {
		woodChestMaterials.clear();
		barrelMaterials.clear();
		limitedBarrelMaterials.clear();
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
		//noop as everything is done in prepare due to the need to have it done before TextureStitchEvent fires
	}

	@Nullable //can return null when resources are reloading and this collection was cleared
	public Map<ChestMaterial, Material> getWoodChestMaterials(WoodType woodType) {
		return woodChestMaterials.getOrDefault(woodType, woodChestMaterials.get(defaultChestWoodType));
	}

	public Optional<Material> getBarrelMaterial(WoodType woodType, BarrelFace barrelFace, BarrelMaterial barrelMaterial) {
		return getMaterial(barrelMaterials, woodType, barrelFace, barrelMaterial);
	}

	private Optional<Material> getMaterial(Map<WoodType, Map<BarrelFace, Map<BarrelMaterial, Material>>> materials, WoodType woodType, BarrelFace barrelFace, BarrelMaterial barrelMaterial) {
		Map<BarrelFace, Map<BarrelMaterial, Material>> woodMaterials = materials.get(woodType);
		if (woodMaterials == null) {
			return Optional.empty();
		}
		Map<BarrelMaterial, Material> faceMaterials = woodMaterials.get(barrelFace);
		if (faceMaterials == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(faceMaterials.get(barrelMaterial));
	}

	public Optional<Material> getLimitedBarrelMaterial(WoodType woodType, BarrelFace barrelFace, BarrelMaterial barrelMaterial) {
		return getMaterial(limitedBarrelMaterials, woodType, barrelFace, barrelMaterial);
	}

	private Optional<StorageTextureDefinition> loadDefinition(Map<ResourceLocation, StorageTextureDefinition> storageTextureDefinitions, ResourceLocation resourceLocation, JsonElement json, Map<ResourceLocation, JsonElement> fileContents) {
		//already loaded probably because it is a parent to another definition
		if (storageTextureDefinitions.containsKey(resourceLocation)) {
			return Optional.of(storageTextureDefinitions.get(resourceLocation));
		}

		if (!json.isJsonObject()) {
			return Optional.empty();
		}

		JsonObject jsonContents = json.getAsJsonObject();

		String type = "";

		@Nullable
		StorageTextureDefinition parentDefinition = null;

		if (jsonContents.has(PARENT_TAG) && jsonContents.get(PARENT_TAG).isJsonPrimitive()) {
			ResourceLocation parent = new ResourceLocation(jsonContents.get(PARENT_TAG).getAsString());
			JsonElement parentJson = fileContents.get(parent);
			parentDefinition = loadDefinition(storageTextureDefinitions, parent, parentJson, fileContents).orElse(null);
			if (parentDefinition != null) {
				type = parentDefinition.getType();
			}
		}

		if (jsonContents.has(TYPE_TAG) && jsonContents.get(TYPE_TAG).isJsonPrimitive()) {
			type = jsonContents.get(TYPE_TAG).getAsString();
		}

		if (!TEXTURE_PARSERS.containsKey(type)) {
			return Optional.empty();
		}

		ITextureParser textureParser = TEXTURE_PARSERS.get(type).get();

		if (parentDefinition != null) {
			textureParser.copyFromParentDefinition(parentDefinition);
		}

		Optional<StorageTextureDefinition> result = textureParser.parseDefinition(type, jsonContents);
		result.ifPresent(def -> storageTextureDefinitions.put(resourceLocation, def));

		return result;
	}

	public Collection<Material> getUniqueChestMaterials() {
		Set<Material> uniqueMaterials = new HashSet<>();
		woodChestMaterials.values().forEach(chestMaterials -> uniqueMaterials.addAll(chestMaterials.values()));
		return uniqueMaterials;
	}

	public static class StorageTextureDefinition {
		private static final String ALL_SIDES_TEXTURES = "allSides";
		private final String type;
		private final Map<String, Map<String, ResourceLocation>> textures;

		@SuppressWarnings({"unused", "java:S1172"}) //ignoring unused parameter because it's needed due to two constructors with the same erasure
		public StorageTextureDefinition(String type, Map<String, Map<String, ResourceLocation>> multiplePartTextures, boolean multipleTextureIgnoredParameter) {
			this.type = type;
			textures = multiplePartTextures;
		}

		public StorageTextureDefinition(String type, Map<String, ResourceLocation> textures) {
			this.type = type;
			this.textures = new HashMap<>();
			this.textures.put(ALL_SIDES_TEXTURES, textures);
		}

		public String getType() {
			return type;
		}

		public Map<String, ResourceLocation> getTextures() {
			return textures.getOrDefault(ALL_SIDES_TEXTURES, new HashMap<>());
		}

		public Map<String, ResourceLocation> getTextures(String part) {
			return textures.getOrDefault(part, new HashMap<>());
		}

		public Set<String> getTextureParts() {
			return textures.keySet();
		}
	}

	private interface ITextureParser {
		void copyFromParentDefinition(StorageTextureDefinition parentDefinition);

		Optional<StorageTextureDefinition> parseDefinition(String type, JsonObject jsonContents);
	}

	private static class ChestTextureParser implements ITextureParser {
		private final Map<String, ResourceLocation> textures = new HashMap<>();

		@Override
		public void copyFromParentDefinition(StorageTextureDefinition parentDefinition) {
			textures.putAll(parentDefinition.getTextures());
		}

		@Override
		public Optional<StorageTextureDefinition> parseDefinition(String type, JsonObject jsonContents) {
			if (jsonContents.has(TEXTURES_TAG) && jsonContents.get(TEXTURES_TAG).isJsonObject()) {
				JsonObject jsonTextures = jsonContents.get(TEXTURES_TAG).getAsJsonObject();

				jsonTextures.keySet().forEach(name -> {
					if (jsonTextures.get(name).isJsonPrimitive()) {
						textures.put(name, new ResourceLocation(jsonTextures.get(name).getAsString()));
					}
				});
			}

			return Optional.of(new StorageTextureDefinition(type, textures));
		}
	}

	private static class BarrelTextureParser implements ITextureParser {
		private final Map<String, Map<String, ResourceLocation>> textures = new HashMap<>();

		@Override
		public void copyFromParentDefinition(StorageTextureDefinition parentDefinition) {
			textures.put("top", new HashMap<>(parentDefinition.getTextures("top")));
			textures.put("bottom", new HashMap<>(parentDefinition.getTextures("bottom")));
			textures.put("side", new HashMap<>(parentDefinition.getTextures("side")));
		}

		@Override
		public Optional<StorageTextureDefinition> parseDefinition(String type, JsonObject jsonContents) {
			if (jsonContents.has(TEXTURES_TAG) && jsonContents.get(TEXTURES_TAG).isJsonObject()) {
				JsonObject jsonTextures = jsonContents.get(TEXTURES_TAG).getAsJsonObject();

				jsonTextures.keySet().forEach(partName -> {
					if (jsonTextures.get(partName).isJsonObject()) {
						JsonObject part = jsonTextures.get(partName).getAsJsonObject();
						Map<String, ResourceLocation> partTextures = textures.computeIfAbsent(partName, pn -> new HashMap<>());
						part.keySet().forEach(textureName -> partTextures.put(textureName, new ResourceLocation(part.get(textureName).getAsString())));
					}
				});
			}

			return Optional.of(new StorageTextureDefinition(type, textures, true));
		}
	}

	public enum ChestMaterial {
		BASE,
		WOOD_TIER,
		IRON_TIER,
		GOLD_TIER,
		DIAMOND_TIER,
		NETHERITE_TIER,
		TINTABLE_MAIN,
		TINTABLE_ACCENT,
		PACKED;

		public static Optional<ChestMaterial> fromString(String materialName) {
			for (ChestMaterial value : values()) {
				if (value.name().toLowerCase(Locale.ROOT).equals(materialName)) {
					return Optional.of(value);
				}
			}
			return Optional.empty();
		}
	}

	public enum BarrelMaterial {
		BASE,
		BASE_1,
		BASE_2,
		BASE_3,
		BASE_4,
		BASE_OPEN,
		HANDLE,
		METAL_BANDS,
		WOOD_TIER,
		IRON_TIER,
		GOLD_TIER,
		DIAMOND_TIER,
		NETHERITE_TIER,
		TINTABLE_MAIN,
		TINTABLE_MAIN_1,
		TINTABLE_MAIN_2,
		TINTABLE_MAIN_3,
		TINTABLE_MAIN_4,
		TINTABLE_MAIN_OPEN,
		TINTABLE_ACCENT,
		TINTABLE_ACCENT_1,
		TINTABLE_ACCENT_2,
		TINTABLE_ACCENT_3,
		TINTABLE_ACCENT_4,
		PACKED;

		public static Optional<BarrelMaterial> fromString(String textureName) {
			for (BarrelMaterial value : values()) {
				if (value.name().toLowerCase(Locale.ROOT).equals(textureName)) {
					return Optional.of(value);
				}
			}
			return Optional.empty();
		}
	}

	public enum BarrelFace {
		TOP,
		BOTTOM,
		SIDE;

		public static Optional<BarrelFace> fromString(String faceName) {
			for (BarrelFace value : values()) {
				if (value.name().toLowerCase(Locale.ROOT).equals(faceName)) {
					return Optional.of(value);
				}
			}
			return Optional.empty();
		}
	}
}
