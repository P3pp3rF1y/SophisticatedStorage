package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import net.p3pp3rf1y.sophisticatedstorage.client.particle.CustomTintTerrainParticle;

import java.util.Random;

class BarrelBlockClientExtensions implements IClientBlockExtensions {
	private final BarrelBlock barrelBlock;
	private final Random random = new Random();

	public BarrelBlockClientExtensions(BarrelBlock barrelBlock) {this.barrelBlock = barrelBlock;}

	@Override
	public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
		if (state.getBlock() != barrelBlock || !(level instanceof ClientLevel clientLevel) || !(target instanceof BlockHitResult blockHitResult)) {
			return false;
		}
		Direction sideHit = blockHitResult.getDirection();
		BlockPos pos = blockHitResult.getBlockPos();
		if (state.getRenderShape() != RenderShape.INVISIBLE) {
			int i = pos.getX();
			int j = pos.getY();
			int k = pos.getZ();
			AABB aabb = state.getShape(level, pos).bounds();
			double d0 = i + random.nextDouble() * (aabb.maxX - aabb.minX - 0.2F) + 0.1F + aabb.minX;
			double d1 = j + random.nextDouble() * (aabb.maxY - aabb.minY - 0.2F) + 0.1F + aabb.minY;
			double d2 = k + random.nextDouble() * (aabb.maxZ - aabb.minZ - 0.2F) + 0.1F + aabb.minZ;
			if (sideHit == Direction.DOWN) {
				d1 = j + aabb.minY - 0.1F;
			}

			if (sideHit == Direction.UP) {
				d1 = j + aabb.maxY + 0.1F;
			}

			if (sideHit == Direction.NORTH) {
				d2 = k + aabb.minZ - 0.1F;
			}

			if (sideHit == Direction.SOUTH) {
				d2 = k + aabb.maxZ + 0.1F;
			}

			if (sideHit == Direction.WEST) {
				d0 = i + aabb.minX - 0.1F;
			}

			if (sideHit == Direction.EAST) {
				d0 = i + aabb.maxX + 0.1F;
			}

			manager.add((new CustomTintTerrainParticle(clientLevel, d0, d1, d2, 0.0D, 0.0D, 0.0D, state, pos).updateSprite(state, pos)).setPower(0.2F).scale(0.6F));
		}

		return true;
	}

	@Override
	public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
		if (state.getBlock() != barrelBlock || !(level instanceof ClientLevel clientLevel)) {
			return false;
		}

		VoxelShape voxelshape = state.getShape(level, pos);
		voxelshape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
			double d1 = Math.min(1.0D, maxX - minX);
			double d2 = Math.min(1.0D, maxY - minY);
			double d3 = Math.min(1.0D, maxZ - minZ);
			int i = Math.max(2, Mth.ceil(d1 / 0.25D));
			int j = Math.max(2, Mth.ceil(d2 / 0.25D));
			int k = Math.max(2, Mth.ceil(d3 / 0.25D));

			for (int l = 0; l < i; ++l) {
				for (int i1 = 0; i1 < j; ++i1) {
					for (int j1 = 0; j1 < k; ++j1) {
						double d4 = (l + 0.5D) / i;
						double d5 = (i1 + 0.5D) / j;
						double d6 = (j1 + 0.5D) / k;
						double d7 = d4 * d1 + minX;
						double d8 = d5 * d2 + minY;
						double d9 = d6 * d3 + minZ;
						manager.add(new CustomTintTerrainParticle(clientLevel, pos.getX() + d7, pos.getY() + d8, pos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D, state, pos).updateSprite(state, pos));
					}
				}
			}
		});
		return true;
	}
}
