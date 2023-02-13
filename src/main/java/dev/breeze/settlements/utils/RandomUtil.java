package dev.breeze.settlements.utils;

import org.bukkit.Location;

import java.util.List;
import java.util.Random;

public class RandomUtil {

    public static final Random RANDOM = new Random();

    public static Location addRandomOffset(Location location) {
        return addRandomOffset(location, 1, 1, 1);
    }

    public static Location addRandomOffset(Location location, double dx, double dy, double dz) {
        return location.clone().add(RANDOM.nextDouble() * dx - dx / 2,
                RANDOM.nextDouble() * dy - dy / 2,
                RANDOM.nextDouble() * dz - dz / 2);
    }

    public static String randomString() {
        return String.valueOf(RANDOM.nextDouble() * 100);
    }

    public static <T> T choice(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    public static <T> T choice(T[] list) {
        return list[RANDOM.nextInt(list.length)];
    }

}
