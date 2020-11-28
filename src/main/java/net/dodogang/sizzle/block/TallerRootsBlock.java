package net.dodogang.sizzle.block;

import java.util.ArrayList;

import net.dodogang.sizzle.block.enums.TallerBlockPart;
import net.dodogang.sizzle.state.property.SizzleProperties;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class TallerRootsBlock extends PlantBlock {
    public static final EnumProperty<TallerBlockPart> PART = SizzleProperties.TALLER_BLOCK_PART;

    public TallerRootsBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(PART, TallerBlockPart.LOWER));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState, WorldAccess world, BlockPos pos, BlockPos posFrom) {
        TallerBlockPart part = state.get(PART);
        if (direction == Direction.UP && part != TallerBlockPart.UPPER) {
            Block block = newState.getBlock();
            if (block != this) return Blocks.AIR.getDefaultState();
        } else if (direction == Direction.DOWN && part != TallerBlockPart.LOWER) {
            Block block = newState.getBlock();
            if (block != this) return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos blockPos = ctx.getBlockPos();
        return blockPos.getY() < 254 && ctx.getWorld().getBlockState(blockPos.up(1)).canReplace(ctx) && ctx.getWorld().getBlockState(blockPos.up(2)).canReplace(ctx)
            ? super.getPlacementState(ctx)
            : null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(1), this.getDefaultState().with(PART, TallerBlockPart.MIDDLE), 3);
        world.setBlockState(pos.up(2), this.getDefaultState().with(PART, TallerBlockPart.UPPER), 3);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(PART) == TallerBlockPart.LOWER) {
            return super.canPlaceAt(state, world, pos);
        } else if(state.get(PART) == TallerBlockPart.MIDDLE) {
            BlockState blockStateDown1 = world.getBlockState(pos.down(1));
            return (blockStateDown1.isOf(this) && blockStateDown1.get(PART) == TallerBlockPart.LOWER);
        } else {
            BlockState blockStateDown1 = world.getBlockState(pos.down(1));
            BlockState blockStateDown2 = world.getBlockState(pos.down(2));
            return (blockStateDown1.isOf(this) && blockStateDown1.get(PART) == TallerBlockPart.MIDDLE)
                && (blockStateDown2.isOf(this) && blockStateDown2.get(PART) == TallerBlockPart.LOWER);
        }
    }

    public void placeAt(WorldAccess world, BlockPos pos, int flags) {
        world.setBlockState(pos, this.getDefaultState().with(PART, TallerBlockPart.LOWER), flags);
        world.setBlockState(pos.up(1), this.getDefaultState().with(PART, TallerBlockPart.MIDDLE), flags);
        world.setBlockState(pos.up(2), this.getDefaultState().with(PART, TallerBlockPart.UPPER), flags);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            if (player.isCreative()) {
                onBreakInCreative(world, pos, state, player);
            } else {
                dropStacks(state, world, pos, (BlockEntity) null, player, player.getMainHandStack());
            }
        }

        super.onBreak(world, pos, state, player);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack) {
        super.afterBreak(world, player, pos, Blocks.AIR.getDefaultState(), blockEntity, stack);
    }

    protected static void onBreakInCreative(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        TallerBlockPart tallerBlockPart = state.get(PART);

        if (tallerBlockPart == TallerBlockPart.UPPER) {
            positions.add(pos.down(1));
            positions.add(pos.down(2));
        } else if (tallerBlockPart == TallerBlockPart.MIDDLE) {
            positions.add(pos.down(1));
            positions.add(pos.up(1));
        } else if (tallerBlockPart == TallerBlockPart.LOWER) {
            positions.add(pos.up(1));
            positions.add(pos.up(2));
        }

        positions.forEach((blockPos) -> {
            world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 35);
            world.syncWorldEvent(null, 2001, blockPos, Block.getRawIdFromState(state));
        });
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public AbstractBlock.OffsetType getOffsetType() {
        return AbstractBlock.OffsetType.XZ;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public long getRenderingSeed(BlockState state, BlockPos pos) {
        return MathHelper.hashCode(pos.getX(), pos.down(state.get(PART) == TallerBlockPart.LOWER ? 0 : 1).getY(), pos.getZ());
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(BlockTags.NYLIUM) || floor.isOf(Blocks.SOUL_SOIL) || super.canPlantOnTop(floor, world, pos);
    }
}
