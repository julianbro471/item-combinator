package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Fighting abilities: what happens when a player hits or kills something with a combined weapon,
 * plus the defensive abilities of charms and armor (no fall damage, thorns, the Eternal Totem).
 */
public final class CombatAbilities {
	private static final TagKey<DamageType> IS_FALL = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("is_fall"));
	private static final TagKey<DamageType> BYPASSES_INVULNERABILITY = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("bypasses_invulnerability"));

	/** True while this class deals extra damage itself, so that damage does not trigger abilities again. */
	private static boolean dealingExtra = false;
	/** World time of the last lightning or blast per player. */
	private static final Map<UUID, Long> LAST_STRIKE = new HashMap<>();

	private CombatAbilities() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(CombatAbilities::allowDamage);
		ServerLivingEntityEvents.ALLOW_DEATH.register(CombatAbilities::allowDeath);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			try {
				afterDamage(entity, source, damage, blocked);
			} catch (Throwable t) {
				ItemCombinator.LOGGER.error("Combat ability failed", t);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			try {
				afterDeath(entity, source);
			} catch (Throwable t) {
				ItemCombinator.LOGGER.error("Kill ability failed", t);
			}
		});
	}

	// ------------------------------------------------------------------ defense

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		try {
			if (entity instanceof ServerPlayerEntity player && source.isIn(IS_FALL) && PassiveAbilities.has(player, traits -> traits.noFall)) {
				return false;
			}
		} catch (Throwable t) {
			ItemCombinator.LOGGER.error("Fall protection failed", t);
		}
		return true;
	}

	/** The Eternal Totem. Returning false means the player does not die. */
	private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		try {
			if (!(entity instanceof ServerPlayerEntity player) || source.isIn(BYPASSES_INVULNERABILITY)) {
				return true;
			}
			ItemStack totem = PassiveAbilities.find(player, traits -> traits.totemCooldown > 0);
			if (totem == null || player.getItemCooldownManager().isCoolingDown(totem.getItem())) {
				return true;
			}
			Traits totemTraits = ComboItems.traits(totem.getItem());
			player.setHealth(8.0F);
			player.clearStatusEffects();
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
			player.getItemCooldownManager().set(totem.getItem(), totemTraits.totemCooldown);
			player.getWorld().sendEntityStatus(player, (byte) 35); // 35 = totem animation and sound
			return false;
		} catch (Throwable t) {
			ItemCombinator.LOGGER.error("Eternal Totem failed", t);
			return true;
		}
	}

	// ------------------------------------------------------------------ hits

	private static void afterDamage(LivingEntity victim, DamageSource source, float damage, boolean blocked) {
		if (!(victim.getWorld() instanceof ServerWorld world)) {
			return;
		}

		// Thorns-like armor of the victim.
		if (victim instanceof ServerPlayerEntity defender && source.getAttacker() instanceof LivingEntity attacker
				&& attacker != victim && !dealingExtra && !source.isOf(DamageTypes.THORNS) && !source.isOf(DamageTypes.EXPLOSION)
				&& !source.isOf(DamageTypes.PLAYER_EXPLOSION)) {
			PassiveAbilities.forEachActive(defender, (stack, armor) -> {
				if (armor.hasThorns) {
					if (armor.thornsDamage > 0) {
						attacker.damage(world.getDamageSources().thorns(defender), armor.thornsDamage);
					}
					if (armor.thornsFire > 0) {
						attacker.setOnFireFor(armor.thornsFire);
					}
					if (armor.thornsBoom > 0) {
						Fx.boom(world, defender, attacker.getPos().add(0.0, attacker.getHeight() * 0.5, 0.0), armor.thornsBoom, false, false);
					}
				}
				return false;
			});
		}

		// Weapon of the attacker. Only direct melee hits count, not arrows and not this mod's own extra damage.
		if (dealingExtra || blocked || !(source.getAttacker() instanceof ServerPlayerEntity player)
				|| source.getSource() != player || !source.isOf(DamageTypes.PLAYER_ATTACK)) {
			return;
		}
		Traits traits = ComboItems.traits(player.getMainHandStack().getItem());
		if (traits == null || !traits.hasCombat()) {
			return;
		}

		if (traits.midas && Spells.goldify(world, victim)) {
			return; // the mob is a gold block now
		}
		if (traits.igniteSeconds > 0) {
			victim.setOnFireFor(traits.igniteSeconds);
		}
		for (Traits.Fx fx : traits.hitEffects) {
			victim.addStatusEffect(new StatusEffectInstance(fx.effect(), fx.ticks(), fx.amplifier()), player);
		}
		if (traits.freeze) {
			victim.setFrozenTicks(Math.max(victim.getFrozenTicks(), victim.getMinFreezeDamageTicks() + 60));
		}
		if (traits.lifesteal > 0) {
			player.heal(Math.max(0.5F, damage * traits.lifesteal));
		}
		if (traits.knockUp > 0) {
			victim.addVelocity(0.0, traits.knockUp, 0.0);
			victim.velocityModified = true;
		}
		if (traits.knockback > 0) {
			Vec3d look = player.getRotationVec(1.0F);
			victim.takeKnockback(traits.knockback, -look.x, -look.z);
			victim.velocityModified = true;
		}
		if (traits.magicDamage > 0) {
			extraDamage(world, player, victim, traits.magicDamage);
		}
		areaEffects(world, player, victim, traits, damage, true);
		if (traits.instakill && victim.isAlive()) {
			killBlow(world, player, victim);
		}
	}

	/** Extra damage that ignores the short "just got hit" protection of the target. */
	private static void extraDamage(ServerWorld world, ServerPlayerEntity player, LivingEntity target, float amount) {
		if (!target.isAlive()) {
			return;
		}
		boolean before = dealingExtra;
		dealingExtra = true;
		try {
			target.timeUntilRegen = 0;
			target.damage(world.getDamageSources().indirectMagic(player, player), amount);
		} finally {
			dealingExtra = before;
		}
	}

	/** Fist of Doom: more damage than anything has health. Counts as a normal player kill, so loot and XP drop. */
	private static void killBlow(ServerWorld world, ServerPlayerEntity player, LivingEntity target) {
		boolean before = dealingExtra;
		dealingExtra = true;
		try {
			target.timeUntilRegen = 0;
			target.damage(world.getDamageSources().playerAttack(player), 1000000.0F);
			Fx.particles(world, ParticleTypes.EXPLOSION, target.getPos().add(0.0, 1.0, 0.0), 2, 0.3, 0.0);
		} finally {
			dealingExtra = before;
		}
	}

	/** A lightning bolt that only looks and sounds real: it starts no fires and hurts nobody by itself. */
	private static void fakeBolt(ServerWorld world, Vec3d at) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
		if (bolt != null) {
			bolt.refreshPositionAfterTeleport(at);
			bolt.setCosmetic(true);
			world.spawnEntity(bolt);
		}
	}

	/** Lightning, chain lightning, explosions and sweep. Also runs for killing blows. */
	private static void areaEffects(ServerWorld world, ServerPlayerEntity player, LivingEntity victim, Traits traits, float damage, boolean victimAlive) {
		boolean ready = true;
		if (traits.strikeCooldown > 0) {
			long now = world.getTime();
			Long last = LAST_STRIKE.get(player.getUuid());
			ready = last == null || now - last >= traits.strikeCooldown || now < last;
			if (ready && (traits.lightningDamage > 0 || traits.blastRadius > 0 || traits.boomHit > 0 || traits.chainRadius > 0)) {
				LAST_STRIKE.put(player.getUuid(), now);
			}
		}
		Vec3d at = victim.getPos();

		if (ready && traits.lightningDamage > 0) {
			fakeBolt(world, at);
			if (victimAlive) {
				extraDamage(world, player, victim, traits.lightningDamage);
			}
		}

		if (ready && traits.chainRadius > 0) {
			int jumps = 0;
			for (LivingEntity other : nearby(world, player, victim, traits.chainRadius)) {
				if (jumps++ >= 8) {
					break;
				}
				fakeBolt(world, other.getPos());
				extraDamage(world, player, other, traits.chainDamage);
			}
		}

		if (ready && traits.blastRadius > 0) {
			Fx.particles(world, ParticleTypes.EXPLOSION, at.add(0.0, 1.0, 0.0), 3, 0.6, 0.0);
			Fx.sound(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
			if (victimAlive) {
				extraDamage(world, player, victim, traits.blastDamage * 0.5F);
			}
			for (LivingEntity other : nearby(world, player, victim, traits.blastRadius)) {
				extraDamage(world, player, other, traits.blastDamage);
			}
		}

		if (ready && traits.boomHit > 0) {
			boolean before = dealingExtra;
			dealingExtra = true;
			try {
				// A real explosion that breaks blocks. The player who swings the weapon is immune.
				Fx.boom(world, player, at.add(0.0, victim.getHeight() * 0.5, 0.0), traits.boomHit, false, true);
			} finally {
				dealingExtra = before;
			}
		}

		if (traits.sweepRadius > 0 && damage > 0) {
			List<LivingEntity> others = nearby(world, player, victim, traits.sweepRadius);
			if (!others.isEmpty()) {
				Fx.particles(world, ParticleTypes.SWEEP_ATTACK, at.add(0.0, 1.0, 0.0), 3, 0.8, 0.0);
				boolean before = dealingExtra;
				dealingExtra = true;
				try {
					for (LivingEntity other : others) {
						other.damage(world.getDamageSources().playerAttack(player), damage * traits.sweepFraction);
					}
				} finally {
					dealingExtra = before;
				}
			}
		}
	}

	/** Living things near the victim that an area attack should hit. Pets, players and armor stands are spared. */
	private static List<LivingEntity> nearby(ServerWorld world, PlayerEntity player, LivingEntity victim, float radius) {
		Box box = victim.getBoundingBox().expand(radius);
		return world.getEntitiesByClass(LivingEntity.class, box, other ->
				other != victim && other != player && other.isAlive()
						&& !(other instanceof PlayerEntity)
						&& !(other instanceof ArmorStandEntity)
						&& !(other instanceof TameableEntity pet && pet.isTamed()));
	}

	// ------------------------------------------------------------------ kills

	private static void afterDeath(LivingEntity victim, DamageSource source) {
		if (!(victim.getWorld() instanceof ServerWorld world) || !(source.getAttacker() instanceof ServerPlayerEntity player)) {
			return;
		}
		Traits traits = ComboItems.traits(player.getMainHandStack().getItem());
		if (traits == null) {
			return;
		}
		boolean melee = source.getSource() == player && source.isOf(DamageTypes.PLAYER_ATTACK);
		if (traits.nuggets > 0) {
			victim.dropStack(new ItemStack(Items.GOLD_NUGGET, 1 + world.random.nextInt(traits.nuggets)));
		}
		if (traits.bonusXp > 0) {
			ExperienceOrbEntity.spawn(world, victim.getPos(), traits.bonusXp);
		}
		if (traits.headChance > 0 && world.random.nextFloat() < traits.headChance) {
			Item head = headOf(victim.getType());
			if (head != null) {
				victim.dropStack(new ItemStack(head));
			}
		}
		if (traits.lifesteal > 0) {
			player.heal(2.0F);
		}
		if (traits.midas && melee && !dealingExtra) {
			Spells.goldify(world, victim);
		}
		// A killing blow does not fire the "after damage" event, so area effects are started here instead.
		if (!dealingExtra && traits.hasCombat() && melee) {
			float weaponDamage = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
			areaEffects(world, player, victim, traits, weaponDamage, false);
		}
	}

	private static Item headOf(EntityType<?> type) {
		if (type == EntityType.ZOMBIE) {
			return Items.ZOMBIE_HEAD;
		}
		if (type == EntityType.SKELETON) {
			return Items.SKELETON_SKULL;
		}
		if (type == EntityType.CREEPER) {
			return Items.CREEPER_HEAD;
		}
		if (type == EntityType.WITHER_SKELETON) {
			return Items.WITHER_SKELETON_SKULL;
		}
		if (type == EntityType.PIGLIN) {
			return Items.PIGLIN_HEAD;
		}
		return null;
	}
}
