package me.steven.indrev.blocks

import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.gui.IRScreenHandlerFactory
import me.steven.indrev.items.IRMachineUpgradeItem
import me.steven.indrev.utils.Tier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.particle.ParticleTypes
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import java.util.*

open class MachineBlock(
    settings: Settings,
    val tier: Tier,
    private val screenHandler: ((Int, PlayerInventory, ScreenHandlerContext) -> ScreenHandler)?,
    private val blockEntityProvider: () -> MachineBlockEntity
) : Block(settings), BlockEntityProvider, InventoryProvider {

    init {
        if (this.defaultState.contains(WORKING_PROPERTY))
            this.defaultState = stateManager.defaultState.with(WORKING_PROPERTY, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        super.appendProperties(builder)
        builder?.add(WORKING_PROPERTY)
    }

    override fun getPlacementState(ctx: ItemPlacementContext?): BlockState? {
        return defaultState.with(WORKING_PROPERTY, false)
    }

    override fun createBlockEntity(view: BlockView?): BlockEntity? = blockEntityProvider()

    override fun buildTooltip(
        stack: ItemStack?,
        view: BlockView?,
        tooltip: MutableList<Text>?,
        options: TooltipContext?
    ) {
        tooltip?.add(TranslatableText("block.machines.tooltip.io", LiteralText("${tier.io} LF/tick").formatted(Formatting.WHITE)).formatted(Formatting.BLUE))
    }

    override fun onUse(
        state: BlockState?,
        world: World,
        pos: BlockPos?,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult? {
        //if (world.isClient) return ActionResult.SUCCESS
        val blockEntity = world.getBlockEntity(pos)
        val stack = player?.getStackInHand(hand)
        val upgrade = stack?.item
        if (upgrade is IRMachineUpgradeItem && !world.isClient) {
            val block = state?.block
            if (block is MachineBlock && block.tier == upgrade.from) {
                if (blockEntity !is MachineBlockEntity || !blockEntity.registry.upgradeable) return ActionResult.PASS
                var upgradedBlock = blockEntity.registry.block(upgrade.to).defaultState
                if (state.contains(VerticalFacingMachineBlock.FACING))
                    upgradedBlock = upgradedBlock.with(VerticalFacingMachineBlock.FACING, state[VerticalFacingMachineBlock.FACING])
                else if (state.contains(FacingMachineBlock.HORIZONTAL_FACING))
                    upgradedBlock = upgradedBlock.with(FacingMachineBlock.HORIZONTAL_FACING, state[FacingMachineBlock.HORIZONTAL_FACING])
                world.setBlockState(pos, upgradedBlock)
                val upgradedBlockEntity = world.getBlockEntity(pos)
                if (upgradedBlockEntity is MachineBlockEntity) {
                    upgradedBlockEntity.energy = blockEntity.energy
                    upgradedBlockEntity.inventoryController?.fromTag(blockEntity.inventoryController?.toTag(CompoundTag()))
                    upgradedBlockEntity.temperatureController?.fromTag(blockEntity.temperatureController?.toTag(CompoundTag()))
                }
                stack.decrement(1)
                return ActionResult.CONSUME
            }
        } else if (screenHandler != null
            && blockEntity is MachineBlockEntity
            && blockEntity.inventoryController != null) {
            player?.openHandledScreen(IRScreenHandlerFactory(screenHandler, pos!!))?.ifPresent { syncId ->
                blockEntity.viewers[player.uuid] = syncId
            }
        }
        return ActionResult.SUCCESS
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos?, newState: BlockState, moved: Boolean) {
        if (!state.isOf(newState.block)) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is MachineBlockEntity && blockEntity.inventoryController != null) {
                ItemScatterer.spawn(world, pos, blockEntity.inventoryController!!.getInventory())
                world.updateComparators(pos, this)
            }
            super.onStateReplaced(state, world, pos, newState, moved)
        }
    }

    override fun getInventory(state: BlockState?, world: WorldAccess?, pos: BlockPos?): SidedInventory {
        val blockEntity = world?.getBlockEntity(pos)
        if (blockEntity !is InventoryProvider) throw IllegalArgumentException("tried to retrieve an inventory from an invalid block entity")
        return blockEntity.getInventory(state, world, pos)
    }

    @Environment(EnvType.CLIENT)
    override fun randomDisplayTick(state: BlockState?, world: World, pos: BlockPos, random: Random?) {
        if (state?.contains(WORKING_PROPERTY) == true && state[WORKING_PROPERTY]) {
            val d = pos.x.toDouble() + 0.5
            val e = pos.y.toDouble() + 1.0
            val f = pos.z.toDouble() + 0.5
            world.addParticle(ParticleTypes.SMOKE, d, e, f, 0.0, 0.0, 0.0)
        }
    }

    companion object {
        val WORKING_PROPERTY: BooleanProperty = BooleanProperty.of("working")
    }
}