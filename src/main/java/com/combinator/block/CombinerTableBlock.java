package com.combinator.block;

import com.combinator.screen.CombinerScreenHandler;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The Combiner Table. Works like a crafting table: it has no storage, items come back when you close it. */
public class CombinerTableBlock extends Block {
	private static final Text TITLE = Text.translatable("container.combinator.combiner");

	public CombinerTableBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
		return ActionResult.CONSUME;
	}

	@Override
	protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
		return new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, player) -> new CombinerScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos)),
				TITLE);
	}
}
