package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

import java.util.Optional;

public interface ISimpleMaterialHolder {
	String MATERIAL_TAG = "material";
	String OVERLAY_HIDDEN_TAG = "overlayHidden";

	Optional<ResourceLocation> getMaterial();

	void setMaterial(@Nullable ResourceLocation material);

	boolean isOverlayHidden();

	void setOverlayHidden(boolean overlayHidden);

	default void saveSimpleMaterialData(CompoundTag tag) {
		getMaterial().ifPresent(material -> tag.putString(MATERIAL_TAG, material.toString()));
		if (isOverlayHidden()) {
			tag.putBoolean(OVERLAY_HIDDEN_TAG, true);
		}
	}

	default void loadSimpleMaterialData(CompoundTag tag) {
		setMaterial(tag.contains(MATERIAL_TAG) ? ResourceLocation.parse(tag.getString(MATERIAL_TAG)) : null);
		setOverlayHidden(tag.getBoolean(OVERLAY_HIDDEN_TAG));
	}
}
