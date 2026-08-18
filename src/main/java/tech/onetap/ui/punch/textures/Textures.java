package tech.onetap.ui.punch.textures;

import net.minecraft.util.Identifier;

public final class Textures {
    private Textures() {
    }

    private static Identifier img(String name) {
        return Identifier.of("mre", "images/" + name + ".png");
    }

    public static final class Header {
        public static final Identifier SEARCH = img("lupa_fallback");
        public static final Identifier CHEVRON_LEFT = img("arrow3");
        public static final Identifier CHEVRON_RIGHT = img("arrow3");
        public static final Identifier SETTINGS = img("spark_1");
        public static final Identifier FRIENDS = img("heart");
        public static final Identifier PROFILE_ADD = img("star");
        public static final Identifier DEV_AVATAR = img("devka");
    }

    public static final class Icons {
        public static final Identifier BOXES = img("map");
        public static final Identifier CHEVRON_DOWN = img("arrow3");
        public static final Identifier CHEVRONS_LEFT_RIGHT = img("arrow3");
        public static final Identifier CIRCLE_PLUS = img("star");
        public static final Identifier COMMAND = img("target");
        public static final Identifier DELETE = img("skull_state_1");
        public static final Identifier DELETE_LEFT = img("skull_state_0");
        public static final Identifier DICES = img("snowflake");
        public static final Identifier EYE = img("target");
        public static final Identifier GAMEPAD = img("dollar");
        public static final Identifier HARD_DRIVE = img("circle");
        public static final Identifier KEYBOARD = img("circle");
        public static final Identifier PERSON_STANDING = img("crest1");
        public static final Identifier PIN = img("triangle");
        public static final Identifier PLUS = img("star");
        public static final Identifier REFRESH_CCW = img("spark_1");
        public static final Identifier SPARKLES = img("spark_2");
        public static final Identifier SWORDS = img("arrow3");
        public static final Identifier TRIANGLE_ALERT = img("triangle");
        public static final Identifier USER_ROUND = img("crest1");
        public static final Identifier USER_ROUND_PLUS = img("star");
    }
}