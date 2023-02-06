package dev.breeze.settlements.utils.sound;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundPreset {

    // Universal volume
    public static final float VOLUME = 0.7f;

    // Commonly used sound effects
    public static final Sound NOTE_BELL = Sound.BLOCK_NOTE_BLOCK_BELL;

    // Melodies (jingles)
    public static final float[] UNUSED_1 = new float[]{1.059463f, 1.059463f, 0.890899f, 1.059463f, 1.414214f};
    public static final float[] UNUSED_2 = new float[]{0.707f, 0.794f, 1.059463f};

    public static final float[] ENABLE = new float[]{0.7f, 1.05f};
    public static final float[] DISABLE = new float[]{1.05f, 0.7f};

    // Notifications
    public static final float[] MENTION = new float[]{1, 1.75f, 1, 1.3f, 1.5f, 2};
    public static final float[] CHAT_MENTION = new float[]{0.7f, 0.7f, 1.05f};


    // Tool augmentation quest
    public static final float[] QUEST_START = new float[]{
            0.595f, 0.707f, 0.595f, 0.891f, 0.841f, 0f, 0f,
            0.707f, 0.841f, 0.707f, 1.059f, 1f, 0f, 0f,
            0.595f, 0.707f, 0.891f, 0.841f, 0.794f, 0f, 0.707f, 0.595f
    };
    public static final float[] QUEST_COMPLETE = new float[]{
            0.595f, 0.667f, 0.595f, 0.891f, 0.749f, 0f, 0f,
            0.667f, 0.749f, 0.667f, 1f, 0.841f, 0f, 0f,
            0.749f, 0.841f, 0.749f, 1.122f, 1f, 0f, 1.122f, 1.414f, 1.498f
    };


    // Shortcuts to specific sound effects
    public static void levelUp(Player p, float pitch) {
        SoundUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, pitch);
    }

    public static void expDing(Player p) {
        SoundUtil.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1);
    }

    public static void chickenEgg(Player p) {
        SoundUtil.playSound(p, Sound.ENTITY_CHICKEN_EGG, 1);
    }

    public static void anvilLand(Player p) {
        SoundUtil.playSound(p, Sound.BLOCK_ANVIL_LAND, 1);
    }

    public static void anvilUse(Player p) {
        SoundUtil.playSound(p, Sound.BLOCK_ANVIL_USE, 1);
    }

    public static void challengeComplete(Player p) {
        SoundUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1);
    }

    public static void meow(Player p, float pitch) {
        SoundUtil.playSound(p, Sound.ENTITY_CAT_AMBIENT, pitch);
    }

}
