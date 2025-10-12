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
public final class ClothConfigScreenFactory {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("graphene.gui.title"))
                .transparentBackground()
                .setSavingRunnable(CoolConfig.SPEC::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory compat = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.high"));
        compat.addEntry(bool(eb, "graphene.gui.name.high.fpleak", CoolConfig.FIX_PEARL_LEAK));
        compat.addEntry(bool(eb, "graphene.gui.name.high.fplerp", CoolConfig.FIX_PROJECTILE_LERP));

        ConfigCategory render = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.render"));
        render.addEntry(bool(eb, "graphene.gui.name.render.skip", CoolConfig.skipOutlineWhenNoGlowing));

        SubCategoryBuilder fps = eb.startSubCategory(Component.translatable("graphene.gui.title.render.fps"));
        fps.add(bool(eb, "graphene.gui.name.render.fps.fpsoo", CoolConfig.fpsoo));
        fps.add(intSlider(eb, "graphene.gui.name.render.fps.udt", 10, 360, CoolConfig.UDT));
        render.addEntry(fps.build());

        SubCategoryBuilder leaf = eb.startSubCategory(Component.translatable("graphene.gui.title.render.leaf"));
        leaf.add(bool(eb, "graphene.gui.name.render.leaf.ualc", CoolConfig.useAdvancedLeafCulling));
        leaf.add(intSlider(eb, "graphene.gui.name.render.leaf.mlc", 1, 6, CoolConfig.minLeafConnections));
        leaf.add(bool(eb, "graphene.gui.name.render.leaf.om", CoolConfig.OPTIMIZE_MANGROVE));
        render.addEntry(leaf.build());
        SubCategoryBuilder trace = eb.startSubCategory(Component.translatable("graphene.gui.title.render.trace"));
        trace.add(intSlider(eb, "graphene.gui.name.render.trace.tT", 1, 8, CoolConfig.tracingThreads));
        trace.add(doubleField(eb, "graphene.gui.name.render.trace.tD", 1, 16, CoolConfig.traceDistance));
        trace.add(doubleField(eb, "graphene.gui.name.render.trace.fbD", 4, 32, CoolConfig.fallbackDistance));
        render.addEntry(trace.build());

        SubCategoryBuilder chest = eb.startSubCategory(Component.translatable("graphene.gui.title.render.chest"));
        chest.add(bool(eb, "graphene.gui.name.render.chest.EO", CoolConfig.ENABLE_OPTIMIZATION));
        chest.add(enumOpt(eb, "graphene.gui.name.render.chest.RM", CoolConfig.RenderMode.class, CoolConfig.RENDER_MODE));
        chest.add(bool(eb, "graphene.gui.name.render.chest.OEC", CoolConfig.OPTIMIZE_ENDER_CHESTS));
        chest.add(bool(eb, "graphene.gui.name.render.chest.OTC", CoolConfig.OPTIMIZE_TRAPPED_CHESTS));
        chest.add(intSlider(eb, "graphene.gui.name.render.chest.MRD", 1, 128, CoolConfig.MAX_RENDER_DISTANCE));
        render.addEntry(chest.build());
        SubCategoryBuilder inactive = eb.startSubCategory(Component.translatable("graphene.gui.title.render.inactive"));
        inactive.add(bool(eb, "graphene.gui.name.render.inactive.RFWI", CoolConfig.REDUCE_FPS_WHEN_INACTIVE));
        inactive.add(intSlider(eb, "graphene.gui.name.render.inactive.IFL", 5, 60, CoolConfig.INACTIVE_FPS_LIMIT));
        inactive.add(bool(eb, "graphene.gui.name.render.inactive.RRDWI", CoolConfig.REDUCE_RENDER_DISTANCE_WHEN_INACTIVE));
        inactive.add(intSlider(eb, "graphene.gui.name.render.inactive.IRD", 2, 12, CoolConfig.INACTIVE_RENDER_DISTANCE));
        render.addEntry(inactive.build());
        ConfigCategory reflex = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.reflex"));
        reflex.addEntry(bool(eb, "graphene.gui.name.reflex.eR", CoolConfig.enableReflex));
        reflex.addEntry(longField(eb, "graphene.gui.name.reflex.rO",
                -1_000_000L, 1_000_000L, CoolConfig.reflexOffsetNs));
        reflex.addEntry(intSlider(eb, "graphene.gui.name.reflex.MF", 0, 1000, CoolConfig.MAX_FPS));


        ConfigCategory particle = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.particle"));
        particle.addEntry(bool(eb, "graphene.gui.name.particle.EPO", CoolConfig.ENABLE_PARTICLE_OPTIMIZATION));

        SubCategoryBuilder lod = eb.startSubCategory(Component.translatable("graphene.gui.title.particle.lod"));
        lod.add(bool(eb, "graphene.gui.name.particle.EPL", CoolConfig.ENABLE_PARTICLE_LOD));
        lod.add(doubleField(eb, "graphene.gui.name.particle.LDH", 4, 64, CoolConfig.LOD_DISTANCE_THRESHOLD));
        lod.add(doubleField(eb, "graphene.gui.name.particle.LRF", 0, 1, CoolConfig.LOD_REDUCTION_FACTOR));
        particle.addEntry(lod.build());

        SubCategoryBuilder timestep = eb.startSubCategory(Component.translatable("graphene.gui.title.particle.timestep"));
        timestep.add(bool(eb, "graphene.gui.name.particle.EFT", CoolConfig.ENABLE_FIXED_TIMESTEP));
        timestep.add(doubleField(eb, "graphene.gui.name.particle.FTI", 0.001, 0.1, CoolConfig.FIXED_TIMESTEP_INTERVAL));
        particle.addEntry(timestep.build());

        SubCategoryBuilder lists = eb.startSubCategory(Component.translatable("graphene.gui.title.particle.lists"));
        lists.add(stringList(eb, "graphene.gui.name.particle.LPW", CoolConfig.LOD_PARTICLE_WHITELIST));
        lists.add(stringList(eb, "graphene.gui.name.particle.LPB", CoolConfig.LOD_PARTICLE_BLACKLIST));
        particle.addEntry(lists.build());


        ConfigCategory light = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.light"));
        light.addEntry(bool(eb, "graphene.gui.name.light.EFL", CoolConfig.ENABLE_FIXED_LIGHT));
        light.addEntry(bool(eb, "graphene.gui.name.light.BL", CoolConfig.BambooLight));


        ConfigCategory entity = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.entity"));
        entity.addEntry(bool(eb, "graphene.gui.name.entity.oE", CoolConfig.optimizeEntities));
        entity.addEntry(intSlider(eb, "graphene.gui.name.entity.hR", 1, 256, CoolConfig.horizontalRange));
        entity.addEntry(intSlider(eb, "graphene.gui.name.entity.vR", 1, 256, CoolConfig.verticalRange));
        entity.addEntry(stringList(eb, "graphene.gui.name.entity.eW", CoolConfig.entityWhitelist));
        entity.addEntry(bool(eb, "graphene.gui.name.entity.iDE", CoolConfig.ignoreDeadEntities));
        entity.addEntry(bool(eb, "graphene.gui.name.entity.OEC", CoolConfig.OPTIMIZE_ENTITY_CLEANUP));
        entity.addEntry(bool(eb, "graphene.gui.name.entity.tRI", CoolConfig.tickRaidersInRaid));
        ConfigCategory item = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.item"));
        item.addEntry(bool(eb, "graphene.gui.name.item.OIO", CoolConfig.OpenIO));
        item.addEntry(intSlider(eb, "graphene.gui.name.item.mSS", -1, 9999, CoolConfig.maxStackSize));
        item.addEntry(doubleField(eb, "graphene.gui.name.item.mD", 0.1, 10, CoolConfig.mergeDistance));
        item.addEntry(bool(eb, "graphene.gui.name.item.lMS", CoolConfig.lockMaxedStacks));
        item.addEntry(intSlider(eb, "graphene.gui.name.item.lM", 0, 2, CoolConfig.listMode));
        item.addEntry(bool(eb, "graphene.gui.name.item.sSC", CoolConfig.showStackCount));
        item.addEntry(bool(eb, "graphene.gui.name.item.ENABLED", CoolConfig.ENABLED));
        item.addEntry(intSlider(eb, "graphene.gui.name.item.MSS", 1, 9999, CoolConfig.MAX_STACK_SIZE));
        item.addEntry(bool(eb, "graphene.gui.name.item.oI", CoolConfig.optimizeItems));

        ConfigCategory mem = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.mem"));
        mem.addEntry(bool(eb, "graphene.gui.name.mem.MLFAE", CoolConfig.MemoryLeakFix_AE2WTLibCreativeTabLeakFix));
        mem.addEntry(bool(eb, "graphene.gui.name.mem.MLFSB", CoolConfig.MemoryLeakFix_ScreenshotByteBufferLeakFix));
        ConfigCategory debug = builder.getOrCreateCategory(Component.translatable("graphene.gui.title.debug"));
        debug.addEntry(bool(eb, "graphene.gui.name.debug.DL", CoolConfig.DEBUG_LOGGING));

        return builder.build();
    }

    private static BooleanListEntry bool(ConfigEntryBuilder eb,
                                         String key,
                                         ForgeConfigSpec.BooleanValue value) {
        return eb.startBooleanToggle(Component.translatable(key), value.get())
                .setTooltip(Component.translatable(key))
                .setSaveConsumer(value::set)
                .build();
    }


    private static LongListEntry longField(ConfigEntryBuilder eb,
                                           String key,
                                           long min,
                                           long max,
                                           ForgeConfigSpec.LongValue value) {
        return eb.startLongField(Component.translatable(key), value.get())
                .setMin(min).setMax(max)
                .setTooltip(Component.translatable(key))
                .setSaveConsumer(value::set)
                .build();
    }


    private static <E extends Enum<E>> EnumListEntry<E> enumOpt(ConfigEntryBuilder eb,
                                                                String key,
                                                                Class<E> clazz,
                                                                ForgeConfigSpec.EnumValue<E> value) {
        return eb.startEnumSelector(Component.translatable(key), clazz, value.get())
                .setTooltip(Component.translatable(key))
                .setSaveConsumer(value::set)
                .build();
    }
    private static StringListListEntry stringList(ConfigEntryBuilder eb,
                                                  String key,
                                                  ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        List<String> currentValue = (List<String>) value.get();
        return eb.startStrList(Component.translatable(key), currentValue)
                .setTooltip(Component.translatable(key))
                .setSaveConsumer(value::set)
                .build();
    }
    private static IntegerSliderEntry intSlider(ConfigEntryBuilder eb,
                                                String key,
                                                int min,
                                                int max,
                                                ForgeConfigSpec.IntValue value) {
        return eb.startIntSlider(Component.translatable(key), value.get(), min, max)
                .setTooltip(Component.translatable(key))
                .setSaveConsumer(value::set)
                .build();
    }

    private static DoubleListEntry doubleField(ConfigEntryBuilder eb,
                                               String key,
                                               double min,
                                               double max,
                                               ForgeConfigSpec.DoubleValue value) {
        return eb.startDoubleField(Component.translatable(key), value.get())
                .setMin(min).setMax(max)
                .setTooltip(Component.translatable(key))
                .setSaveConsumer(value::set)
                .build();
    }

    private ClothConfigScreenFactory() {}
}