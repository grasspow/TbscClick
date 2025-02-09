package tbsc.clickmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import tbsc.clickmod.impl.TbscClick;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Because this mod supports multiple versions of Minecraft at the same time, this support class will contain as much
 * general code as possible, and the main class will have all version-dependent code.
 *
 * @author tbsc on 02/09/2021
 */
public class Compat {

    private final TbscClick mod;

    public static boolean shouldLeftClick = false;
    public static boolean shouldSmartAttack = false;
    public static boolean shouldRightClick = false;
    public static boolean shouldCrouch = false;
    public static boolean holdingRightButton = false;
    public static int clickTickInterval = 1;
    boolean holdRightWasPressed = false;
    private int leftCooldown = 0;
    private int rightCooldown = 0;
    private int crouchCooldown = 0;

    public Compat(TbscClick mod) {
        this.mod = mod;
    }

    public void onTick() {
        if (!mod.isGamePaused()) {
            /* AUTO LEFT CLICK */

            if (shouldLeftClick) {
                if (leftCooldown == 0) {
                    leftClick(false);
                    leftCooldown = clickTickInterval - 1;
                } else {
                    leftCooldown--;
                }
            }

            /* SMART ATTACK */

            if (shouldSmartAttack) {
                leftClick(true);
            }

            /* AUTO RIGHT CLICK */

            if (shouldRightClick) {
                if (!mod.isInGame() && !mod.isInPauseMenu()) {
                    // Right clicking has probably caused a GUI to open, stop auto clicking
                    mod.sendMessage("Auto right clicking has stopped because a GUI opened");
                    shouldRightClick = false;
                } else if (rightCooldown == 0) {
                    mcReflRightClick();
                    rightCooldown = clickTickInterval - 1;
                } else {
                    rightCooldown--;
                }
            }

            /* HOLD RIGHT CLICK */

            // When in another screen, key should be held and right click cooldown is over, right click
            if (holdingRightButton && !mod.isInGame() && mcReflRightClickDelayTimer() == 0 && !mod.isPlayerHandBusy()) {
                mcReflRightClick();
            }

            if (holdRightWasPressed && !mod.getToggleHoldRightKey().isDown()) {
                holdingRightButton = !holdingRightButton;
                mod.setHoldButton(mod.getUseKey(), holdingRightButton);
                holdRightWasPressed = false;
            }

            /* Hold Crouch*/
            if (shouldCrouch) {
                if (crouchCooldown == 0) {
                    crouch();
                    crouchCooldown = clickTickInterval;
                } else {
                    crouchCooldown--;
                }
            }
        }
    }

    public void onKeyPressed() {
        if (mod.getToggleLeftKey().isDown()) {
            disableSmartAttackByConflict();
            shouldLeftClick = !shouldLeftClick;
        }
        if (mod.getToggleSmartAttackKey().isDown()) {
            disableAutoLeftByConflict();
            shouldSmartAttack = !shouldSmartAttack;
        }
        if (mod.getToggleRightKey().isDown()) {
            disableHoldRightByConflict();
            shouldRightClick = !shouldRightClick;
        }
        if (mod.getToggleHoldRightKey().isDown()) {
            disableAutoRightByConflict();
            holdRightWasPressed = true;
        }

        if (mod.getSpeedKey().isDown()) {
            int chatId = 8327; // magic number
            String plural = "s";
            if ((clickTickInterval += mod.getTicksStepBetweenClicks()) > mod.getMaxTicksBetweenClicks()) {
                plural = "";
                clickTickInterval = mod.getMinTicksBetweenClicks();
            }
            mod.sendMessageWithId("New auto click interval: every " + clickTickInterval + " tick" + plural, chatId);
        }

        if (mod.getCrouchKey().isDown()) {
            shouldCrouch = !shouldCrouch;
        }
    }

    /**
     * @param smart If true, waits until the attack cooldown is over before clicking again, despite being called.
     */
    public void leftClick(boolean smart) {
        Minecraft minecraft = mod.getMinecraft();
        IRayTrace rayTrace = mod.getRayTrace();
        mod.postClickInputEvent();
        if (!smart || mod.getSmartAttackCooldown() == 1.0F) {
            if (rayTrace.isBlockTrace() && !rayTrace.isMissType() && !rayTrace.isEmptyBlock(minecraft)) {
                rayTrace.leftClickBlock(minecraft);
            } else if (rayTrace.isEntityTrace() && rayTrace.isLookingAtEntity(minecraft)) {
                rayTrace.leftClickEntity(minecraft);
            }
            mod.swingHandIfShould();
        }
    }

