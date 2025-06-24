package info.iwwi.addon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.HashSet;
import java.util.Set;

import info.iwwi.addon.IWWIAddon;

public class StoneESP extends Module {
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<SettingColor> espColor = sgRender.add(new ColorSetting.Builder()
        .name("esp-color")
        .description("ESP box color.")
        .defaultValue(new SettingColor(255, 0, 0, 100))
        .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Box render mode.")
        .defaultValue(ShapeMode.Both)
        .build());

    private final Setting<Boolean> chatFeedback = sgRender.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Announce detections in chat.")
        .defaultValue(true)
        .build());

    private final Set<BlockPos> detected = new HashSet<>();


    private static final Set<Block> illegalAboveY10 = Set.of(

        Blocks.DEEPSLATE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_COAL_ORE,
        Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES,
        Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_TILES,
        Blocks.INFESTED_DEEPSLATE,


        Blocks.NETHER_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.NETHER_QUARTZ_ORE,
        Blocks.NETHER_GOLD_ORE, Blocks.ANCIENT_DEBRIS, Blocks.BASALT, Blocks.POLISHED_BASALT,
        Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS,


        Blocks.END_STONE, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.CHORUS_FLOWER, Blocks.CHORUS_PLANT,


        Blocks.SPAWNER, Blocks.BEDROCK
    );

    public StoneESP() {
        super(IWWIAddon.CATEGORY, "stone-esp", "ESP for player placed blocks above Y=10 (Overworld, Nether, End).");
    }

    @EventHandler
    private void onChunkLoad(ChunkDataEvent event) {
        scanChunk(event.chunk());
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        BlockPos pos = event.pos;
        BlockState state = event.newState;

        if (isIllegalBlock(state, pos.getY())) {
            if (detected.add(pos) && chatFeedback.get()) {
                info("§d[Above Y10 ESP] §fplayer placed block (" + state.getBlock().getName().getString() + ") at §a" + pos.toShortString());
            }
        }
    }

    private void scanChunk(Chunk chunk) {
        int xStart = chunk.getPos().getStartX();
        int zStart = chunk.getPos().getStartZ();
        int yMin = chunk.getBottomY();
        int yMax = yMin + chunk.getHeight();

        for (int x = xStart; x < xStart + 16; x++) {
            for (int z = zStart; z < zStart + 16; z++) {
                for (int y = Math.max(11, yMin); y < yMax; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (isIllegalBlock(state, y) && detected.add(pos) && chatFeedback.get()) {
                        info("§d[Above Y10 ESP] §fplayer placed block (" + state.getBlock().getName().getString() + ") at §a" + pos.toShortString());
                    }
                }
            }
        }
    }

    private boolean isIllegalBlock(BlockState state, int y) {
        return y > 10 && illegalAboveY10.contains(state.getBlock());
    }

    @Override
    public void onActivate() {
        detected.clear();
        if (mc.world == null) return;

        for (Chunk chunk : Utils.chunks()) {
            scanChunk(chunk);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        Color side = new Color(espColor.get());
        Color lines = new Color(espColor.get());

        for (BlockPos pos : detected) {
            event.renderer.box(pos, side, lines, shapeMode.get(), 0);
        }
    }
}
