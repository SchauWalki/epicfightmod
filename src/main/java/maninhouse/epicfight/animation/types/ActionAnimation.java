package maninhouse.epicfight.animation.types;

import java.util.Map;

import maninhouse.epicfight.animation.AnimationPlayer;
import maninhouse.epicfight.animation.JointKeyframe;
import maninhouse.epicfight.animation.JointTransform;
import maninhouse.epicfight.animation.Pose;
import maninhouse.epicfight.animation.TransformSheet;
import maninhouse.epicfight.capabilities.entity.LivingData;
import maninhouse.epicfight.collada.AnimationDataExtractor;
import maninhouse.epicfight.config.ConfigurationIngame;
import maninhouse.epicfight.model.Armature;
import maninhouse.epicfight.utils.math.OpenMatrix4f;
import maninhouse.epicfight.utils.math.Vec3f;
import maninhouse.epicfight.utils.math.Vec4f;
import net.minecraft.block.BlockState;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;

public class ActionAnimation extends MainFrameAnimation {
	protected final boolean breakMovement;
	protected final boolean affectYCoord;
	protected float delayTime;
	
	public ActionAnimation(int id, float convertTime, boolean breakMove, boolean affectY, String path) {
		this(id, convertTime, Float.MAX_VALUE, breakMove, affectY, path);
	}
	
	public ActionAnimation(int id, float convertTime, float postDelay, boolean breakMove, boolean affectY, String path) {
		super(id, convertTime, path);
		this.breakMovement = breakMove;
		this.affectYCoord = affectY;
		this.delayTime = postDelay;
	}
	
	@Override
	public void onActivate(LivingData<?> entitydata) {
		super.onActivate(entitydata);
		entitydata.cancelUsingItem();
		
		if (this.breakMovement) {
			entitydata.getOriginalEntity().setMotion(0.0D, entitydata.getOriginalEntity().getMotion().y, 0.0D);
		}
	}
	
	@Override
	public void onUpdate(LivingData<?> entitydata) {
		super.onUpdate(entitydata);
		this.move(entitydata, this);
	}
	
	@Override
	public void updateOnLinkAnimation(LivingData<?> entitydata, LinkAnimation linkAnimation) {
		this.move(entitydata, linkAnimation);
	};
	
