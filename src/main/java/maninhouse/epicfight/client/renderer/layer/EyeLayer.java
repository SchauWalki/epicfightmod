package maninhouse.epicfight.client.renderer.layer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import maninhouse.epicfight.capabilities.entity.LivingData;
import maninhouse.epicfight.client.model.ClientModel;
import maninhouse.epicfight.utils.math.OpenMatrix4f;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EyeLayer<E extends LivingEntity, T extends LivingData<E>> extends Layer<E, T> {
	private final RenderType renderType;
	private final ClientModel eyeModel;
	
	public EyeLayer(ResourceLocation eyeTexture, ClientModel eyeModel) {
		this.renderType = RenderType.getEyes(eyeTexture);
		this.eyeModel = eyeModel;
	}
	
	@Override
	public void renderLayer(T entitydata, E entityliving, MatrixStack matrixStackIn, IRenderTypeBuffer buffer, int packedLightIn, OpenMatrix4f[] poses, float partialTicks) {
		IVertexBuilder ivertexbuilder = buffer.getBuffer(this.renderType);
		this.eyeModel.draw(matrixStackIn, ivertexbuilder, 15728640, 1.0F, 1.0F, 1.0F, 1.0F, poses);
	}
}