    /**
     * Calls Minecraft.startUseItem using reflection.
     */
    public void mcReflRightClick() {
        mcReflInvokeMethod(mod.getRightClickMouseMethodMapping());
    }

    private Field leftClickCounterField = null;

    private void mcReflSetLeftClickCounter(int leftClickCounter) {
        try {
            if (leftClickCounterField == null) {
                leftClickCounterField = ObfuscationReflectionHelper.findField(Minecraft.class, mod.getLeftClickCounterFieldMapping());
            }
            leftClickCounterField.setAccessible(true);
            leftClickCounterField.set(mod.getMinecraft(), leftClickCounter);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    private void mcReflLeftClick(int leftClickCounter) {
        mcReflSetLeftClickCounter(leftClickCounter);
        mcReflInvokeMethod(mod.getLeftClickMouseMethodMapping());
    }

    private Field rightClickDelayTimerField = null;

    private int mcReflRightClickDelayTimer() {
        try {
            if (rightClickDelayTimerField == null) {
                rightClickDelayTimerField = ObfuscationReflectionHelper.findField(Minecraft.class, mod.getRightClickDelayTimerFieldMapping());
            }
            rightClickDelayTimerField.setAccessible(true);
            return (int) rightClickDelayTimerField.get(mod.getMinecraft());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void mcReflInvokeMethod(String name) {
        mcReflInvokeMethod(name, new Class[0], new Object[0]);
    }

    private void mcReflInvokeMethod(String name, Class<?>[] types, Object[] args) {
        reflInvokeMethod(Minecraft.class, mod.getMinecraft(), name, types, args);
    }

    private <T> void reflInvokeMethod(Class<T> clazz, T instance, String name) {
        reflInvokeMethod(clazz, instance, name, new Class[0], new Object[0]);
    }

    private final Map<String, Method> methods = new HashMap<>();

    public <T> void reflInvokeMethod(Class<T> clazz, T instance, String name, Class<?>[] types, Object[] args) {
        try {
            if (methods.get(clazz.getName() + name) == null) {
                methods.put(clazz.getName() + name, ObfuscationReflectionHelper.findMethod(clazz, name, types));
            }
            Method method = methods.get(clazz.getName() + name);
            method.setAccessible(true);
            method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static boolean isSneaking = false;

    private void crouch() {
        isSneaking = !isSneaking;
        Minecraft mc = mod.getMinecraft();
        LocalPlayer player = mc.player;
        if (mc.getConnection() != null && player != null) {
            player.setShiftKeyDown(isSneaking);
            ServerboundPlayerCommandPacket.Action action = isSneaking
                    ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY
                    : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;

            mc.getConnection().send(new ServerboundPlayerCommandPacket(player, action));
        }
    }

    private void disableAutoLeftByConflict() {
        if (shouldLeftClick) {
            shouldLeftClick = false;
            mod.sendMessage("Conflict: Disabled auto click left button.");
        }
    }

    private void disableSmartAttackByConflict() {
        if (shouldSmartAttack) {
            shouldSmartAttack = false;
            mod.sendMessage("Conflict: Disabled auto smart attack.");
        }
    }

    private void disableHoldRightByConflict() {
        if (holdingRightButton) {
            holdRightWasPressed = true;
            mod.sendMessage("Conflict: Disabled holding right button.");
        }
    }

    private void disableAutoRightByConflict() {
        if (shouldRightClick) {
            shouldRightClick = false;
            mod.sendMessage("Conflict: Disabled auto clicking right button.");
        }
    }

    public void onRenderGameOverlay() {
        List<String> renderList = new ArrayList<>();

        if (shouldLeftClick) {
            renderList.add("Auto Left Clicking");
        }
        if (shouldRightClick) {
            renderList.add("Auto Right Clicking");
        }
        if (shouldSmartAttack) {
            renderList.add("Auto Smart Attacking");
        }
        if (holdingRightButton) {
            renderList.add("Holding Right Button");
        }
        if (shouldCrouch) {
            renderList.add("Auto Crouching");
        }

        for (int i = 0; i < renderList.size(); ++i) {
            String renderString = renderList.get(i);
            mod.renderTextOnScreen(renderString, 6, 6 + 10 * i, 0xFF0000);
        }
    }

    public void onInitGuiPre() {
        // Triggered when opening any screen that isn't the game
        // Opening a screen clears all pressed keybinds, so make it pressed again
        // This will persist when exiting the screen
        if (holdingRightButton) {
            mod.setHoldButton(mod.getUseKey(), true);
        }
    }
}
