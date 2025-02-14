package grasspow.tbscclick.impl;

import com.mojang.logging.LogUtils;
import grasspow.tbscclick.Compat;
import grasspow.tbscclick.IClick;
import grasspow.tbscclick.IKeyBind;
import grasspow.tbscclick.IRayTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.Lazy;
import org.slf4j.Logger;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(TbscClick.MODID)
public class TbscClick implements IClick {
    public static final String MODID = "tbscclick";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static Lazy<KeyMapping> keyToggleRight = Lazy.of(() -> new KeyMapping("key.tbscclick.toggleright", GLFW.GLFW_KEY_G, "key.categories.tbscclick"));
    public static Lazy<KeyMapping> keyToggleLeft = Lazy.of(() -> new KeyMapping("key.tbscclick.toggleleft", GLFW.GLFW_KEY_H, "key.categories.tbscclick"));
    public static Lazy<KeyMapping> keyToggleSmartAttack = Lazy.of(() -> new KeyMapping("key.tbscclick.togglesmartattack", GLFW.GLFW_KEY_V, "key.categories.tbscclick"));
    public static Lazy<KeyMapping> keyToggleHoldRight = Lazy.of(() -> new KeyMapping("key.tbscclick.toggleholdright", GLFW.GLFW_KEY_B, "key.categories.tbscclick"));
    public static Lazy<KeyMapping> keySpeed = Lazy.of(() -> new KeyMapping("key.tbscclick.speed", GLFW.GLFW_KEY_N, "key.categories.tbscclick"));
    public static Lazy<KeyMapping> keyCrouch = Lazy.of(() -> new KeyMapping("key.tbscclick.crouch", GLFW.GLFW_KEY_APOSTROPHE, "key.categories.tbscclick"));
    private IKeyBind myKeyUse;
    private IKeyBind myKeyToggleRight;
    private IKeyBind myKeyToggleLeft;
    private IKeyBind myKeyToggleSmartAttack;
    private IKeyBind myKeyToggleHoldRight;
    private IKeyBind myKeySpeed;
    private IKeyBind myKeyCrouch;

    private static int ticksStepBetweenClicks = Config.DEF_TICKS_STEP;
    private static int maxTicksBetweenClicks = Config.DEF_MAX_TICKS;
    private static int minTicksBetweenClicks = Config.DEF_MIN_TICKS;

    public Minecraft minecraft = null;
    private Compat compat;

