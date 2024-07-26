package me.steven.indrev.items.heat

import me.steven.indrev.utils.translatable
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import kotlin.math.absoluteValue

open class IRHeatFactorItem(settings: Settings, val heatFactor: Double) : Item(settings) {

    override fun appendTooltip(
        stack: ItemStack?,
        world: World?,
        tooltip: MutableList<Text>?,
        context: TooltipContext?
    ) {
        super.appendTooltip(stack, world, tooltip, context)

        if (heatFactor > 0) {
            tooltip?.add(translatable("item.indrev.heat.heater", heatFactor.absoluteValue).formatted(Formatting.DARK_RED))
        } else {
            tooltip?.add(translatable("item.indrev.heat.cooler", heatFactor.absoluteValue).formatted(Formatting.AQUA))
        }
    }
}
