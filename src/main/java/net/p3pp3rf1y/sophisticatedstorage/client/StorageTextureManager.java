package net.p3pp3rf1y.sophisticatedstorage.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("java:S6548")
public class StorageTextureManager extends SimpleJsonResourceReloadListener<JsonElement> {
	public static final StorageTextureManager INSTANCE = new StorageTextureManager();
	private static final String PARENT_TAG = "parent";
	private static final String TYPE_TAG = "type";
	private static final String TEXTURES_TAG = "textures";
	private static final WoodType DEFAULT_CHEST_WOOD_TYPE = WoodType.ACACIA;
	private static final String CHEST_SUFFIX = "_chest";
	private static final Map<String, Supplier<ITextureParser>> TEXTURE_PARSERS = new HashMap<>();

	static {
		TEXTURE_PARSERS.put("chest", ChestTextureParser::new);
	}

	private final Map<WoodType, Map<ChestType, Map<ChestMaterial, SpriteId>>> woodChestMaterials = new HashMap<>();

	private StorageTextureManager() {
		super(ExtraCodecs.JSON, FileToIdConverter.json("storage_texture_definitions"));
	}

	@Override
	protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		clear();
		Map<Identifier, JsonElement> fileContents = super.prepare(resourceManager, profiler);
		Map<Identifier, StorageTextureDefinition> storageTextureDefinitions = new HashMap<>();
		fileContents.forEach((identifier, json) -> loadDefinition(storageTextureDefinitions, identifier, json, fileContents));

		storageTextureDefinitions.forEach((fileName, definition) -> {
			String type = definition.getType();
			String filePath = fileName.getPath();
			if (type.equals("chest") && filePath.endsWith(CHEST_SUFFIX)) {
				WoodType.values().filter(wt -> wt.name().equals(filePath.substring(0, filePath.lastIndexOf(CHEST_SUFFIX)))).findFirst().ifPresent(wt -> {
					Map<ChestType, Map<ChestMaterial, SpriteId>> chestMaterials = new EnumMap<>(ChestType.class);
					definition.getTextures()
							.forEach((chestTypeName, textures) -> textures.forEach((textureName, rl) -> ChestMaterial.fromString(textureName)
									.ifPresent(cm -> chestMaterials
											.computeIfAbsent(ChestType.valueOf(chestTypeName.toUpperCase(Locale.ROOT)), t -> new EnumMap<>(ChestMaterial.class))
											.put(cm, new SpriteId(Sheets.CHEST_SHEET, rl)))));
					woodChestMaterials.put(wt, chestMaterials);
				});
			}
		});
		return fileContents;
	}

	private void clear() {
		woodChestMaterials.clear();
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
		// noop as everything is done in prepare due to the need to have it done before texture usage
	}

	@Nullable
	public Map<ChestMaterial, SpriteId> getWoodChestMaterials(ChestType chestType, WoodType woodType) {
		Map<ChestType, Map<ChestMaterial, SpriteId>> chestTypeMaterials = woodChestMaterials.getOrDefault(woodType,
				woodChestMaterials.get(DEFAULT_CHEST_WOOD_TYPE));
		return chestTypeMaterials == null ? null : chestTypeMaterials.get(chestType);
	}

	private Optional<StorageTextureDefinition> loadDefinition(Map<Identifier, StorageTextureDefinition> storageTextureDefinitions, Identifier identifier,
			JsonElement json, Map<Identifier, JsonElement> fileContents) {
		if (storageTextureDefinitions.containsKey(identifier)) {
			return Optional.of(storageTextureDefinitions.get(identifier));
		}

		if (json == null || !json.isJsonObject()) {
			return Optional.empty();
		}

		JsonObject jsonContents = json.getAsJsonObject();
		String type = "";
		StorageTextureDefinition parentDefinition = null;

		if (jsonContents.has(PARENT_TAG) && jsonContents.get(PARENT_TAG).isJsonPrimitive()) {
			Identifier parent = Identifier.parse(jsonContents.get(PARENT_TAG).getAsString());
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
		result.ifPresent(def -> storageTextureDefinitions.put(identifier, def));
		return result;
	}

	public static class StorageTextureDefinition {
		private static final String ALL_SIDES_TEXTURES = "allSides";
		private final String type;
		private final Map<String, Map<String, Map<String, Identifier>>> textures;

		@SuppressWarnings({"unused", "java:S1172"})
		public StorageTextureDefinition(String type, Map<String, Map<String, Map<String, Identifier>>> multiplePartTextures,
				boolean multipleTextureIgnoredParameter) {
			this.type = type;
			textures = multiplePartTextures;
		}

		public StorageTextureDefinition(String type, Map<String, Map<String, Identifier>> textures) {
			this.type = type;
			this.textures = new HashMap<>();
			this.textures.put(ALL_SIDES_TEXTURES, textures);
		}

		public String getType() {
			return type;
		}

		public Map<String, Map<String, Identifier>> getTextures() {
			return textures.getOrDefault(ALL_SIDES_TEXTURES, new HashMap<>());
		}
	}

	private interface ITextureParser {
		void copyFromParentDefinition(StorageTextureDefinition parentDefinition);

		Optional<StorageTextureDefinition> parseDefinition(String type, JsonObject jsonContents);
	}

	private static class ChestTextureParser implements ITextureParser {
		private final Map<String, Map<String, Identifier>> textures = new HashMap<>();

		@Override
		public void copyFromParentDefinition(StorageTextureDefinition parentDefinition) {
			parentDefinition.getTextures().forEach((key, value) -> textures.put(key, new HashMap<>(value)));
		}

		@Override
		public Optional<StorageTextureDefinition> parseDefinition(String type, JsonObject jsonContents) {
			if (jsonContents.has(TEXTURES_TAG) && jsonContents.get(TEXTURES_TAG).isJsonObject()) {
				JsonObject jsonTextures = jsonContents.get(TEXTURES_TAG).getAsJsonObject();
				jsonTextures.keySet().forEach(name -> jsonTextures.get(name).getAsJsonObject().entrySet().forEach(entry -> {
					if (entry.getValue().isJsonPrimitive()) {
						textures.computeIfAbsent(name, k -> new HashMap<>()).put(entry.getKey(), Identifier.parse(entry.getValue().getAsString()));
					}
				}));
			}

			return Optional.of(new StorageTextureDefinition(type, textures));
		}
	}

	public enum ChestMaterial {
		BASE, WOOD_TIER, COPPER_TIER, IRON_TIER, GOLD_TIER, DIAMOND_TIER, NETHERITE_TIER, TINTABLE_MAIN, TINTABLE_ACCENT, PACKED;

		public static Optional<ChestMaterial> fromString(String materialName) {
			for (ChestMaterial value : values()) {
				if (value.name().toLowerCase(Locale.ROOT).equals(materialName)) {
					return Optional.of(value);
				}
			}
			return Optional.empty();
		}
	}
}
