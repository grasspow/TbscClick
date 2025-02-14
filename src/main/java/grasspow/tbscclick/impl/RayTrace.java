package grasspow.tbscclick.impl;

import grasspow.tbscclick.IRayTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * @author tbsc on 02/09/2021
 */
public class RayTrace implements IRayTrace {

    private final HitResult trace;

    public RayTrace(HitResult trace) {
        this.trace = trace;
    }

    @Override
    public boolean isBlockTrace() {
        return trace instanceof BlockHitResult;
    }

    private BlockHitResult block() {
        return (BlockHitResult) trace;
    }

    @Override
    public boolean isEntityTrace() {
        return trace instanceof EntityHitResult;
    }

    @Override
    public boolean isMissType() {
        return trace.getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean isLookingAtEntity(Minecraft minecraft) {
        return minecraft.crosshairPickEntity != null;
    }

    @Override
    public void leftClickEntity(Minecraft minecraft) {
        if (minecraft.gameMode != null && minecraft.player != null && minecraft.crosshairPickEntity != null) {
            minecraft.gameMode.attack(minecraft.player, minecraft.crosshairPickEntity);
        }
    }

    @Override
    public boolean isEmptyBlock(Minecraft minecraft) {
        return minecraft.level != null && minecraft.level.isEmptyBlock(block().getBlockPos());
    }

    @Override
    public void leftClickBlock(Minecraft minecraft) {
        if (minecraft.gameMode != null) {
            BlockHitResult trace = (BlockHitResult) this.trace;
            minecraft.gameMode.startDestroyBlock(trace.getBlockPos(), trace.getDirection());
        }
    }
}
