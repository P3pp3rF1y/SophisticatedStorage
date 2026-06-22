package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;

public interface ISimpleMaterialHolder {
	String MATERIAL_TAG = "material";
	String OVERLAY_HIDDEN_TAG = "overlayHidden";

	Optional<Identifier> getMaterial();

	void setMaterial(@Nullable Identifier material);

	boolean isOverlayHidden();

	void setOverlayHidden(boolean overlayHidden);

	default void saveSimpleMaterialData(ValueOutput out) {
		getMaterial().ifPresent(material -> out.putString(MATERIAL_TAG, material.toString()));
		if (isOverlayHidden()) {
			out.putBoolean(OVERLAY_HIDDEN_TAG, true);
		}
	}

	default void loadSimpleMaterialData(ValueInput in) {
		setMaterial(in.getString(MATERIAL_TAG).map(Identifier::parse).orElse(null));
		setOverlayHidden(in.getBooleanOr(OVERLAY_HIDDEN_TAG, false));
	}
}
