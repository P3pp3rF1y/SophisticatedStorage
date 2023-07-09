package net.p3pp3rf1y.sophisticatedstorage.compat.jade;

import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@SuppressWarnings("unused") //used by Jade's reflection
@WailaPlugin
public class StorageJadePlugin implements IWailaPlugin {
	@Override
	public void registerClient(IWailaClientRegistration registration) {
		ModBlocks.BLOCKS.getEntries().forEach(registeredBlock -> {
			Block block = registeredBlock.get();
			if (block instanceof WoodStorageBlockBase) {
				registration.usePickedResult(block);
			}
		});
	}
}
