package grasspow.tbscclick.impl;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final Config CONFIG;
    public static ModConfigSpec CONFIG_SPEC;

    public static final String KEY_TICKS_STEP = "ticksStep";
    public static final String COMMENT_TICKS_STEP = "When changing auto click interval, by how much should it change";
    public static final int DEF_TICKS_STEP = 1;
    public static ModConfigSpec.IntValue TICK_STEP;

    public static final String KEY_MAX_TICKS = "maxTicksBetweenClicks";
    public static final String COMMENT_MAX_TICKS = "The maximal auto click interval possible before looping back to the minimum";
    public static final int DEF_MAX_TICKS = 10;
    public static ModConfigSpec.IntValue MAX_TICKS;

    public static final String KEY_MIN_TICKS = "minTicksBetweenClicks";
    public static final String COMMENT_MIN_TICKS = "The minimal auto click interval";
    public static final int DEF_MIN_TICKS = 1;
    public static ModConfigSpec.IntValue MIN_TICKS;

    private Config(ModConfigSpec.Builder builder) {
        TICK_STEP = builder.comment(COMMENT_TICKS_STEP)
                .defineInRange(KEY_TICKS_STEP, DEF_TICKS_STEP, 1, 9999999);

        MAX_TICKS = builder.comment(COMMENT_MAX_TICKS)
                .defineInRange(KEY_MAX_TICKS, DEF_MAX_TICKS, 1, 9999999);

        MIN_TICKS = builder.comment(COMMENT_MIN_TICKS)
                .defineInRange(KEY_MIN_TICKS, DEF_MIN_TICKS, 1, 9999999);
    }

    static {
        Pair<Config, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(Config::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

}
