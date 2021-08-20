package maninhouse.epicfight.skill;

import java.util.UUID;

import maninhouse.epicfight.animation.LivingMotion;
import maninhouse.epicfight.capabilities.entity.player.PlayerData;
import maninhouse.epicfight.capabilities.entity.player.ServerPlayerData;
import maninhouse.epicfight.client.ClientEngine;
import maninhouse.epicfight.entity.eventlistener.PlayerEventListener.EventType;
import maninhouse.epicfight.gamedata.Animations;
import maninhouse.epicfight.gamedata.Sounds;
import maninhouse.epicfight.network.server.STCLivingMotionChange;
import maninhouse.epicfight.particle.HitParticleType;
import maninhouse.epicfight.particle.Particles;
import maninhouse.epicfight.utils.game.IExtendedDamageSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

public class LiechtenauerSkill extends SpecialAttackSkill {
	private static final UUID EVENT_UUID = UUID.fromString("244c57c0-a837-11eb-bcbc-0242ac130002");
	
	public LiechtenauerSkill(String skillName) {
		super(40.0F, 4, ActivateType.DURATION_INFINITE, skillName);
	}
	
	@Override
	public void onInitiate(SkillContainer container) {
		container.maxDuration = this.maxDuration + EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.SWEEPING, container.executer.getOriginalEntity());
		container.executer.getEventListener().addEventListener(EventType.DEALT_DAMAGE_POST_EVENT, EVENT_UUID, (event) -> {
			if (container.isActivated()) {
				if (!event.getTarget().isAlive()) {
					this.setDurationSynchronize(event.getPlayerData(), container.duration + 1);
				}
			}
			return false;
		});
		
		container.executer.getEventListener().addEventListener(EventType.HIT_EVENT, EVENT_UUID, (event) -> {
			if (container.duration > 0 && this.isExecutableState(event.getPlayerData()) && this.canExecute(event.getPlayerData()) &&
					isBlockableSource(event.getForgeEvent().getSource())) {
				DamageSource damageSource = event.getForgeEvent().getSource();
				boolean isFront = false;
				Vector3d vector3d2 = damageSource.getDamageLocation();
				
				if (vector3d2 != null) {
					Vector3d vector3d = event.getPlayerData().getOriginalEntity().getLook(1.0F);
					Vector3d vector3d1 = vector3d2.subtractReverse(event.getPlayerData().getOriginalEntity().getPositionVec()).normalize();
					vector3d1 = new Vector3d(vector3d1.x, 0.0D, vector3d1.z);
					if (vector3d1.dotProduct(vector3d) < 0.0D) {
						isFront = true;
					}
				}
				
				if (isFront) {
					this.setDurationSynchronize(event.getPlayerData(), container.duration - 1);
					event.getPlayerData().playAnimationSynchronize(Animations.LONGSWORD_GUARD_HIT, 0);
					event.getPlayerData().playSound(Sounds.CLASH, -0.1F, 0.2F);
					Entity playerentity = event.getPlayerData().getOriginalEntity();
					Particles.HIT_BLUNT.get().spawnParticleWithArgument(((ServerWorld)playerentity.world), HitParticleType.POSITION_MIDDLE_OF_EACH_ENTITY,
							HitParticleType.ARGUMENT_ZERO, playerentity, damageSource.getImmediateSource());
					
					float knockback = 0.25F;
					
					if (damageSource instanceof IExtendedDamageSource) {
						knockback += ((IExtendedDamageSource)damageSource).getImpact() * 0.1F;
					}
					if (damageSource.getImmediateSource() instanceof LivingEntity) {
						knockback += EnchantmentHelper.getKnockbackModifier((LivingEntity)damageSource.getImmediateSource()) * 0.1F;
					}
					
					event.getPlayerData().knockBackEntity(damageSource.getImmediateSource(), knockback);
					return true;
				}
			}
			
			return false;
		});
		
		container.executer.getEventListener().addEventListener(EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID, (event) -> {
			if (event.getPlayerData().getSkill(this.slot).isActivated()) {
				ClientPlayerEntity clientPlayer = event.getPlayerData().getOriginalEntity();
				clientPlayer.setSprinting(false);
				clientPlayer.sprintToggleTimer = -1;
				ClientEngine.INSTANCE.inputController.setKeyBind(Minecraft.getInstance().gameSettings.keyBindSprint, false);
			}
			
			return false;
		});
		
		
	}
	
	@Override
	public void onRemoved(SkillContainer container) {
		container.executer.getEventListener().removeListener(EventType.HIT_EVENT, EVENT_UUID);
		container.executer.getEventListener().removeListener(EventType.DEALT_DAMAGE_POST_EVENT, EVENT_UUID);
		container.executer.getEventListener().removeListener(EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID);
	}
	
	@Override
	public void executeOnServer(ServerPlayerData executer, PacketBuffer args) {
		if (executer.getSkill(this.slot).isActivated()) { 
			super.cancelOnServer(executer, args);
			this.setConsumptionSynchronize(executer, this.consumption * ((float)executer.getSkill(this.slot).duration /
					(this.maxDuration + EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.SWEEPING, executer.getOriginalEntity()) + 1)));
			this.setDurationSynchronize(executer, 0);
			this.setStackSynchronize(executer, executer.getSkill(this.slot).getStack() - 1);
			executer.setLivingMotionCurrentItem(executer.getHeldItemCapability(Hand.MAIN_HAND));
		} else {
			this.setDurationSynchronize(executer, this.maxDuration + EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.SWEEPING, executer.getOriginalEntity()));
			executer.getSkill(this.slot).activate();
			STCLivingMotionChange msg = new STCLivingMotionChange(executer.getOriginalEntity().getEntityId(), 6);
			msg.setMotions(LivingMotion.IDLE, LivingMotion.WALK, LivingMotion.RUN, LivingMotion.JUMP, LivingMotion.KNEEL, LivingMotion.SNEAK);
			msg.setAnimations(Animations.BIPED_IDLE_LONGSWORD, Animations.BIPED_WALK_LONGSWORD, Animations.BIPED_WALK_LONGSWORD, Animations.BIPED_WALK_LONGSWORD,
					Animations.BIPED_WALK_LONGSWORD, Animations.BIPED_WALK_LONGSWORD);
			((ServerPlayerData)executer).modifyLivingMotion(msg);
		}
	}
	
	@Override
	public void cancelOnServer(ServerPlayerData executer, PacketBuffer args) {
		super.cancelOnServer(executer, args);
		this.setConsumptionSynchronize(executer, 0);
		this.setStackSynchronize(executer, executer.getSkill(this.slot).getStack() - 1);
		executer.setLivingMotionCurrentItem(executer.getHeldItemCapability(Hand.MAIN_HAND));
	}
	
	@Override
	public boolean canExecute(PlayerData<?> executer) {
		return executer.getHeldItemCapability(Hand.MAIN_HAND).getSpecialAttack(executer) == this && executer.getOriginalEntity().getRidingEntity() == null;
	}
	
	@Override
	public SpecialAttackSkill registerPropertiesToAnimation() {
		return this;
	}
	
	private static boolean isBlockableSource(DamageSource damageSource) {
		return !damageSource.isUnblockable() && !damageSource.isExplosion();
	}
}