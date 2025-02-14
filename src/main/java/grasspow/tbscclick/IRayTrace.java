package grasspow.tbscclick;

import net.minecraft.client.Minecraft;

/**
 * @author tbsc on 02/09/2021
 */
public interface IRayTrace {

    boolean isBlockTrace();
    boolean isEntityTrace();
    boolean isMissType();

    boolean isLookingAtEntity(Minecraft minecraft);
    void leftClickEntity(Minecraft minecraft);
    boolean isEmptyBlock(Minecraft minecraft);
    void leftClickBlock(Minecraft minecraft);
}
