package dev.breeze.settlements.entities.navigation;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.stream.Stream;

@Getter
public class VillagerNavigation extends GroundPathNavigation {

    public static final int FENCE_GATE_COST_MALUS = 2;

    public VillagerNavigation(Mob entity, Level world) {
        super(entity, world);
    }

    /**
     * Override to use custom villager pathfinder
     */
    @Override
    protected @Nonnull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new VillagerNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);

        return new PathFinder(this.nodeEvaluator, range);
    }

    /**
     * A modified version of the WalkNodeEvaluator for the custom villagers
     * - villagers can now walk "through" fence gates
     */
    private static class VillagerNodeEvaluator extends WalkNodeEvaluator {

        // Copied from parent because it's private
        private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap<>();

        @Override
        public void done() {
            this.collisionCache.clear();
            super.done();
        }

        @Override
        protected boolean isNeighborValid(@Nullable Node node, Node successor) {
            // Get super's answer first
            boolean isValid = super.isNeighborValid(node, successor);

            // Check if node is null or closed (previously visited)
            if (node == null || node.closed)
                return false;

            BlockPathTypes blockPathTypes = this.getCachedBlockType(this.mob, node.x, node.y, node.z);
            if (blockPathTypes != BlockPathTypes.FENCE)
                return isValid;

            // If the block is fence gate, return true
            // - cause custom villagers can open fence gates
            BlockPos blockPos = new BlockPos(node.x, node.y, node.z);
            if (isFenceGate(this.level.getBlockState(blockPos)))
                return !node.closed;

            return isValid;
        }

        @Override
        protected boolean isDiagonalValid(Node xNode, Node zNode, Node xDiagNode, Node zDiagNode) {
            // Get super's answer first
            boolean isValid = super.isDiagonalValid(xNode, zNode, xDiagNode, zDiagNode);

            // If any of the 4 nodes is a fence gate, then not valid
            if (isValid && Stream.of(xNode, zNode, xDiagNode, zDiagNode).anyMatch((node -> node.type == BlockPathTypes.FENCE && node.costMalus == FENCE_GATE_COST_MALUS))) {
                return false;
            }

            return isValid;
        }

        //        @Override
        public BlockPathTypes xxxgetBlockPathType(BlockGetter world, int x, int y, int z, Mob mob, int sizeX, int sizeY, int sizeZ, boolean canOpenDoors,
                                                  boolean canEnterOpenDoors) {
            EnumSet<BlockPathTypes> enumSet = EnumSet.noneOf(BlockPathTypes.class);
            BlockPathTypes blockPathTypes = BlockPathTypes.BLOCKED;
            BlockPos blockPos = mob.blockPosition();
            blockPathTypes = this.getBlockPathTypes(world, x, y, z, sizeX, sizeY, sizeZ, canOpenDoors, canEnterOpenDoors, enumSet, blockPathTypes, blockPos);

            if (enumSet.contains(BlockPathTypes.FENCE)) {
                // Added fence gate identification
                return isFenceGate(world.getBlockState(new BlockPos(x, y, z))) ? BlockPathTypes.OPEN : BlockPathTypes.FENCE;
            } else if (enumSet.contains(BlockPathTypes.UNPASSABLE_RAIL)) {
                return BlockPathTypes.UNPASSABLE_RAIL;
            } else {
                BlockPathTypes blockPathTypes2 = BlockPathTypes.BLOCKED;

                for (BlockPathTypes blockPathTypes3 : enumSet) {
                    if (mob.getPathfindingMalus(blockPathTypes3) < 0.0F) {
                        return blockPathTypes3;
                    }

                    if (mob.getPathfindingMalus(blockPathTypes3) >= mob.getPathfindingMalus(blockPathTypes2)) {
                        blockPathTypes2 = blockPathTypes3;
                    }
                }

                return blockPathTypes == BlockPathTypes.OPEN && mob.getPathfindingMalus(blockPathTypes2) == 0.0F && sizeX <= 1 ? BlockPathTypes.OPEN :
                        blockPathTypes2;
            }
        }

//        @Override
//        protected BlockPathTypes evaluateBlockPathType(BlockGetter world, boolean canOpenDoors, boolean canEnterOpenDoors, BlockPos pos, BlockPathTypes
//        type) {
//            if (type == BlockPathTypes.FENCE && isFenceGate(world.getBlockState(pos))) {
//                MessageUtil.broadcast(world.getBlockState(pos).toString());
//                InteractWithFenceGate2.highlight(this.mob.level.getMinecraftWorld(), pos, Particle.VILLAGER_ANGRY);
//                return BlockPathTypes.OPEN;
//            }
//            return super.evaluateBlockPathType(world, canOpenDoors, canEnterOpenDoors, pos, type);
//        }

        /**
         * Mostly copied over from the parent class
         * - with some minor refactoring
         * - and one additional block to allow "passage" through fence gates
         */
        @Override
        @Nullable
        protected Node findAcceptedNode(int x, int y, int z, int maxYStep, double prevFeetY, Direction direction, BlockPathTypes nodeType) {
            Node node = null;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            double d = this.getFloorLevel(mutableBlockPos.set(x, y, z));
            if (d - prevFeetY > this.getMobJumpHeight()) {
                return null;
            }
            BlockPathTypes blockPathTypes = this.getCachedBlockType(this.mob, x, y, z);
            float penalty = this.mob.getPathfindingMalus(blockPathTypes);
            double e = (double) this.mob.getBbWidth() / 2.0D;
            if (penalty >= 0.0F) {
                node = this.getNodeAndUpdateCostToMax(x, y, z, blockPathTypes, penalty);
            }

            if (doesBlockHavePartialCollision(nodeType) && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
                node = null;
            }

            if (blockPathTypes != BlockPathTypes.WALKABLE && (!this.isAmphibious() || blockPathTypes != BlockPathTypes.WATER)) {
                if ((node == null || node.costMalus < 0.0F) && maxYStep > 0 && (blockPathTypes != BlockPathTypes.FENCE || this.canWalkOverFences()) && blockPathTypes != BlockPathTypes.UNPASSABLE_RAIL && blockPathTypes != BlockPathTypes.TRAPDOOR && blockPathTypes != BlockPathTypes.POWDER_SNOW) {
                    node = this.findAcceptedNode(x, y + 1, z, maxYStep - 1, prevFeetY, direction, nodeType);
                    if (node != null && (node.type == BlockPathTypes.OPEN || node.type == BlockPathTypes.WALKABLE) && this.mob.getBbWidth() < 1.0F) {
                        double g = (double) (x - direction.getStepX()) + 0.5D;
                        double h = (double) (z - direction.getStepZ()) + 0.5D;
                        AABB aABB = new AABB(g - e, this.getFloorLevel(mutableBlockPos.set(g, y + 1, h)) + 0.001D, h - e, g + e,
                                (double) this.mob.getBbHeight() + this.getFloorLevel(mutableBlockPos.set(node.x, node.y, node.z)) - 0.002D, h + e);
                        if (this.hasCollisions(aABB)) {
                            node = null;
                        }
                    }
                }

                if (!this.isAmphibious() && blockPathTypes == BlockPathTypes.WATER && !this.canFloat()) {
                    if (this.getCachedBlockType(this.mob, x, y - 1, z) != BlockPathTypes.WATER) {
                        return node;
                    }

                    while (y > this.mob.level.getMinBuildHeight()) {
                        --y;
                        blockPathTypes = this.getCachedBlockType(this.mob, x, y, z);
                        if (blockPathTypes != BlockPathTypes.WATER) {
                            return node;
                        }

                        node = this.getNodeAndUpdateCostToMax(x, y, z, blockPathTypes, this.mob.getPathfindingMalus(blockPathTypes));
                    }
                }

                if (blockPathTypes == BlockPathTypes.OPEN) {
                    int i = 0;
                    int j = y;

                    while (blockPathTypes == BlockPathTypes.OPEN) {
                        --y;
                        if (y < this.mob.level.getMinBuildHeight()) {
                            return this.getBlockedNode(x, j, z);
                        }

                        if (i++ >= this.mob.getMaxFallDistance()) {
                            return this.getBlockedNode(x, y, z);
                        }

                        blockPathTypes = this.getCachedBlockType(this.mob, x, y, z);
                        penalty = this.mob.getPathfindingMalus(blockPathTypes);
                        if (blockPathTypes != BlockPathTypes.OPEN && penalty >= 0.0F) {
                            node = this.getNodeAndUpdateCostToMax(x, y, z, blockPathTypes, penalty);
                            break;
                        }

                        if (penalty < 0.0F) {
                            return this.getBlockedNode(x, y, z);
                        }
                    }
                }

                // >> Custom code begin -- recognize fence gate as "walkable"
                if (node == null && blockPathTypes == BlockPathTypes.FENCE && isFenceGate(this.level.getBlockState(new BlockPos(x, y, z)))) {
                    node = this.getNode(x, y, z);
                    node.type = BlockPathTypes.FENCE;
                    node.costMalus = FENCE_GATE_COST_MALUS;
                }
                // << Custom code ends

                if (doesBlockHavePartialCollision(blockPathTypes) && node == null) {
                    node = this.getNode(x, y, z);
                    node.closed = true;
                    node.type = blockPathTypes;
                    node.costMalus = blockPathTypes.getMalus();
                }

            }
            return node;
        }

        /**
         * Copied from the parent class
         */
        private double getMobJumpHeight() {
            return Math.max(1.125D, this.mob.maxUpStep);
        }

        /**
         * Copied from the parent class
         */
        private Node getNodeAndUpdateCostToMax(int x, int y, int z, BlockPathTypes type, float penalty) {
            Node node = this.getNode(x, y, z);
            node.type = type;
            node.costMalus = Math.max(node.costMalus, penalty);
            return node;
        }

        /**
         * Copied from the parent class
         */
        private static boolean doesBlockHavePartialCollision(BlockPathTypes nodeType) {
            return nodeType == BlockPathTypes.FENCE || nodeType == BlockPathTypes.DOOR_WOOD_CLOSED || nodeType == BlockPathTypes.DOOR_IRON_CLOSED;
        }

        /**
         * Copied from the parent class
         */
        private boolean canReachWithoutCollision(Node node) {
            AABB aABB = this.mob.getBoundingBox();
            Vec3 vec3 = new Vec3((double) node.x - this.mob.getX() + aABB.getXsize() / 2.0D,
                    (double) node.y - this.mob.getY() + aABB.getYsize() / 2.0D,
                    (double) node.z - this.mob.getZ() + aABB.getZsize() / 2.0D);
            int i = Mth.ceil(vec3.length() / aABB.getSize());
            vec3 = vec3.scale(1.0F / (float) i);

            for (int j = 1; j <= i; ++j) {
                aABB = aABB.move(vec3);
                if (this.hasCollisions(aABB)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Copied from the parent class
         */
        private boolean hasCollisions(AABB box) {
            return this.collisionCache.computeIfAbsent(box, (box2) -> !this.level.noCollision(this.mob, box));
        }

        /**
         * Copied from the parent class
         */
        private Node getBlockedNode(int x, int y, int z) {
            Node node = this.getNode(x, y, z);
            node.type = BlockPathTypes.BLOCKED;
            node.costMalus = -1.0F;
            return node;
        }

        private static boolean isFenceGate(BlockState state) {
            return state.is(BlockTags.FENCE_GATES, stateBase -> stateBase.getBlock() instanceof FenceGateBlock);
        }

    }

}