    public TbscClick(ModContainer container,IEventBus bus) {
        container.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC);
        bus.addListener(this::onRegisterKeyMappingsEvent);
        bus.addListener(this::onLoad);
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::onInitGuiPre);
        NeoForge.EVENT_BUS.addListener(this::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(this::onRenderGameOverlay);
    }

    @SubscribeEvent
    public void onRegisterKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        compat = new Compat(this);
        minecraft = Minecraft.getInstance();
        processConfig();
        event.register(keyToggleRight.get());
        event.register(keyToggleLeft.get());
        event.register(keyToggleSmartAttack.get());
        event.register(keyToggleHoldRight.get());
        event.register(keySpeed.get());
        event.register(keyCrouch.get());

        myKeyUse = new KeyBind(minecraft.options.keyUse);
        myKeyToggleRight = new KeyBind(keyToggleRight.get());
        myKeyToggleLeft = new KeyBind(keyToggleLeft.get());
        myKeyToggleSmartAttack = new KeyBind(keyToggleSmartAttack.get());
        myKeyToggleHoldRight = new KeyBind(keyToggleHoldRight.get());
        myKeySpeed = new KeyBind(keySpeed.get());
        myKeyCrouch = new KeyBind(keyCrouch.get());
    }

    private static void processConfig() {
        ticksStepBetweenClicks = Config.TICK_STEP.get();
        maxTicksBetweenClicks = Config.MAX_TICKS.get();
        minTicksBetweenClicks = Config.MIN_TICKS.get();
    }

    @SubscribeEvent
    void onLoad(final ModConfigEvent event) {
        processConfig();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Post event) {
        compat.onTick();
    }

    @SubscribeEvent
    public void onInitGuiPre(ScreenEvent.Init.Pre event) {
        compat.onInitGuiPre();
    }

    @SubscribeEvent
    public void onKeyPressed(InputEvent.Key event) {
        compat.onKeyPressed();
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGuiLayerEvent.Post event) {
        if (event.getName() == VanillaGuiLayers.DEBUG_OVERLAY) {
            compat.onRenderGameOverlay();
        }
    }

    @Override
    public Minecraft getMinecraft() {
        return minecraft;
    }

    @Override
    public boolean isGamePaused() {
        return minecraft.isPaused();
    }

    @Override
    public boolean isInGame() {
        return minecraft.screen == null;
    }

    @Override
    public boolean isInPauseMenu() {
        return minecraft.screen instanceof PauseScreen;
    }

    @Override
    public IRayTrace getRayTrace() {
        return new RayTrace(minecraft.hitResult);
    }

    @Override
    public float getSmartAttackCooldown() {
        return minecraft.player != null ? minecraft.player.getAttackStrengthScale(0) : 0.0F;
    }

    InputEvent.InteractionKeyMappingTriggered clickInputEvent;

    @Override
    public void postClickInputEvent() {
        clickInputEvent = ClientHooks.onClickInput(0, minecraft.options.keyAttack, InteractionHand.MAIN_HAND);
    }

    @Override
    public void swingHandIfShould() {
        if (clickInputEvent.shouldSwingHand() && minecraft.player != null) {
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void setHoldButton(IKeyBind key, boolean held) {
        key.setHeld(held);
    }

    @Override
    public IKeyBind getUseKey() {
        return myKeyUse;
    }

    @Override
    public IKeyBind getToggleRightKey() {
        return myKeyToggleRight;
    }

    @Override
    public IKeyBind getToggleLeftKey() {
        return myKeyToggleLeft;
    }

    @Override
    public IKeyBind getToggleSmartAttackKey() {
        return myKeyToggleSmartAttack;
    }

    @Override
    public IKeyBind getToggleHoldRightKey() {
        return myKeyToggleHoldRight;
    }

    @Override
    public IKeyBind getSpeedKey() {
        return myKeySpeed;
    }

    @Override
    public IKeyBind getCrouchKey() {
        return myKeyCrouch;
    }

    @Override
    public int getTicksStepBetweenClicks() {
        return ticksStepBetweenClicks;
    }

    @Override
    public int getMaxTicksBetweenClicks() {
        return maxTicksBetweenClicks;
    }

    @Override
    public int getMinTicksBetweenClicks() {
        return minTicksBetweenClicks;
    }

    @Override
    public String getRightClickMouseMethodMapping() {
        return "m_91277_";
    }

    @Override
    public String getLeftClickCounterFieldMapping() {
        return "f_91078_";
    }

    @Override
    public String getLeftClickMouseMethodMapping() {
        return "m_202354_";
    }

    @Override
    public String getRightClickDelayTimerFieldMapping() {
        return "f_91011_";
    }

    @Override
    public void renderTextOnScreen(String text, float x, float y, int color) {
        PoseStack poseStack = new PoseStack();
        minecraft.font.drawInBatch(
                Component.literal(ChatFormatting.BOLD + text),
                x,
                y,
                color,
                false,
                poseStack.last().pose(),
                minecraft.renderBuffers().bufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                15728880
        );
    }

    @Override
    public boolean isPlayerHandBusy() {
        return minecraft.player != null && minecraft.player.isHandsBusy();
    }

    @Override
    public void sendMessage(String message) {
        minecraft.gui.getChat().addMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    private static final byte[] signatureBytes = new byte[]{
            (byte) 0xad, 0x54, (byte) 0xba, 0x5b, 0x71, 0x35, 0x0d, (byte) 0x90,
            (byte) 0xcb, 0x58, 0x06, (byte) 0x8f, (byte) 0xac, 0x1a, 0x40, (byte) 0xe8,
            (byte) 0xe8, (byte) 0xc8, (byte) 0xd8, (byte) 0x8c, (byte) 0xb7, 0x14, (byte) 0x8c, (byte) 0xa2,
            (byte) 0xd0, (byte) 0xf2, 0x14, 0x11, 0x43, (byte) 0xa0, (byte) 0xf2, 0x69,
            (byte) 0xe4, 0x14, 0x31, 0x46, (byte) 0xcd, (byte) 0x80, 0x28, 0x39,
            (byte) 0xe1, 0x33, 0x2a, 0x66, (byte) 0xab, (byte) 0xf5, 0x7e, 0x5d,
            0x7e, (byte) 0xd0, 0x04, 0x0e, 0x20, 0x4a, 0x7d, (byte) 0xc1,
            0x32, (byte) 0xe2, (byte) 0xde, (byte) 0x80, 0x6f, (byte) 0x85, 0x18, 0x68,
            (byte) 0x89, 0x5c, 0x27, 0x4b, (byte) 0x88, (byte) 0xc6, 0x5b, (byte) 0xf4,
            0x6e, 0x05, 0x0a, 0x3b, (byte) 0xba, 0x50, (byte) 0xd9, (byte) 0xc8,
            0x5e, 0x1c, (byte) 0x8c, (byte) 0xc7, 0x28, (byte) 0xff, (byte) 0xcb, (byte) 0xfc,
            (byte) 0xcd, (byte) 0xc6, (byte) 0xcb, 0x15, 0x65, (byte) 0x9e, (byte) 0xac, 0x0d,
            0x44, (byte) 0xba, (byte) 0xda, (byte) 0xa9, (byte) 0xb3, 0x59, 0x12, (byte) 0xc9,
            0x50, 0x7a, 0x0f, 0x13, (byte) 0xb1, (byte) 0xd5, (byte) 0xd2, (byte) 0xdd,
            (byte) 0x98, 0x11, 0x55, 0x52, 0x7c, (byte) 0xaf, (byte) 0xf1, 0x5e,
            (byte) 0xdf, 0x5a, (byte) 0xbd, (byte) 0x9a, 0x48, 0x36, (byte) 0x8e, 0x4e,
            0x06, (byte) 0xec, 0x66, (byte) 0xba, 0x20, (byte) 0xa5, (byte) 0xe4, (byte) 0xf6,
            0x21, (byte) 0x90, 0x43, (byte) 0xa2, (byte) 0xe1, 0x46, (byte) 0xd4, 0x7a,
            (byte) 0x9e, (byte) 0xc2, 0x09, 0x6b, (byte) 0x83, 0x12, 0x79, 0x67,
            (byte) 0xe1, (byte) 0x96, 0x03, (byte) 0x85, (byte) 0x9e, (byte) 0xfc, (byte) 0xbb, (byte) 0xd7,
            0x42, 0x40, 0x0b, (byte) 0xcc, (byte) 0xc2, 0x44, 0x2e, (byte) 0xfd,
            (byte) 0x80, (byte) 0xfb, (byte) 0xbe, 0x37, (byte) 0xb3, 0x3d, 0x17, (byte) 0x8c,
            (byte) 0xeb, 0x1d, 0x5f, (byte) 0x8c, 0x4a, (byte) 0xfe, (byte) 0x8b, 0x39,
            0x46, 0x72, 0x2d, (byte) 0xd4, (byte) 0xd1, (byte) 0xd0, 0x4e, (byte) 0xda,
            0x35, (byte) 0xd9, (byte) 0x97, (byte) 0xa0, 0x15, 0x58, (byte) 0xe4, (byte) 0xe3,
            (byte) 0xa6, (byte) 0xb7, 0x4f, 0x18, 0x6e, (byte) 0xf4, (byte) 0xde, 0x58,
            0x7d, (byte) 0xa1, (byte) 0xe2, 0x1c, 0x61, 0x55, (byte) 0xed, (byte) 0xd2,
            0x67, (byte) 0x84, 0x72, (byte) 0xd0, 0x79, (byte) 0xfe, 0x33, (byte) 0x92,
            0x4d, 0x24, (byte) 0xb8, (byte) 0x98, (byte) 0xcc, 0x09, (byte) 0xc2, (byte) 0xee,
            (byte) 0xa0, (byte) 0xcb, (byte) 0xa0, (byte) 0xe4, 0x33, (byte) 0xdd, 0x03, 0x2b,
            0x7d, 0x6e, (byte) 0x83, 0x22, 0x2e, 0x7d, (byte) 0xf2, 0x23,
            (byte) 0xdb, 0x66, 0x1d, (byte) 0xf4, (byte) 0xf6, (byte) 0xe6, (byte) 0xe1, (byte) 0xf1
    };

    @Override
    public void sendMessageWithId(String message) {
        minecraft.gui.getChat().addMessage(Component.literal(message), new MessageSignature(signatureBytes), GuiMessageTag.system());
    }
}
