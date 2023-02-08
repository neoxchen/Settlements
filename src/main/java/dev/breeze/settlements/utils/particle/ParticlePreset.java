package dev.breeze.settlements.utils.particle;

import dev.breeze.settlements.Main;
import dev.breeze.settlements.utils.LocationUtil;
import dev.breeze.settlements.utils.SafeRunnable;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParticlePreset {

    public static void displayLine(Location from, Location to, int sampleCount, Particle particle, int count, double dx, double dy, double dz, double speed) {
        List<Location> line = LocationUtil.getLine(from, to, sampleCount);
        ParticleUtil.globalParticle(line, particle, count, dx, dy, dz, speed);
    }

    public static void displayCircle(Location center, double radius, int sampleCount, Particle particle, int count, double dx, double dy, double dz,
                                     double speed) {
        List<Location> circle = LocationUtil.getCircle(center, radius, sampleCount);
        ParticleUtil.globalParticle(circle, particle, count, dx, dy, dz, speed);
    }

    public static void displayShield(Location center, double yaw) {
        double[] xCoordsFrom = new double[]{0, -1, -2, -3, -4, -5, -6, -6, -7, -7, -7, -8, -8, -8, -8, -9, -9, -9, -9, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0};
        double[] xCoordsTo = new double[]{0, 1, 2, 3, 4, 5, 6, 6, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        double[] yCoords = new double[]{-12.5, -12, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 5, 5, 6, 6, 7, 7, 8, 8, 9};

        for (int a = 0; a < xCoordsFrom.length; a++) {
            Location loc1 = center.clone().add(xCoordsFrom[a] / 10, yCoords[a] / 10, 0);
            Location loc2 = center.clone().add(xCoordsTo[a] / 10, yCoords[a] / 10, 0);
            List<Location> rotated = LocationUtil.rotateLocations(center, Arrays.asList(loc1, loc2), yaw, 0, 0, 1);
            displayLine(rotated.get(0), rotated.get(1), 8, Particle.CRIT_MAGIC, 1, 0, 0, 0, 0);
        }
    }

    public static void magicCircle(Location location, Particle particle, int durationTicks, double size, double rotationSpeed, int sample) {
        ArrayList<Location> circle = LocationUtil.getCircle(location, size, sample);
        int gap = sample / 6;

        int rotationDelay = (int) (1 / rotationSpeed);
        int rotationDelta = rotationSpeed > 0 ? 1 : -1;

        new SafeRunnable() {
            int duration = 0;
            int offset = 0;

            @Override
            public void safeRun() {
                if (duration > durationTicks) {
                    this.cancel();
                    return;
                }

                // Rotate @ speed
                if (rotationDelay != 0 && duration % rotationDelay == 0)
                    offset = Math.floorMod(offset + rotationDelta, sample);

                Location topPoint = circle.get(offset % sample);
                Location leftPoint = circle.get((offset + gap * 2) % sample);
                Location rightPoint = circle.get((offset + gap * 4) % sample);
                Location topPoint2 = circle.get((offset + gap) % sample);
                Location leftPoint2 = circle.get((offset + gap * 3) % sample);
                Location rightPoint2 = circle.get((offset + gap * 5) % sample);

                displayLine(topPoint.clone(), leftPoint.clone(), sample / 3, particle, 1, 0, 0, 0, 0);
                displayLine(leftPoint.clone(), rightPoint.clone(), sample / 3, particle, 1, 0, 0, 0, 0);
                displayLine(rightPoint.clone(), topPoint.clone(), sample / 3, particle, 1, 0, 0, 0, 0);

                displayLine(topPoint2.clone(), leftPoint2.clone(), sample / 3, particle, 1, 0, 0, 0, 0);
                displayLine(leftPoint2.clone(), rightPoint2.clone(), sample / 3, particle, 1, 0, 0, 0, 0);
                displayLine(rightPoint2.clone(), topPoint2.clone(), sample / 3, particle, 1, 0, 0, 0, 0);

                for (Location circleLoc : circle)
                    location.getWorld().spawnParticle(particle, circleLoc.getX(), circleLoc.getY(), circleLoc.getZ(), 1, 0, 0, 0, 0);
                duration++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, 1);
    }

    public static void magicCircle2(Location location, Particle particle, int durationTicks, double size, double rotationSpeed, int sample) {
        ArrayList<Location> circle = LocationUtil.getCircle(location, size, sample);
        int gap = sample / 10;

        int rotationDelay = (int) (1 / rotationSpeed);
        int rotationDelta = rotationSpeed > 0 ? 1 : -1;

        new SafeRunnable() {
            int duration = 0;
            int offset = 0;
            Location[] keyPoints = new Location[10];

            @Override
            public void safeRun() {
                if (duration > durationTicks) {
                    this.cancel();
                    return;
                }
                // Rotate @ speed
                if (rotationDelay != 0 && duration % rotationDelay == 0)
                    offset = Math.floorMod(offset + rotationDelta, sample);
                // Get key points
                for (int a = 0; a < 10; a++)
                    keyPoints[a] = circle.get((offset + gap * a) % sample);

                // Display particles
                for (int a = 0; a < 10; a += 2)
                    displayLine(keyPoints[a].clone(), keyPoints[(a + 2) % 10].clone(), sample / 3, particle, 1, 0, 0, 0, 0);
                for (int a = 1; a < 10; a += 2)
                    displayLine(keyPoints[a].clone(), keyPoints[(a + 2) % 10].clone(), sample / 3, particle, 1, 0, 0, 0, 0);

                for (Location circleLoc : circle)
                    location.getWorld().spawnParticle(particle, circleLoc.getX(), circleLoc.getY(), circleLoc.getZ(), 1,
                            0, 0, 0, 0);
                duration++;
            }
        }.runTaskTimer(Main.getPlugin(), 0, 1);
    }

}
