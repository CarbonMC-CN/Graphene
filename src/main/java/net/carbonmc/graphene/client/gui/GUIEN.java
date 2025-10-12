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
                .setTitle(Component.literal("⚙ Graphene Config"))
                .transparentBackground()
                .setSavingRunnable(CoolConfig.SPEC::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory compat = builder.getOrCreateCategory(Component.literal("High-Version Optimization Port"));
        compat.addEntry(bool(eb, "Fix Pearl Leak", CoolConfig.FIX_PEARL_LEAK));
        compat.addEntry(bool(eb, "Fix Projectile Lerp", CoolConfig.FIX_PROJECTILE_LERP));
        ConfigCategory render = builder.getOrCreateCategory(Component.literal("Rendering Optimization"));
        render.addEntry(bool(eb, "Skip Outline When No Glowing", CoolConfig.skipOutlineWhenNoGlowing));
        SubCategoryBuilder fps = eb.startSubCategory(Component.literal("Fps"));
        fps.add(bool(eb, "Reduce Render Latency", CoolConfig.fpsoo));
        fps.add(intSlider(eb, "Fill in the screen refresh rate", 10, 360, CoolConfig.UDT));
        render.addEntry(fps.build());
        SubCategoryBuilder chest = eb.startSubCategory(Component.literal("Chest Rendering Optimization"));
        chest.add(bool(eb, "Enable Optimization", CoolConfig.ENABLE_OPTIMIZATION));
        chest.add(enumOpt(eb, "Render Mode", CoolConfig.RenderMode.class, CoolConfig.RENDER_MODE));
        chest.add(bool(eb, "Optimize Ender Chests", CoolConfig.OPTIMIZE_ENDER_CHESTS));
        chest.add(bool(eb, "Optimize Trapped Chests", CoolConfig.OPTIMIZE_TRAPPED_CHESTS));
        chest.add(intSlider(eb, "Max Render Distance", 1, 128, CoolConfig.MAX_RENDER_DISTANCE));
        render.addEntry(chest.build());
        SubCategoryBuilder leaf = eb.startSubCategory(Component.literal("Leaf Optimization"));
        leaf.add(bool(eb, "Advanced Leaf Culling", CoolConfig.useAdvancedLeafCulling));
        leaf.add(intSlider(eb, "Min Leaf Connections", 1, 6, CoolConfig.minLeafConnections));
        leaf.add(bool(eb, "Optimize Mangrove", CoolConfig.OPTIMIZE_MANGROVE));
        render.addEntry(leaf.build());

        ConfigCategory reflex = builder.getOrCreateCategory(Component.literal("Reflex Low Latency"));
        reflex.addEntry(bool(eb, "Enable Reflex", CoolConfig.enableReflex));
        reflex.addEntry(longField(eb, "Reflex Offset (ns)",
                -1_000_000L, 1_000_000L, CoolConfig.reflexOffsetNs));
        reflex.addEntry(intSlider(eb, "Reflex FPS Cap", 0, 1000, CoolConfig.MAX_FPS));

        ConfigCategory particle = builder.getOrCreateCategory(Component.literal("Particle Optimization"));
        particle.addEntry(bool(eb, "Enable Particle Optimization", CoolConfig.ENABLE_PARTICLE_OPTIMIZATION));

        SubCategoryBuilder lod = eb.startSubCategory(Component.literal("LOD System"));
        lod.add(bool(eb, "Enable Particle LOD", CoolConfig.ENABLE_PARTICLE_LOD));
        lod.add(doubleField(eb, "LOD Distance Threshold", 4, 64, CoolConfig.LOD_DISTANCE_THRESHOLD));
        lod.add(doubleField(eb, "LOD Reduction Factor", 0, 1, CoolConfig.LOD_REDUCTION_FACTOR));
        particle.addEntry(lod.build());

        SubCategoryBuilder timestep = eb.startSubCategory(Component.literal("Time Step"));
        timestep.add(bool(eb, "Enable Fixed Time Step", CoolConfig.ENABLE_FIXED_TIMESTEP));
        timestep.add(doubleField(eb, "Time Step Interval", 0.001, 0.1, CoolConfig.FIXED_TIMESTEP_INTERVAL));
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
        light.addEntry(bool(eb, "Bamboo Lighting Optimization - Skip needless lighting calc for bamboo", CoolConfig.BambooLight));

        ConfigCategory entity = builder.getOrCreateCategory(Component.literal("Entity Optimization"));
         entity.addEntry(bool(eb, "Entity Tick Optimization", CoolConfig.optimizeEntities));

        entity.addEntry(intSlider(eb, "Horizontal Range", 1, 256, CoolConfig.horizontalRange));
        entity.addEntry(intSlider(eb, "Vertical Range", 1, 256, CoolConfig.verticalRange));
        entity.addEntry(bool(eb, "Ignore Dead Entities", CoolConfig.ignoreDeadEntities));
        entity.addEntry(stringList(eb, "EntityWhiteList", CoolConfig.entityWhitelist));
        entity.addEntry(bool(eb, "Clean Up Dead Entities", CoolConfig.OPTIMIZE_ENTITY_CLEANUP));
        entity.addEntry(bool(eb, "Keep Raiders Ticking During Raid", CoolConfig.tickRaidersInRaid));
        ConfigCategory item = builder.getOrCreateCategory(Component.literal("Item Optimization"));
        item.addEntry(bool(eb, "Enable Item Optimization", CoolConfig.OpenIO));
        item.addEntry(intSlider(eb, "Max Stack Size", -1, 9999, CoolConfig.maxStackSize));
        item.addEntry(doubleField(eb, "Merge Radius", 0.1, 10, CoolConfig.mergeDistance));
        item.addEntry(bool(eb, "Lock Maxed Stacks", CoolConfig.lockMaxedStacks));
        item.addEntry(intSlider(eb, "List Mode", 0, 2, CoolConfig.listMode));
        item.addEntry(bool(eb, "Show Stack Count", CoolConfig.showStackCount));
        item.addEntry(bool(eb, "Enable Custom Stack", CoolConfig.ENABLED));
        item.addEntry(intSlider(eb, "Custom Max Stack", 1, 9999, CoolConfig.MAX_STACK_SIZE));
        item.addEntry(bool(eb, "Item Entity Tick Optimization", CoolConfig.optimizeItems));

        ConfigCategory mem = builder.getOrCreateCategory(Component.literal("Memory Optimization"));
        mem.addEntry(bool(eb, "Memory Leak Fix - AE2WTLibCreativeTabLeakFix", CoolConfig.MemoryLeakFix_AE2WTLibCreativeTabLeakFix));
        mem.addEntry(bool(eb, "Memory Leak Fix - ScreenshotByteBufferLeakFix", CoolConfig.MemoryLeakFix_ScreenshotByteBufferLeakFix));
        ConfigCategory debug = builder.getOrCreateCategory(Component.literal("Debug"));
        debug.addEntry(bool(eb, "Debug Logging", CoolConfig.DEBUG_LOGGING));

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
                .setSaveConsumer(value::set)
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