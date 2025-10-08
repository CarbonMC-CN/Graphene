package net.carbonmc.graphene.client.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.carbonmc.graphene.config.CoolConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
@OnlyIn(Dist.CLIENT)
public final class GUIEN {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("⚙ Graphene Configuration"))
                .transparentBackground()
                .setSavingRunnable(CoolConfig.SPEC::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory compat = builder.getOrCreateCategory(Component.literal("High Version Optimization Port"));
        compat.addEntry(bool(eb, "Fix Pearl Leak", CoolConfig.FIX_PEARL_LEAK));
        compat.addEntry(bool(eb, "Fix Projectile Lerp", CoolConfig.FIX_PROJECTILE_LERP));

        ConfigCategory render = builder.getOrCreateCategory(Component.literal("Render Optimization"));
        render.addEntry(bool(eb, "Skip Outline When No Glowing Entities", CoolConfig.skipOutlineWhenNoGlowing));
        SubCategoryBuilder fps = eb.startSubCategory(Component.literal("Reduce Render Latency"));
        fps.add(bool(eb, "Enable Optimization", CoolConfig.fpsoo));
        render.addEntry(fps.build());

        SubCategoryBuilder chest = eb.startSubCategory(Component.literal("Chest Render Optimization"));
        chest.add(bool(eb, "Enable Optimization", CoolConfig.ENABLE_OPTIMIZATION));
        chest.add(enumOpt(eb, "Render Mode", CoolConfig.RenderMode.class, CoolConfig.RENDER_MODE));
        chest.add(bool(eb, "Optimize Ender Chests", CoolConfig.OPTIMIZE_ENDER_CHESTS));
        chest.add(bool(eb, "Optimize Trapped Chests", CoolConfig.OPTIMIZE_TRAPPED_CHESTS));
        chest.add(intSlider(eb, "Max Render Distance", 1, 128, CoolConfig.MAX_RENDER_DISTANCE));
        render.addEntry(chest.build());

        ConfigCategory reflex = builder.getOrCreateCategory(Component.literal("Reflex Low Latency"));
        reflex.addEntry(bool(eb, "Enable Reflex", CoolConfig.enableReflex));
        reflex.addEntry(longField(eb, "Reflex Offset (ns)",
                -1_000_000L, 1_000_000L, CoolConfig.reflexOffsetNs));
        reflex.addEntry(intSlider(eb, "Reflex Max FPS", 0, 1000, CoolConfig.MAX_FPS));

        SubCategoryBuilder cull = eb.startSubCategory(Component.literal("Advanced Culling"));
        cull.add(bool(eb, "Enable Culling", CoolConfig.ENABLEDCULL));
        cull.add(intSlider(eb, "Culling Depth", 1, 5, CoolConfig.CULLING_DEPTH));
        cull.add(doubleField(eb, "Rejection Rate", 0, 1, CoolConfig.REJECTION_RATE));
        cull.add(bool(eb, "Ultra Aggressive Culling", CoolConfig.ULTRA_CULLING));
        cull.add(intSlider(eb, "Ultra Depth", 1, 4, CoolConfig.ULTRA_DEPTH));
        cull.add(doubleField(eb, "Backface Culling Threshold", 0, 1, CoolConfig.ULTRA_BACKFACE));
        cull.add(bool(eb, "Advanced Culling Algorithm", CoolConfig.ADVANCED_CULLING));
        render.addEntry(cull.build());

        SubCategoryBuilder trace = eb.startSubCategory(Component.literal("Path Tracing"));
        trace.add(bool(eb, "Async Tracing", CoolConfig.useAsyncTracing));
        trace.add(intSlider(eb, "Tracing Threads", 1, 8, CoolConfig.tracingThreads));
        trace.add(doubleField(eb, "Trace Distance", 1, 16, CoolConfig.traceDistance));
        trace.add(doubleField(eb, "Fallback Distance", 4, 32, CoolConfig.fallbackDistance));
        render.addEntry(trace.build());

        SubCategoryBuilder leaf = eb.startSubCategory(Component.literal("Leaf Optimization"));
        leaf.add(bool(eb, "Advanced Leaf Culling", CoolConfig.useAdvancedLeafCulling));
        leaf.add(intSlider(eb, "Min Leaf Connections", 1, 6, CoolConfig.minLeafConnections));
        leaf.add(bool(eb, "Optimize Mangrove", CoolConfig.OPTIMIZE_MANGROVE));
        render.addEntry(leaf.build());

        ConfigCategory particle = builder.getOrCreateCategory(Component.literal("Particle Optimization"));
        particle.addEntry(bool(eb, "Enable Particle Optimization", CoolConfig.ENABLE_PARTICLE_OPTIMIZATION));

        SubCategoryBuilder lod = eb.startSubCategory(Component.literal("LOD System"));
        lod.add(bool(eb, "Enable Particle LOD", CoolConfig.ENABLE_PARTICLE_LOD));
        lod.add(doubleField(eb, "LOD Distance Threshold", 4, 64, CoolConfig.LOD_DISTANCE_THRESHOLD));
        lod.add(doubleField(eb, "LOD Reduction Factor", 0, 1, CoolConfig.LOD_REDUCTION_FACTOR));
        particle.addEntry(lod.build());

        SubCategoryBuilder timestep = eb.startSubCategory(Component.literal("Timestep"));
        timestep.add(bool(eb, "Enable Fixed Timestep", CoolConfig.ENABLE_FIXED_TIMESTEP));
        timestep.add(doubleField(eb, "Fixed Timestep Interval", 0.001, 0.1, CoolConfig.FIXED_TIMESTEP_INTERVAL));
        particle.addEntry(timestep.build());

        SubCategoryBuilder lists = eb.startSubCategory(Component.literal("Particle Lists"));
        lists.add(stringList(eb, "LOD Whitelist", CoolConfig.LOD_PARTICLE_WHITELIST));
        lists.add(stringList(eb, "LOD Blacklist", CoolConfig.LOD_PARTICLE_BLACKLIST));
        particle.addEntry(lists.build());

        SubCategoryBuilder inactive = eb.startSubCategory(Component.literal("Inactive State Optimization"));
        inactive.add(bool(eb, "Reduce FPS When Inactive", CoolConfig.REDUCE_FPS_WHEN_INACTIVE));
        inactive.add(intSlider(eb, "Inactive FPS Limit", 5, 60, CoolConfig.INACTIVE_FPS_LIMIT));
        inactive.add(bool(eb, "Reduce Render Distance When Inactive", CoolConfig.REDUCE_RENDER_DISTANCE_WHEN_INACTIVE));
        inactive.add(intSlider(eb, "Inactive Render Distance", 2, 12, CoolConfig.INACTIVE_RENDER_DISTANCE));
        render.addEntry(inactive.build());

        ConfigCategory light = builder.getOrCreateCategory(Component.literal("Lighting Performance Optimization"));
        light.addEntry(bool(eb, "Lighting Optimization", CoolConfig.ENABLE_FIXED_LIGHT));
        light.addEntry(bool(eb, "Bamboo Lighting Optimization - Remove unnecessary lighting calculations for bamboo", CoolConfig.BambooLight));

        ConfigCategory math = builder.getOrCreateCategory(Component.literal("Math Optimization"));
        math.addEntry(bool(eb, "Improve Random Performance", CoolConfig.RandomOptimizeENABLED));

        ConfigCategory entity = builder.getOrCreateCategory(Component.literal("Entity Optimization"));
        entity.addEntry(bool(eb, "Disable Entity Collisions", CoolConfig.disableEntityCollisions));
        entity.addEntry(bool(eb, "Entity Tick Optimization", CoolConfig.optimizeEntities));
        entity.addEntry(bool(eb, "Villager Optimization", CoolConfig.VILLAGER_MOVE_OPTIMIZE));
        entity.addEntry(intSlider(eb, "Horizontal Range", 1, 256, CoolConfig.horizontalRange));
        entity.addEntry(intSlider(eb, "Vertical Range", 1, 256, CoolConfig.verticalRange));
        entity.addEntry(bool(eb, "Ignore Dead Entities", CoolConfig.ignoreDeadEntities));
        entity.addEntry(bool(eb, "Clean Up Dead Entities", CoolConfig.OPTIMIZE_ENTITY_CLEANUP));
        entity.addEntry(bool(eb, "Keep Ticking Raiders During Raid", CoolConfig.tickRaidersInRaid));

        ConfigCategory item = builder.getOrCreateCategory(Component.literal("Item Optimization"));
        item.addEntry(bool(eb, "Enable Item Optimization", CoolConfig.OpenIO));
        item.addEntry(intSlider(eb, "Max Stack Size", -1, 9999, CoolConfig.maxStackSize));
        item.addEntry(doubleField(eb, "Merge Radius", 0.1, 10, CoolConfig.mergeDistance));
        item.addEntry(bool(eb, "Lock Maxed Stacks", CoolConfig.lockMaxedStacks));
        item.addEntry(intSlider(eb, "List Mode", 0, 2, CoolConfig.listMode));
        item.addEntry(bool(eb, "Show Stack Count", CoolConfig.showStackCount));
        item.addEntry(bool(eb, "Enable Custom Stacking", CoolConfig.ENABLED));
        item.addEntry(intSlider(eb, "Custom Max Stack Size", 1, 9999, CoolConfig.MAX_STACK_SIZE));
        item.addEntry(bool(eb, "Item Entity Tick Optimization", CoolConfig.optimizeItems));

        ConfigCategory mem = builder.getOrCreateCategory(Component.literal("Memory Optimization"));
        mem.addEntry(intSlider(eb, "Cleanup Interval (seconds)", 60, 3600, CoolConfig.MEMORY_CLEAN_INTERVAL));
        mem.addEntry(bool(eb, "Trigger GC", CoolConfig.ENABLE_GC));

        ConfigCategory chunk = builder.getOrCreateCategory(Component.literal("Chunk Optimization"));
        chunk.addEntry(bool(eb, "Chunk Loading Speed Optimization - Main", CoolConfig.XtackChunk));
        chunk.addEntry(bool(eb, "Chunk Optimization - Beta (Unknown Effect)", CoolConfig.XtackChunk_BETA));
        chunk.addEntry(bool(eb, "Chunk Loading Speed Optimization - Slow Entities - Beta (Unknown Consequences)", CoolConfig.FAST_CHUNK_ENTITY));
        chunk.addEntry(bool(eb, "Aggressive Chunk Unloading - Beta (Unknown Consequences)", CoolConfig.aggressiveChunkUnloading));
        chunk.addEntry(intSlider(eb, "Chunk Unload Delay (seconds)", 10, 600, CoolConfig.chunkUnloadDelay));
        chunk.addEntry(intSlider(eb, "ChunkThreads", 4, 64, CoolConfig.CHUNKTHREADS));
        chunk.addEntry(intSlider(eb, "ChunkIOThreads", 4, 64, CoolConfig.CHUNKIO_THREADS));
        chunk.addEntry(bool(eb, "ChunkOptimize-IO", CoolConfig.CHUNK_REDIRECT_IO));
        chunk.addEntry(bool(eb, "ChunkOptimize-LIGHT", CoolConfig.CHUNk_REDIRECT_LIGHTING));
        ConfigCategory async = builder.getOrCreateCategory(Component.literal("Async Optimization"));
        async.addEntry(bool(eb, "Async Particles", CoolConfig.ASYNC_PARTICLES));
        async.addEntry(intSlider(eb, "Max Async Operations Per Tick", 100, 10000, CoolConfig.MAX_ASYNC_OPERATIONS_PER_TICK));
        async.addEntry(bool(eb, "Disable Async On Error", CoolConfig.DISABLE_ASYNC_ON_ERROR));
        async.addEntry(intSlider(eb, "Async Event Timeout (seconds)", 1, 10, CoolConfig.ASYNC_EVENT_TIMEOUT));
        async.addEntry(bool(eb, "Wait For Async Events", CoolConfig.WAIT_FOR_ASYNC_EVENTS));
        async.addEntry(intSlider(eb, "Max CPU Cores", 2, 128, CoolConfig.maxCPUPro));
        async.addEntry(intSlider(eb, "Max Threads", 2, 256, CoolConfig.maxthreads));

        ConfigCategory evt = builder.getOrCreateCategory(Component.literal("Event System"));
        evt.addEntry(bool(eb, "Enable Async Event System", CoolConfig.FEATURE_ENABLED));
        evt.addEntry(bool(eb, "Strict Class Checking", CoolConfig.STRICT_CLASS_CHECKING));


        return builder.build();
    }

    private static BooleanListEntry bool(ConfigEntryBuilder eb,
                                         String key,
                                         ForgeConfigSpec.BooleanValue value) {
        return eb.startBooleanToggle(Component.literal(key), value.get())
                .setTooltip(Component.literal(key))
                .setSaveConsumer(value::set)
                .build();
    }


    private static LongListEntry longField(ConfigEntryBuilder eb,
                                           String key,
                                           long min,
                                           long max,
                                           ForgeConfigSpec.LongValue value) {
        return eb.startLongField(Component.literal(key), value.get())
                .setMin(min).setMax(max)
                .setTooltip(Component.literal(key))
                .setSaveConsumer(value::set)
                .build();
    }


    private static <E extends Enum<E>> EnumListEntry<E> enumOpt(ConfigEntryBuilder eb,
                                                                String key,
                                                                Class<E> clazz,
                                                                ForgeConfigSpec.EnumValue<E> value) {
        return eb.startEnumSelector(Component.literal(key), clazz, value.get())
                .setTooltip(Component.literal(key))
                .setSaveConsumer(value::set)
                .build();
    }
    private static StringListListEntry stringList(ConfigEntryBuilder eb,
                                                  String key,
                                                  ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        List<String> currentValue = (List<String>) value.get();
        return eb.startStrList(Component.literal(key), currentValue)
                .setTooltip(Component.literal(key))
                .setSaveConsumer(newList -> value.set((List<? extends String>) newList))
                .build();
    }
    private static IntegerSliderEntry intSlider(ConfigEntryBuilder eb,
                                                String key,
                                                int min,
                                                int max,
                                                ForgeConfigSpec.IntValue value) {
        return eb.startIntSlider(Component.literal(key), value.get(), min, max)
                .setTooltip(Component.literal(key))
                .setSaveConsumer(value::set)
                .build();
    }

    private static DoubleListEntry doubleField(ConfigEntryBuilder eb,
                                               String key,
                                               double min,
                                               double max,
                                               ForgeConfigSpec.DoubleValue value) {
        return eb.startDoubleField(Component.literal(key), value.get())
                .setMin(min).setMax(max)
                .setTooltip(Component.literal(key))
                .setSaveConsumer(value::set)
                .build();
    }

    private GUIEN() {}
}