package net.p3pp3rf1y.sophisticatedstorage.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.client.render.RenderHelper;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GenericWoodStorageTintCache {
	private static final double MAX_MERGE_DISTANCE = 30.0;
	private static final Cache<WoodType, Optional<TintColors>> TINT_CACHE = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();

	private GenericWoodStorageTintCache() {
	}

	public static void clear() {
		TINT_CACHE.invalidateAll();
	}

	public static Optional<TintColors> getTintColors(WoodType woodType) {
		Optional<TintColors> tintColors = TINT_CACHE.getIfPresent(woodType);
		if (tintColors == null) {
			tintColors = calculateTintColors(woodType);
			TINT_CACHE.put(woodType, tintColors);
		}
		return tintColors;
	}

	public static int getMainColor(WoodType woodType) {
		return getTintColors(woodType).map(TintColors::mainColor).orElse(-1);
	}

	public static int getAccentColor(WoodType woodType) {
		return getMainColor(woodType);
	}

	private static Optional<TintColors> calculateTintColors(WoodType woodType) {
		return GenericWoodStorageHelper.getGenericWoodInfo(woodType).flatMap(info -> {
			try {
				TextureAtlasSprite sprite = RenderHelper.getSprite(info.planksLocation(), null, RandomSource.create());
				NativeImage image = sprite.contents().getOriginalImage();
				List<ColorGroup> colorGroups = getColorGroups(image, sprite.contents().width(), sprite.contents().height());
				if (colorGroups.isEmpty()) {
					return Optional.empty();
				}

				ColorGroup mainGroup = colorGroups.get(0);
				int mainColor = FastColor.ARGB32.color(255, FastColor.ARGB32.red(mainGroup.getRepresentativeColor()),
						FastColor.ARGB32.green(mainGroup.getRepresentativeColor()), FastColor.ARGB32.blue(mainGroup.getRepresentativeColor()));
				return Optional.of(new TintColors(mainColor));
			} catch (Exception e) {
				return Optional.empty();
			}
		});
	}

	private static List<ColorGroup> getColorGroups(NativeImage image, int width, int height) {
		Map<Integer, Integer> colorCounts = new HashMap<>();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = abgrToArgb(image.getPixelRGBA(x, y));
				if (FastColor.ARGB32.alpha(color) < 32 || touchesTransparentPixel(image, width, height, x, y)) {
					continue;
				}
				colorCounts.merge(color & 0xFFFFFF, 1, Integer::sum);
			}
		}

		List<ColorGroup> colorGroups = new ArrayList<>();
		for (Map.Entry<Integer, Integer> colorCount : colorCounts.entrySet()) {
			int color = colorCount.getKey();
			Optional<ColorGroup> matchingGroup = colorGroups.stream().filter(group -> group.distanceTo(color) <= MAX_MERGE_DISTANCE).findFirst();
			if (matchingGroup.isPresent()) {
				matchingGroup.get().add(color, colorCount.getValue());
			} else {
				colorGroups.add(new ColorGroup(color, colorCount.getValue()));
			}
		}
		colorGroups.sort(Comparator.comparingInt(ColorGroup::totalWeight).reversed());
		return colorGroups;
	}

	private static boolean touchesTransparentPixel(NativeImage image, int width, int height, int x, int y) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				if (dx == 0 && dy == 0) {
					continue;
				}
				int neighborX = x + dx;
				int neighborY = y + dy;
				if (neighborX < 0 || neighborY < 0 || neighborX >= width || neighborY >= height) {
					continue;
				}
				int color = abgrToArgb(image.getPixelRGBA(neighborX, neighborY));
				if (FastColor.ARGB32.alpha(color) < 32) {
					return true;
				}
			}
		}
		return false;
	}

	private static int abgrToArgb(int color) {
		return FastColor.ARGB32.color(FastColor.ABGR32.alpha(color), FastColor.ABGR32.red(color), FastColor.ABGR32.green(color), FastColor.ABGR32.blue(color));
	}

	public record TintColors(int mainColor) {
	}

	private static class ColorGroup {
		private int redSum = 0;
		private int greenSum = 0;
		private int blueSum = 0;
		private int totalWeight = 0;
		private final List<Integer> colors = new ArrayList<>();

		ColorGroup(int color, int weight) {
			add(color, weight);
		}

		void add(int color, int weight) {
			redSum += FastColor.ARGB32.red(color) * weight;
			greenSum += FastColor.ARGB32.green(color) * weight;
			blueSum += FastColor.ARGB32.blue(color) * weight;
			totalWeight += weight;
			colors.add(color);
		}

		int totalWeight() {
			return totalWeight;
		}

		int getRepresentativeColor() {
			int averageColor = getAverageColor();
			int bestColor = colors.get(0);
			double bestDistance = Double.MAX_VALUE;
			for (int color : colors) {
				double distance = colorDistance(averageColor, color);
				if (distance < bestDistance) {
					bestDistance = distance;
					bestColor = color;
				}
			}
			return bestColor;
		}

		double distanceTo(int color) {
			return colorDistance(getAverageColor(), color);
		}

		private int getAverageColor() {
			if (totalWeight == 0) {
				return 0;
			}
			return FastColor.ARGB32.color(255, redSum / totalWeight, greenSum / totalWeight, blueSum / totalWeight);
		}

		private static double colorDistance(int firstColor, int secondColor) {
			int dr = FastColor.ARGB32.red(firstColor) - FastColor.ARGB32.red(secondColor);
			int dg = FastColor.ARGB32.green(firstColor) - FastColor.ARGB32.green(secondColor);
			int db = FastColor.ARGB32.blue(firstColor) - FastColor.ARGB32.blue(secondColor);
			return Math.sqrt(dr * dr + dg * dg + db * db);
		}
	}
}
