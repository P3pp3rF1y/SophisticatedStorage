package net.p3pp3rf1y.sophisticatedstorage.compat.rubidium;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class RubidiumCompat implements ICompat {
	@Override
	public void setup() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> RubidiumTranslucentVertexConsumer::register);
	}
}
