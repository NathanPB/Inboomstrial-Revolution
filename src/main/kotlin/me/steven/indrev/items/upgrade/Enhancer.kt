package me.steven.indrev.items.upgrade

import me.steven.indrev.config.IRConfig

enum class Enhancer {
    SPEED, BUFFER, BLAST_FURNACE, SMOKER, DAMAGE, KILLSWITCH;

    companion object {
        val DEFAULT = arrayOf(SPEED, BUFFER, KILLSWITCH)
        val FURNACE = arrayOf(SPEED, BUFFER, KILLSWITCH, BLAST_FURNACE, SMOKER)

        fun getDamageMultiplier(enhancers: Map<Enhancer, Int>): Double {
            return (IRConfig.upgrades.damageUpgradeModifier * (enhancers[DAMAGE] ?: 0).toDouble()).coerceAtLeast(1.0)
        }
    }
}
