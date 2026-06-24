package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompressionUpgradeConfig {
	private static final String DECOMPRESSIBLE_MATCHER = "([a-z0-9_.-]+:[a-z0-9_/.-]+)=(\\d+)x([a-z0-9_.-]+:[a-z0-9_/.-]+)";
	public final ModConfigSpec.IntValue maxNumberOfSlots;
	public final ModConfigSpec.ConfigValue<List<? extends String>> additionalDecompressibleItems;

	@Nullable
	private Map<Item, DecompressionResult> additionalDecompressibleItemsMap = null;

	public CompressionUpgradeConfig(ModConfigSpec.Builder builder) {
		builder.comment("Compression Upgrade Settings").push("compressionUpgrade");
		maxNumberOfSlots = builder.comment("Defines how many slots at a maximum compression upgrade is able to use").defineInRange("maxNumberOfSlots", 5, 3, 9);
		additionalDecompressibleItems = builder
				.comment("List of items that can be decompressed by compression upgrade and their results. "
						+ "Item registry names are expected here in format of \"mod:itemBeingDecompressed=Nxmod:itemDecompressResult\"")
				.defineList("additionalDecompressibleItems", CompressionUpgradeConfig::getDecompressibleItemsDefault,
						() -> "minecraft:glowstone=4xminecraft:glowstone_dust",
						itemName -> itemName instanceof String str && str.matches(DECOMPRESSIBLE_MATCHER));
		builder.pop();
	}

	@Nonnull
	private static List<String> getDecompressibleItemsDefault() {
		return List.of(getDecompressibleEntry(Items.GLOWSTONE, 4, Items.GLOWSTONE_DUST), getDecompressibleEntry(Items.QUARTZ_BLOCK, 4, Items.QUARTZ),
				getDecompressibleEntry(Items.CLAY, 4, Items.CLAY_BALL), getDecompressibleEntry(Items.SNOW_BLOCK, 4, Items.SNOWBALL),
				getDecompressibleEntry(Items.BRICKS, 4, Items.BRICK), getDecompressibleEntry(Items.NETHER_BRICKS, 4, Items.NETHER_BRICK),
				getDecompressibleEntry(Items.NETHER_WART_BLOCK, 9, Items.NETHER_WART), getDecompressibleEntry(Items.MELON, 9, Items.MELON_SLICE),
				getDecompressibleEntry(Items.PACKED_ICE, 9, Items.ICE), getDecompressibleEntry(Items.BLUE_ICE, 9, Items.PACKED_ICE));
	}

	private static String getDecompressibleEntry(Item fromItem, int count, Item toItem) {
		return BuiltInRegistries.ITEM.getKey(fromItem) + "=" + count + "x" + BuiltInRegistries.ITEM.getKey(toItem);
	}

	public Optional<DecompressionResult> getDecompressionResult(Item item) {
		if (additionalDecompressibleItemsMap == null) {
			additionalDecompressibleItemsMap = new HashMap<>();
			Pattern pattern = Pattern.compile(DECOMPRESSIBLE_MATCHER);
			additionalDecompressibleItems.get().forEach(decompressibleItem -> {
				Matcher matcher = pattern.matcher(decompressibleItem);
				if (matcher.find()) {
					Item fromItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(matcher.group(1)));
					int count = Integer.parseInt(matcher.group(2));
					Item toItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(matcher.group(3)));
					if (fromItem != Items.AIR && toItem != Items.AIR && count > 1) {
						additionalDecompressibleItemsMap.put(fromItem, new DecompressionResult(new ItemStack(toItem), count));
					}
				}
			});
		}
		return Optional.ofNullable(additionalDecompressibleItemsMap.get(item))
				.or(() -> Config.SERVER.compactingUpgrade.getUncompactingResult(new ItemStack(item), 3, 3)
						.map(uncompactingResult -> new DecompressionResult(uncompactingResult.result(), uncompactingResult.count())));
	}

	public Optional<CompressionResult> getCompressionResult(ItemStack stack) {
		return Config.SERVER.compactingUpgrade
				.getCompactingResult(stack, 3, 3, (result, count) -> getDecompressionResult(result.getItem())
						.filter(decompressionResult -> decompressionResult.matches(stack, count)).isPresent())
				.map(compactingResult -> new CompressionResult(compactingResult.result().getResult(), compactingResult.count()));
	}

	public void clearCache() {
		additionalDecompressibleItemsMap = null;
	}

	public record DecompressionResult(ItemStack result, int count) {
		public DecompressionResult {
			result = result.copyWithCount(1);
		}

		public boolean matches(ItemStack stack, int count) {
			return this.count == count && result.getItem() == stack.getItem();
		}
	}

	public record CompressionResult(ItemStack result, int count) {
		public CompressionResult {
			result = result.copyWithCount(1);
		}
	}
}
