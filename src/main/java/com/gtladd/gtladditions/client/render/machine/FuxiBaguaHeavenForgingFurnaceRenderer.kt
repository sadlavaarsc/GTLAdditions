package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.RotationParams
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.vertex.PoseStack
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.utils.RenderUtil
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.math.sin

class FuxiBaguaHeavenForgingFurnaceRenderer(
    baseCasing: ResourceLocation,
    workableModel: ResourceLocation,
    partEntry: BlockEntry<Block>,
    partCasing: ResourceLocation
) : PartWorkableCasingMachineRenderer(baseCasing, workableModel, partEntry, partCasing) {

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        if (blockEntity is IMachineBlockEntity) {
            val machine = blockEntity.metaMachine as? GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine ?: return
            
            if (machine.recipeLogic.isWorking) {
                val tick = RenderUtil.getSmoothTick(machine, partialTicks)
                val starPos = getRotatedRenderPosition(Direction.EAST, machine.frontFacing, -81.0, 0.0, 0.0)
                val seed = blockEntity.blockPos.asLong()
                
                renderStar(tick, poseStack, buffer, seed, starPos.x, starPos.y, starPos.z)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        registry.accept(STAR_LAYER_0)
        registry.accept(STAR_LAYER_2)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384

    @OnlyIn(Dist.CLIENT)
    private fun renderStar(
        tick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        randomSeed: Long,
        x: Double,
        y: Double,
        z: Double
    ) {
        poseStack.pushPose()
        poseStack.translate(x, y, z)

        // 应用心跳效果
        val heartbeatScale = calculateHeartbeatScale(tick)
        poseStack.scale(heartbeatScale, heartbeatScale, heartbeatScale)

        // 调整后的半径
        val baseRadius = 0.15f
        val middleRadius = baseRadius + 0.003f
        val outerRadius = middleRadius * 1.02f

        val cache = getOrCreateCache(randomSeed)
        val rotationSpeedMultiplier = 1.0f

        // 使用固定的白色，移除变色功能
        val starColor = 0xFFFFFFFF.toInt()

        RenderUtils.renderStarLayer(
            poseStack, buffer, STAR_LAYER_2, middleRadius,
            cache.rotation2.axis, cache.rotation1.getAngle(tick * rotationSpeedMultiplier),
            starColor, RenderType.translucent()
        )

        RenderUtils.renderStarLayer(
            poseStack, buffer, STAR_LAYER_0, baseRadius,
            cache.rotation1.axis, cache.rotation0.getAngle(tick * rotationSpeedMultiplier),
            starColor, RenderType.solid()
        )

        RenderUtils.renderHaloLayer(
            poseStack, buffer, outerRadius,
            cache.rotation0.axis, cache.rotation0.getAngle(tick * rotationSpeedMultiplier),
            HALO_TEX, STAR_LAYER_2,
            1.0f, true
        )

        poseStack.popPose()
    }

    @OnlyIn(Dist.CLIENT)
    private data class RenderCache(val seed: Long) {
        val rotation0: RotationParams
        val rotation1: RotationParams
        val rotation2: RotationParams

        init {
            val random = RandomSource.create(seed)
            rotation0 = RenderUtils.createRandomRotation(random, 2.0f, 3.0f)
            rotation1 = RenderUtils.createRandomRotation(random, 0.9f, 1.5f)
            rotation2 = RenderUtils.createRandomRotation(random, 0.9f, 1.5f)
        }
    }

    @OnlyIn(Dist.CLIENT)
    companion object {
        private val STAR_LAYER_0 = GTLAdditions.id("obj/star_layer_0")
        private val STAR_LAYER_2 = GTLAdditions.id("obj/star_layer_2")
        private val HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex2.png")
        private val CACHE_MAP = ConcurrentHashMap<Long, RenderCache>()
        
        // 心跳效果参数
        private const val HEARTBEAT_FREQUENCY = 0.14f
        private const val HEARTBEAT_AMPLITUDE = 0.22f

        private fun getOrCreateCache(seed: Long): RenderCache = CACHE_MAP.computeIfAbsent(seed) { RenderCache(it) }
        
        private fun calculateHeartbeatScale(tick: Float): Float {
            val phase = sin(tick * HEARTBEAT_FREQUENCY)
            val pulse = phase * phase
            return 1.0f - pulse * HEARTBEAT_AMPLITUDE
        }
    }
}
