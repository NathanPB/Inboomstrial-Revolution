package me.steven.indrev.gui.tooltip.energy

import com.mojang.blaze3d.systems.RenderSystem
import me.steven.indrev.utils.getEnergyString
import me.steven.indrev.utils.identifier
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.tooltip.TooltipComponent
import net.minecraft.client.render.VertexConsumerProvider
import me.steven.indrev.utils.literal
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Formatting
import org.joml.Matrix4f

open class EnergyTooltipComponent(private val data: EnergyTooltipData) : TooltipComponent {
    override fun getHeight(): Int = 18

    override fun getWidth(textRenderer: TextRenderer): Int {
        val percentage = if (data.maxEnergy > 0) data.energy * 100 / data.maxEnergy else 0
        val text =
            literal("${getEnergyString(data.energy)} LF (${percentage.toInt()}%)").formatted(Formatting.GRAY)
        return textRenderer.getWidth(text) + 20
    }

    override fun drawItems(textRenderer: TextRenderer, x: Int, y: Int, ctx: DrawContext) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, identifier("textures/gui/energy_icon.png"))
        ctx.drawTexture(identifier("textures/gui/energy_icon.png"), x, y, 0, 0f, 0f, 18, 18, 18, 18)
    }

    override fun drawText(
        textRenderer: TextRenderer,
        x: Int,
        y: Int,
        matrix4f: Matrix4f,
        immediate: VertexConsumerProvider.Immediate
    ) {
        val percentage = if (data.maxEnergy > 0) data.energy * 100 / data.maxEnergy else 0
        val text =
            literal("${getEnergyString(data.energy)} LF (${percentage.toInt()}%)").formatted(Formatting.GRAY)
        textRenderer.draw(text.asOrderedText(), x.toFloat() + 19, (y.toFloat() + 9) - textRenderer.fontHeight / 2, -1, true, matrix4f, immediate, TextRenderer.TextLayerType.NORMAL, 0, 15728880)
    }
}