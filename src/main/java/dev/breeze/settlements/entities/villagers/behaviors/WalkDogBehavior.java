package dev.breeze.settlements.entities.villagers.behaviors;

import dev.breeze.settlements.entities.villagers.memories.VillagerMemoryType;
import dev.breeze.settlements.entities.wolves.VillagerWolf;
import dev.breeze.settlements.entities.wolves.behaviors.WolfWalkBehavior;
import dev.breeze.settlements.utils.PacketUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.Map;

public class WalkDogBehavior extends Behavior<Villager> {

    private static final float SPEED_MODIFIER = 0.6F;

    private VillagerWolf cachedWolf;

    public WalkDogBehavior() {
        super(Map.of(
                // A dog must be present to walk
                VillagerMemoryType.WALK_DOG_TARGET, MemoryStatus.VALUE_PRESENT
        ), WolfWalkBehavior.MAX_WALK_DURATION);
    }

    @Override
    protected final boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull Villager self) {
        return self.getBrain().hasMemoryValue(VillagerMemoryType.WALK_DOG_TARGET);
    }

    @Override
    protected final boolean canStillUse(ServerLevel level, Villager self, long gameTime) {
        return this.checkExtraStartConditions(level, self);
    }

    @Override
    protected void start(ServerLevel level, Villager self, long gameTime) {
        VillagerWolf wolf = self.getBrain().getMemory(VillagerMemoryType.WALK_DOG_TARGET).get();
        this.cachedWolf = wolf;

        // Send leash packet (we don't want to actually leash the wolf)
        ClientboundSetEntityLinkPacket packet = new ClientboundSetEntityLinkPacket(wolf, self);
        PacketUtil.sendPacketToAllPlayers(packet);

    }

    @Override
    protected final void tick(ServerLevel level, Villager self, long gameTime) {
        PathNavigation navigation = self.getNavigation();
        if (!navigation.isDone())
            return;

        // Follow the wolf
        VillagerWolf wolf = self.getBrain().getMemory(VillagerMemoryType.WALK_DOG_TARGET).get();
        self.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(wolf, SPEED_MODIFIER, 3));
    }

    @Override
    protected void stop(ServerLevel level, Villager self, long gameTime) {
        if (this.cachedWolf != null) {
            // Detach leash
            ClientboundSetEntityLinkPacket packet = new ClientboundSetEntityLinkPacket(this.cachedWolf, null);
            PacketUtil.sendPacketToAllPlayers(packet);
        }
    }

}
