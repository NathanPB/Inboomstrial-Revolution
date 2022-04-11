package me.steven.indrev.gui.tooltip.oredatacards

import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import me.steven.indrev.api.OreDataCards
import me.steven.indrev.utils.drawCircle
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.tooltip.TooltipComponent
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.item.ItemRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import net.minecraft.util.math.Matrix4f
import kotlin.math.atan2

class OreDataCardTooltipComponent(val data: OreDataCardTooltipData) : TooltipComponent {
    override fun getHeight(): Int {
        return 18 * (if (data.cardData.rng == 0) 2 else 3) + (18* data.cardData.entries.size / 4) + 18
    }

    override fun getWidth(textRenderer: TextRenderer?): Int {
        return 18
    }

    override fun drawText(
        textRenderer: TextRenderer,
        x: Int,
        y: Int,
        matrix: Matrix4f?,
        vertexConsumers: VertexConsumerProvider.Immediate?
    ) {
        val dataTxt = "${(data.cardData.maxCycles - data.cardData.used) * 100 / OreDataCards.MAX_SIZE}%"
        val width = textRenderer.getWidth(dataTxt)
        textRenderer.draw(dataTxt, x.toFloat() + 18*4 + 13 - width / 2, y.toFloat() + 16, -1, false, matrix, vertexConsumers, false, 0, 15728880)
        textRenderer.draw("Data", x.toFloat() + 3, y.toFloat() + 5,  0x007E7E, false, matrix, vertexConsumers, false, 0, 15728880)

        val richness = LiteralText("Richness: ").styled { s -> s.withColor(0x007E7E) }.append(LiteralText("${(data.cardData.richness * 100).toInt()}%").formatted(Formatting.WHITE))
        textRenderer.draw(richness, x.toFloat() + 3, y.toFloat() +4 + 18 * 2 + (18 * (data.cardData.entries.size / 4)),  -1, false, matrix, vertexConsumers, false, 0, 15728880)

        if (data.cardData.rng == -1) {
            textRenderer.draw("-RNG", x.toFloat(), y.toFloat() + 16 * 5,  -1, false, matrix, vertexConsumers, false, 0, 15728880)
        } else if (data.cardData.rng == 1) {
            textRenderer.draw("+RNG", x.toFloat(), y.toFloat() + 16 * 5,  -1, false, matrix, vertexConsumers, false, 0, 15728880)
        }
    }

    override fun drawItems(
        textRenderer: TextRenderer,
        x: Int,
        y: Int,
        matrices: MatrixStack,
        itemRenderer: ItemRenderer,
        z: Int
    ) {

        data.cardData.entries.forEachIndexed { index, entry ->
            itemRenderer.renderInGui(ItemStack(entry.item), x + 6 + (index % 3 * 18), y + 17 + (18 * (index / 3)))
        }

        matrices.push()
        matrices.translate(0.0, 0.0, z.toDouble())
        val width = 18*2
        val offset = 18*3 + 12
        drawCircle(matrices, 1,1, x + offset, y + 1, width) { _, _ -> 0x22FFFFFF.toInt() }
        drawCircle(matrices, data.cardData.maxCycles, OreDataCards.MAX_SIZE, x + offset, y + 1, width) { _, _ -> 0xFF0000FF.toInt() }
        drawCircle(matrices, data.cardData.used, OreDataCards.MAX_SIZE, x + offset, y + 1, width) { _, _ -> 0xFFFF0000.toInt() }
        matrices.pop()
    }


}