package maninhouse.epicfight.capabilities.entity.mob;

import java.util.Iterator;
import java.util.Set;

import maninhouse.epicfight.animation.LivingMotion;
import maninhouse.epicfight.animation.types.StaticAnimation;
import maninhouse.epicfight.client.animation.AnimatorClient;
import maninhouse.epicfight.entity.ai.AttackPatternGoal;
import maninhouse.epicfight.entity.ai.AttackPatternPercentGoal;
import maninhouse.epicfight.entity.ai.ChasingGoal;
import maninhouse.epicfight.entity.ai.attribute.ModAttributes;
import maninhouse.epicfight.gamedata.Animations;
import maninhouse.epicfight.gamedata.Models;
import maninhouse.epicfight.gamedata.Sounds;
import maninhouse.epicfight.model.Model;
import maninhouse.epicfight.utils.game.IExtendedDamageSource;
import maninhouse.epicfight.utils.game.IExtendedDamageSource.StunType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;

public class IronGolemData extends BipedMobData<IronGolemEntity> {
	private int deathTimerExt;
	
	public IronGolemData() {
		super(Faction.VILLAGER);
	}
	
	@Override
	public void onEntityJoinWorld(IronGolemEntity entityIn) {
		super.onEntityJoinWorld(entityIn);
		Set<PrioritizedGoal> goals = this.orgEntity.goalSelector.goals;
		Iterator<PrioritizedGoal> iterator = goals.iterator();
		Goal toRemove = null;
		
		while (iterator.hasNext()) {
			PrioritizedGoal goal = iterator.next();
			Goal inner = goal.getGoal();

			if (inner instanceof MoveTowardsTargetGoal) {
				toRemove = inner;
				break;
			}
		}

		if (toRemove != null) {
			orgEntity.goalSelector.removeGoal(toRemove);
		}

		this.orgEntity.entityCollisionReduction = 0.2F;
	}
	
	@Override
	protected void initAttributes() {
		super.initAttributes();
		this.orgEntity.getAttribute(ModAttributes.MAX_STRIKES.get()).setBaseValue(4.0D);
		this.orgEntity.getAttribute(ModAttributes.IMPACT.get()).setBaseValue(10.0D);
	}
	
	@Override
	protected void initAnimator(AnimatorClient animatorClient) {
		super.initAnimator(animatorClient);
		animatorClient.addLivingAnimation(LivingMotion.IDLE, Animations.GOLEM_IDLE);
		animatorClient.addLivingAnimation(LivingMotion.WALK, Animations.GOLEM_WALK);
		animatorClient.addLivingAnimation(LivingMotion.DEATH, Animations.GOLEM_DEATH);
	}

	@Override
	public void updateMotion() {
		super.commonCreatureUpdateMotion();
	}

	@Override
	public void update() {
		if (this.orgEntity.getHealth() <= 0.0F) {
			this.orgEntity.rotationPitch = 0;

			if (this.orgEntity.deathTime > 1 && this.deathTimerExt < 20) {
				this.deathTimerExt++;
				this.orgEntity.deathTime--;
			}
		}
		
		super.update();
	}

	@Override
	public void setAIAsUnarmed() {
		this.orgEntity.goalSelector.addGoal(0, new AttackPatternPercentGoal(this, this.orgEntity, 0.0D, 1.5D, 0.3F, true, MobAttackPatterns.GOLEM_SWINGARM));
		this.orgEntity.goalSelector.addGoal(0, new AttackPatternPercentGoal(this, this.orgEntity, 1.0D, 2.5D, 0.15F, true, MobAttackPatterns.GOLEM_HEADBUTT));
		this.orgEntity.goalSelector.addGoal(0, new AttackPatternGoal(this, this.orgEntity, 0.0D, 2.0D, true, MobAttackPatterns.GOLEM_SMASH_GROUND));
		this.orgEntity.goalSelector.addGoal(1, new ChasingGoal(this, this.orgEntity, 1.0D, false));
	}
	
	@Override
	public void setAIAsArmed() {
		this.setAIAsUnarmed();
	}

	@Override
	public SoundEvent getWeaponHitSound(Hand hand) {
		return Sounds.BLUNT_HIT_HARD;
	}

	@Override
	public SoundEvent getSwingSound(Hand hand) {
		return Sounds.WHOOSH_BIG;
	}
	
	@Override
	public float getDamageToEntity(Entity targetEntity, IExtendedDamageSource source, Hand hand) {
		float damage = super.getDamageToEntity(targetEntity, source, hand);
		return (int)damage > 0 ? damage / 2.0F + (float)this.orgEntity.getRNG().nextInt((int)damage) : damage;
	}

	@Override
	public StaticAnimation getHitAnimation(StunType stunType) {
		return null;
	}

	@Override
	public <M extends Model> M getEntityModel(Models<M> modelDB) {
		return modelDB.ENTITY_GOLEM;
	}
}