	private void move(LivingData<?> entitydata, DynamicAnimation animation) {
		LivingEntity livingentity = entitydata.getOriginalEntity();
		
		if (entitydata.isRemote()) {
			if (!(livingentity instanceof ClientPlayerEntity)) {
				return;
			}
		} else {
			if ((livingentity instanceof ServerPlayerEntity)) {
				return;
			}
		}
		
		if (entitydata.getEntityState().isInaction()) {
			Vec3f vec3 = this.getCoordVector(entitydata, animation);
			BlockPos blockpos = new BlockPos(livingentity.getPosX(), livingentity.getBoundingBox().minY - 1.0D, livingentity.getPosZ());
			BlockState blockState = livingentity.world.getBlockState(blockpos);
			ModifiableAttributeInstance attribute = livingentity.getAttribute(Attributes.MOVEMENT_SPEED);
			boolean soulboost = blockState.isIn(BlockTags.SOUL_SPEED_BLOCKS) && EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.SOUL_SPEED, livingentity) > 0;
			double speedFactor = soulboost ? 1.0D : livingentity.world.getBlockState(blockpos).getBlock().getSpeedFactor();
			double moveMultiplier = (attribute.getValue() / attribute.getBaseValue()) * speedFactor;
			livingentity.move(MoverType.SELF, new Vector3d(vec3.x * moveMultiplier, vec3.y, vec3.z * moveMultiplier));
		}
	}
	
	@Override
	public EntityState getState(float time) {
		if (time <= this.delayTime) {
			return EntityState.PRE_DELAY;
		} else {
			return EntityState.CANCELABLE_POST_DELAY;
		}
	}
	
	@Override
	public Pose getPoseByTime(DynamicAnimation animation, LivingData<?> entitydata, float time) {
		Pose pose = new Pose();
		for (String jointName : animation.jointTransforms.keySet()) {
			if (!entitydata.isRemote() || this.isEnabledJoint(jointName)) {
				JointTransform jt = animation.jointTransforms.get(jointName).getInterpolatedTransform(time);
				if (jointName.equals("Root")) {
					Vec3f vec = jt.getPosition();
					vec.x = 0.0F;
					vec.y = this.affectYCoord && vec.y > 0.0F ? 0.0F : vec.y;
					vec.z = 0.0F;
				}
				pose.putJointData(jointName, jt);
			}
		}
		return pose;
	}
	
	protected Pose getRawPose(LivingData<?> entitydata, float time) {
		Pose pose = new Pose();
		for (String jointName : this.jointTransforms.keySet()) {
			JointTransform jt = this.jointTransforms.get(jointName).getInterpolatedTransform(time);
			pose.putJointData(jointName, jt);
		}
		return pose;
	}
	
	@Override
	public void getLinkAnimation(Pose pose1, float timeModifier, LivingData<?> entitydata, LinkAnimation dest) {
		float totalTime = timeModifier > 0.0F ? timeModifier + this.convertTime : this.convertTime;
		float updatePerTicks = this.getPlaySpeed(entitydata) * ConfigurationIngame.A_TICK;
		float nextStart = 0;
		for (; nextStart < totalTime; nextStart += updatePerTicks) {
		}
		nextStart -= totalTime;
		
		if (timeModifier < 0.0F) {
			nextStart -= timeModifier;
			dest.startsAt = nextStart;
		}
		
		dest.getTransfroms().clear();
		dest.setTotalTime(totalTime);
		dest.setNextAnimation(this);
		Map<String, JointTransform> data1 = pose1.getJointTransformData();
		Map<String, JointTransform> data2 = this.getRawPose(entitydata, nextStart).getJointTransformData();
		
		for (String jointName : data1.keySet()) {
			JointKeyframe[] keyframes = new JointKeyframe[2];
			keyframes[0] = new JointKeyframe(0, data1.get(jointName));
			keyframes[1] = new JointKeyframe(totalTime, data2.get(jointName));

			TransformSheet sheet = new TransformSheet(keyframes);
			dest.addSheet(jointName, sheet);
		}
	}
	
	protected Vec3f getCoordVector(LivingData<?> entitydata, DynamicAnimation animation) {
		if (animation.jointTransforms.containsKey("Root")) {
			LivingEntity elb = entitydata.getOriginalEntity();
			AnimationPlayer player = entitydata.getAnimator().getPlayerFor(animation);
			JointTransform jt = animation.jointTransforms.get("Root").getInterpolatedTransform(player.getElapsedTime());
			JointTransform prevJt = animation.jointTransforms.get("Root").getInterpolatedTransform(player.getPrevElapsedTime());
			Vec4f currentPos = new Vec4f(jt.getPosition().x, jt.getPosition().y, jt.getPosition().z, 1.0F);
			Vec4f prevPos = new Vec4f(prevJt.getPosition().x, prevJt.getPosition().y, prevJt.getPosition().z, 1.0F);
			OpenMatrix4f mat = entitydata.getModelMatrix(1.0F);
			mat.m30 = 0;
			mat.m31 = 0;
			mat.m32 = 0;
			OpenMatrix4f.transform(mat, currentPos, currentPos);
			OpenMatrix4f.transform(mat, prevPos, prevPos);
			boolean hasNoGravity = entitydata.getOriginalEntity().hasNoGravity();
			float dx = prevPos.x - currentPos.x;
			float dy = (this.affectYCoord && currentPos.y > 0.0F) || hasNoGravity ? currentPos.y - prevPos.y : 0.0F;
			float dz = prevPos.z - currentPos.z;
			dx = Math.abs(dx) > 0.0000001F ? dx : 0.0F;
			dz = Math.abs(dz) > 0.0000001F ? dz : 0.0F;
			if (this.affectYCoord && currentPos.y > 0.0F && !hasNoGravity) {
				Vector3d motion = elb.getMotion();
				elb.setMotion(motion.x, motion.y <= 0 ? motion.y + 0.08D : motion.y, motion.z);
			}
			return new Vec3f(dx, dy, dz);
		} else {
			return new Vec3f(0, 0, 0);
		}
	}
	
	@Override
	public StaticAnimation loadAnimation(Armature armature, Dist dist) {
		if (this.animationLocation != null) {
			AnimationDataExtractor.extractActionAnimation(this.animationLocation, this, armature, dist);
		}
		
		return this;
	}
}