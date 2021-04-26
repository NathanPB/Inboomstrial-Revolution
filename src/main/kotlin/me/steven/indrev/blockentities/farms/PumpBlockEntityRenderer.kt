package me.steven.indrev.blockentities.farms

import alexiil.mc.lib.attributes.fluid.render.FluidRenderFace
import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume
import me.steven.indrev.blocks.machine.HorizontalFacingMachineBlock
import me.steven.indrev.utils.identifier
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.util.math.Direction
import kotlin.math.floor

class PumpBlockEntityRenderer(dispatcher: BlockEntityRenderDispatcher) : BlockEntityRenderer<PumpBlockEntity>(dispatcher) {
    override fun render(
        entity: PumpBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.run {

            push()
            val inputVolume = entity.fluidComponent!![0]
            if (!inputVolume.isEmpty) {

                translate(0.5, 0.5, 0.5)
                var direction = entity.cachedState[HorizontalFacingMachineBlock.HORIZONTAL_FACING]
                if (direction.axis == Direction.Axis.X) direction = direction.opposite
                multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(direction.asRotation()))
                translate(-0.5, -0.5, -0.5)
                push()
                multiply(Vector3f.NEGATIVE_X.getDegreesQuaternion(22.5f))
                renderFluid(inputVolume)
                pop()

                push()
                multiply(Vector3f.NEGATIVE_Z.getDegreesQuaternion(22.5f))
                translate(0.5, 0.5, 0.5)
                multiply(Vector3f.NEGATIVE_Y.getDegreesQuaternion(90f))
                translate(-0.5, -0.5, -0.5)
                matrices.translate(0.0, 0.38, 0.0765)
                renderFluid(inputVolume)
                pop()

                push()
                multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(22.5f))
                translate(0.5, 0.5, 0.5)
                multiply(Vector3f.NEGATIVE_Y.getDegreesQuaternion(180f))
                translate(-0.5, -0.5, -0.5)
                matrices.translate(0.1284, 0.0, 0.1285)
                renderFluid(inputVolume)
                pop()

                FluidVolumeRenderer.VCPS.draw()
            }
            pop()

            val currentY = floor(entity.movingTicks).toInt()
            for (y in 1..currentY) {
                push()
                translate(0.0, -y.toDouble(), 0.0)
                renderModel(vertexConsumers, entity)
                pop()
            }
            if (currentY.toDouble() != entity.movingTicks) {
                push()
                scale(1.01f, 1f, 1.01f)
                translate(-0.005, -entity.movingTicks, -0.005)
                renderModel(vertexConsumers, entity)
                pop()
            }
        }
    }

    private fun MatrixStack.renderModel(vertexConsumers: VertexConsumerProvider, entity: PumpBlockEntity) {
        val model = MinecraftClient.getInstance().bakedModelManager.getModel(ModelIdentifier(identifier("pump_pipe"), ""))
        val buffer = vertexConsumers.getBuffer(RenderLayers.getBlockLayer(entity.cachedState))
        val light = WorldRenderer.getLightmapCoordinates(entity.world, entity.pos)
        MinecraftClient.getInstance().blockRenderManager.modelRenderer.render(
            peek(), buffer, null, model, -1f, -1f, -1f, light, OverlayTexture.DEFAULT_UV
        )
    }

    private fun MatrixStack.renderFluid(inputVolume: FluidVolume) {
        val yMax = 0.15 + (0.16 * (1 / inputVolume.amount().asInexactDouble()))
        val face =
            listOf(
                FluidRenderFace.createFlatFaceZ(0.443, 0.15, 0.32, 0.556, yMax, 0.32, 2.0, false, false),
                FluidRenderFace.createFlatFaceZ(0.443, 0.15, 0.32+ (0.55-0.443), 0.556, yMax, 0.32+ (0.55-0.443), 2.0, true, false),
                FluidRenderFace.createFlatFaceX(0.443, 0.15, 0.323, 0.443, yMax, 0.426, 2.0, false, false),
                FluidRenderFace.createFlatFaceX(0.443+ (0.55-0.443), 0.15, 0.323, 0.443+ (0.55-0.443), yMax, 0.426, 2.0, true, false)
            )
        inputVolume.render(face, FluidVolumeRenderer.VCPS, this)
    }

    override fun rendersOutsideBoundingBox(blockEntity: PumpBlockEntity?): Boolean = true
}