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
import net.minecraft.world.level.block.state.properties.WoodType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StorageTextureManager extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public static final StorageTextureManager INSTANCE = new StorageTextureManager();
	private static final String PARENT_TAG = "parent";
	private static final String TYPE_TAG = "type";
	private static final String TEXTURES_TAG = "textures";

	private static final WoodType defaultChestWoodType = WoodType.ACACIA;
	private static final String CHEST_SUFFIX = "_chest";

	private final Map<WoodType, Map<ChestMaterial, Material>> woodChestMaterials = new HashMap<>();

	private StorageTextureManager() {
		super(GSON, "storage_texture_definitions");
	}

	@Override
	protected Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
		Map<ResourceLocation, JsonElement> fileContents = super.prepare(pResourceManager, pProfiler);
		Map<ResourceLocation, StorageTextureDefinition> storageTextureDefinitions = new HashMap<>();
		fileContents.forEach((resourceLocation, json) -> loadDefinition(storageTextureDefinitions, resourceLocation, json, fileContents));

		storageTextureDefinitions.forEach((fileName, definition) -> {
			String filePath = fileName.getPath();
			if (filePath.endsWith(CHEST_SUFFIX)) {
				WoodType.values().filter(wt -> wt.name().equals(filePath.substring(0, filePath.lastIndexOf(CHEST_SUFFIX)))).findFirst().ifPresent(wt -> {
					Map<ChestMaterial, Material> chestMaterials = new EnumMap<>(ChestMaterial.class);
					definition.getTextures().forEach((textureName, rl) ->
							ChestMaterial.fromString(textureName).ifPresent(cm -> chestMaterials.put(cm, new Material(Sheets.CHEST_SHEET, rl))));

					woodChestMaterials.put(wt, chestMaterials);
				});
			}
		});
		return fileContents;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
		//noop as everything is done in prepare due to the need to have it done before TextureStitchEvent fires
	}

	public Map<ChestMaterial, Material> getWoodChestMaterials(WoodType woodType) {
		return woodChestMaterials.getOrDefault(woodType, woodChestMaterials.get(defaultChestWoodType));
	}

	@Nullable
	private StorageTextureDefinition loadDefinition(Map<ResourceLocation, StorageTextureDefinition> storageTextureDefinitions, ResourceLocation resourceLocation, JsonElement json, Map<ResourceLocation, JsonElement> fileContents) {
		//already loaded probably because it is a parent to another definition
		if (storageTextureDefinitions.containsKey(resourceLocation)) {
			return storageTextureDefinitions.get(resourceLocation);
		}

		if (!json.isJsonObject()) {
			return null;
		}

		JsonObject jsonContents = json.getAsJsonObject();

		String type = "";
		Map<String, ResourceLocation> textures = new HashMap<>();

		if (jsonContents.has(PARENT_TAG) && jsonContents.get(PARENT_TAG).isJsonPrimitive()) {
			ResourceLocation parent = new ResourceLocation(jsonContents.get(PARENT_TAG).getAsString());
			JsonElement parentJson = fileContents.get(parent);
			StorageTextureDefinition parentDefinition = loadDefinition(storageTextureDefinitions, parent, parentJson, fileContents);
			if (parentDefinition != null) {
				type = parentDefinition.getType();
				textures.putAll(parentDefinition.getTextures());
			}
		}

		if (jsonContents.has(TYPE_TAG) && jsonContents.get(TYPE_TAG).isJsonPrimitive()) {
			type = jsonContents.get(TYPE_TAG).getAsString();
		}
		if (jsonContents.has(TEXTURES_TAG) && jsonContents.get(TEXTURES_TAG).isJsonObject()) {
			JsonObject jsonTextures = jsonContents.get(TEXTURES_TAG).getAsJsonObject();

			jsonTextures.keySet().forEach(name -> {
				if (jsonTextures.get(name).isJsonPrimitive()) {
					textures.put(name, new ResourceLocation(jsonTextures.get(name).getAsString()));
				}
			});
		}

		StorageTextureDefinition definition = new StorageTextureDefinition(type, textures);
		storageTextureDefinitions.put(resourceLocation, definition);
		return definition;
	}

	public Collection<Material> getUniqueChestMaterials() {
		Set<Material> uniqueMaterials = new HashSet<>();
		woodChestMaterials.values().forEach(chestMaterials -> uniqueMaterials.addAll(chestMaterials.values()));
		return uniqueMaterials;
	}

	public static class StorageTextureDefinition {
		private final String type;
		private final Map<String, ResourceLocation> textures;

		public StorageTextureDefinition(String type, Map<String, ResourceLocation> textures) {
			this.type = type;
			this.textures = textures;
		}

		public String getType() {
			return type;
		}

		public Map<String, ResourceLocation> getTextures() {
			return textures;
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
}
