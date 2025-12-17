package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.resources.Identifier;

import java.util.Map;

public interface IMaterialHolder {
	void setMaterials(Map<BarrelMaterial, Identifier> materials);

	Map<BarrelMaterial, Identifier> getMaterials();

	boolean canHoldMaterials();
}
