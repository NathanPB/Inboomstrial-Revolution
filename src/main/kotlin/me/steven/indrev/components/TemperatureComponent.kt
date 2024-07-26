package me.steven.indrev.components

import me.steven.indrev.blockentities.BaseBlockEntity
import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.items.heat.IRHeatFactorItem
import me.steven.indrev.items.upgrade.Enhancer
import me.steven.indrev.registry.IRItemRegistry
import me.steven.indrev.utils.component1
import me.steven.indrev.utils.component2
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.world.World

class TemperatureComponent(
    private val blockEntity: BaseBlockEntity,
    private val baseHeatingSpeed: Double,
    val optimalRange: IntRange,
    val limit: Int
) {

    var temperature: Double = 25.0

    init {
        blockEntity.trackInt(MachineBlockEntity.TEMPERATURE_ID) { temperature.toInt() }
        blockEntity.trackInt(MachineBlockEntity.MAX_TEMPERATURE_ID) { limit }
    }

    private var ticks = 0

    fun readNbt(tag: NbtCompound?) {
        temperature = tag?.getDouble("Temperature") ?: 0.0
    }

    fun writeNbt(tag: NbtCompound): NbtCompound {
        tag.putDouble("Temperature", temperature)
        return tag
    }

    fun isFullEfficiency(): Boolean {
        return temperature.toInt() in optimalRange
    }

    private fun getHeatingSpeed(): Double {
        val machine = blockEntity as? MachineBlockEntity<*> ?: return this.baseHeatingSpeed
        val enhancerComponent = machine.enhancerComponent ?: return this.baseHeatingSpeed

        val speedEnhancersMultiplier = enhancerComponent.enhancers.getInt(Enhancer.SPEED).let { it * it }
        val coolerFactor = ((machine.inventoryComponent?.inventory?.coolerStack?.item as? IRHeatFactorItem)?.heatFactor ?: 0.0) * 1.5

        return this.baseHeatingSpeed * speedEnhancersMultiplier + coolerFactor + 1
    }

    private fun getCoolingSpeed(): Double {
        val machine = blockEntity as? MachineBlockEntity<*> ?: return this.baseHeatingSpeed / 1.5
        val coolerItem = (machine.inventoryComponent?.inventory?.coolerStack?.item as? IRHeatFactorItem) ?: return this.baseHeatingSpeed / 1.5
        return (coolerItem.heatFactor * -1).coerceAtLeast(0.0)
    }

    private fun getExplosionPower(): Float {
        val tempLowerBound = optimalRange.last

        val powerUpperBound = 25f
        val powerLowerBound = 3f

        val power = powerLowerBound + (temperature - tempLowerBound) * (powerUpperBound - powerLowerBound) / (5000 - tempLowerBound)
        return power.toFloat().coerceIn(powerLowerBound, powerUpperBound)
    }

    fun tick(shouldHeatUp: Boolean) {
        ticks++
        val machine = blockEntity as? MachineBlockEntity<*>
        val random = blockEntity.world!!.random
        val inv = machine?.inventoryComponent?.inventory
        val (coolerStack, coolerItem) = inv?.coolerStack ?: ItemStack.EMPTY

        fun consumeCooler() {
            if (coolerStack.isDamageable && ticks % 120 == 0) coolerStack.damage(1, random, null)
            if (coolerStack.damage >= coolerStack.maxDamage) coolerStack.decrement(1)
        }

        val isHeatingUp = shouldHeatUp || (machine != null && when (coolerItem) {
            IRItemRegistry.HEAT_COIL -> { // Heat Coil will keep the temperature in about the first 10% of the optimal range, +- 5 degrees
                val targetTemperature = (optimalRange.first + (optimalRange.last - optimalRange.first) * 0.1).toInt() + random.nextInt(10) - 5
                (temperature.toInt() <= targetTemperature && machine.use(16)).also { if (it) consumeCooler() }
            }
            is IRHeatFactorItem -> {
                if (coolerItem.heatFactor < 0 && temperature > 35.0) consumeCooler()
                false
            }
            else -> false
        })

        if (isHeatingUp) {
            temperature = temperature.plus(getHeatingSpeed()).coerceAtLeast(35.0) + random.nextInt(10) - 5
        } else if (temperature > 35.0) {
            temperature -= getCoolingSpeed()
        } else if (ticks % 15 == 0) {
            temperature = (temperature + (2 * random.nextFloat() - 1) / 2).coerceIn(20.0, 35.0)
        }

        if (temperature >= limit) {
            blockEntity.world?.createExplosion(
                null,
                blockEntity.pos.x.toDouble(),
                blockEntity.pos.y.toDouble(),
                blockEntity.pos.z.toDouble(),
                getExplosionPower(),
                false,
                World.ExplosionSourceType.BLOCK
            )
        }
    }
}
