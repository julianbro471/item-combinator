package com.combinator.ability;

import com.combinator.ItemCombinator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * A thrown Noclip Pearl. To the game it is a snowball that looks like the pearl, so it needs no entity type of its own.
 * Where it lands, the thrower clips into the Backrooms, and whatever it hit comes along.
 */
public class ThrownNoclipPearl extends SnowballEntity {
	public ThrownNoclipPearl(World world, LivingEntity owner) {
		super(world, owner);
	}

	@Override
	protected void onCollision(HitResult hit) {
		if (this.getWorld().isClient || this.isRemoved()) {
			return;
		}
		Entity target = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
		try {
			Worlds.pearlLanded(this, target);
		} catch (Throwable t) {
			ItemCombinator.error("Thrown Noclip Pearl failed", t);
		}
		this.discard();
	}
}
