package dev.breeze.settlements.utils.sound;

import dev.breeze.settlements.Main;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


public class SoundUtil {

    public static void playSound(Player player, Sound sound, float pitch) {
        playSound(player, sound, SoundPreset.VOLUME, pitch);
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, SoundPreset.VOLUME, pitch);
    }

    public static void playSoundPublic(Location loc, Sound sound, float pitch) {
        playSoundPublic(loc, sound, SoundPreset.VOLUME, pitch);
    }

    public static void playSoundPublic(Location loc, Sound sound, float volume, float pitch) {
        loc.getWorld().playSound(loc, sound, SoundPreset.VOLUME, pitch);
    }

    public static void playNotes(float[] notes, Sound sound, Player p, int interval) {
        new BukkitRunnable() {
            int count = 0;

            public void run() {
                if (count >= notes.length) {
                    this.cancel();
                    return;
                }
                float tone = notes[count];
                if (tone != 0)
                    p.playSound(p.getLocation(), sound, SoundPreset.VOLUME, tone);
                count++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, interval);
    }

    public static void playNotesPublic(float[] notes, Sound sound, Location loc, int interval) {
        new BukkitRunnable() {
            int count = 0;

            public void run() {
                if (count >= notes.length) {
                    this.cancel();
                    return;
                }
                float tone = notes[count];
                if (tone == 0)
                    return;
                loc.getWorld().playSound(loc, sound, SoundPreset.VOLUME, tone);
                count++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, interval);
    }


}

