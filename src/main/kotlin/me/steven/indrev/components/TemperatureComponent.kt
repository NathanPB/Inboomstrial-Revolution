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
    val limit: Int,
    private val explodes: Boolean
) {

    var temperature: Double = 25.0
    var isKillswitchActivated = false

    init {
        blockEntity.trackInt(MachineBlockEntity.TEMPERATURE_ID) { temperature.toInt() }
        blockEntity.trackInt(MachineBlockEntity.MAX_TEMPERATURE_ID) { limit }
    }

    private var ticks = 0

    fun readNbt(tag: NbtCompound?) {
        temperature = tag?.getDouble("Temperature") ?: 0.0
        isKillswitchActivated = tag?.getBoolean("KillswitchActivated") ?: false
    }

    fun writeNbt(tag: NbtCompound): NbtCompound {
        tag.putDouble("Temperature", temperature)
        tag.putBoolean("KillswitchActivated", isKillswitchActivated)
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
        val baseCoolingSpeed = this.baseHeatingSpeed / 1.5
        val machine = blockEntity as? MachineBlockEntity<*> ?: return baseCoolingSpeed
        val coolerItem = (machine.inventoryComponent?.inventory?.coolerStack?.item as? IRHeatFactorItem) ?: return baseCoolingSpeed
        return (coolerItem.heatFactor * -1).coerceAtLeast(baseCoolingSpeed)
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

        var forceHeatUp = false
        var shouldConsumeCooler = false

        if (machine != null) {
             when (coolerItem) {
                IRItemRegistry.HEAT_COIL -> { // Heat Coil will keep the temperature in about the first 10% of the optimal range, +- 5 degrees
                    if (!isKillswitchActivated) {
                        val targetTemperature = (optimalRange.first + (optimalRange.last - optimalRange.first) * 0.1).toInt() + random.nextInt(10) - 5
                        val heaterActivated = (temperature.toInt() <= targetTemperature && machine.use(16))
                        forceHeatUp = heaterActivated
                        shouldConsumeCooler = heaterActivated
                    }
                }
                is IRHeatFactorItem -> {
                    val coolerActivated = coolerItem.heatFactor < 0 && temperature > 35.0
                    shouldConsumeCooler = coolerActivated
                }
            }
        }

        if (shouldConsumeCooler) {
            if (coolerStack.isDamageable && ticks % 120 == 0) coolerStack.damage(1, random, null)
            if (coolerStack.damage >= coolerStack.maxDamage) coolerStack.decrement(1)
        }

        if (shouldHeatUp || forceHeatUp) {
            temperature = temperature
                .plus(getHeatingSpeed())
                .coerceAtLeast(35.0)
                .plus(random.nextInt(10) - 5)
                .coerceAtMost(limit.toDouble())
        } else if (temperature > 35.0) {
            temperature -= getCoolingSpeed()
        } else if (ticks % 15 == 0) {
            isKillswitchActivated = false
            temperature = (temperature + (2 * random.nextFloat() - 1) / 2).coerceIn(20.0, 35.0)
        }

        // Killswitch is triggered when the temperature is within 25% of the difference between the limit and the upper bound of the optimal range.
        if (
            !isKillswitchActivated
            && machine != null
            && temperature >= limit - (limit - optimalRange.last) * 0.25
            && machine.enhancerComponent?.enhancers?.contains(Enhancer.KILLSWITCH) == true
        ) {
            isKillswitchActivated = true
        }

        if (explodes && temperature >= limit) {
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
