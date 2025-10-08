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
                .setTitle(Component.literal("⚙ Graphene 配置"))
                .transparentBackground()
                .setSavingRunnable(CoolConfig.SPEC::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory compat = builder.getOrCreateCategory(Component.literal("高版本优化移植"));
        compat.addEntry(bool(eb, "修复珍珠泄漏", CoolConfig.FIX_PEARL_LEAK));
        compat.addEntry(bool(eb, "修复抛射物插值", CoolConfig.FIX_PROJECTILE_LERP));
        ConfigCategory render = builder.getOrCreateCategory(Component.literal("渲染优化"));
        render.addEntry(bool(eb, "无发光实体时跳过轮廓", CoolConfig.skipOutlineWhenNoGlowing));
        SubCategoryBuilder fps = eb.startSubCategory(Component.literal("降低渲染延迟"));
        fps.add(bool(eb, "启用优化", CoolConfig.fpsoo));
        render.addEntry(fps.build());

        SubCategoryBuilder chest = eb.startSubCategory(Component.literal("箱子渲染优化"));
        chest.add(bool(eb, "启用优化", CoolConfig.ENABLE_OPTIMIZATION));
        chest.add(enumOpt(eb, "渲染模式", CoolConfig.RenderMode.class, CoolConfig.RENDER_MODE));
        chest.add(bool(eb, "优化末影箱", CoolConfig.OPTIMIZE_ENDER_CHESTS));
        chest.add(bool(eb, "优化陷阱箱", CoolConfig.OPTIMIZE_TRAPPED_CHESTS));
        chest.add(intSlider(eb, "最大渲染距离", 1, 128, CoolConfig.MAX_RENDER_DISTANCE));
        render.addEntry(chest.build());

        ConfigCategory reflex = builder.getOrCreateCategory(Component.literal("Reflex 低延迟"));
        reflex.addEntry(bool(eb, "启用 Reflex", CoolConfig.enableReflex));
        reflex.addEntry(longField(eb, "Reflex 偏移 (ns)",
                -1_000_000L, 1_000_000L, CoolConfig.reflexOffsetNs));
        reflex.addEntry(intSlider(eb, "Reflex 帧率上限", 0, 1000, CoolConfig.MAX_FPS));

        SubCategoryBuilder cull = eb.startSubCategory(Component.literal("高级剔除"));
        cull.add(bool(eb, "启用剔除", CoolConfig.ENABLEDCULL));
        cull.add(intSlider(eb, "剔除深度", 1, 5, CoolConfig.CULLING_DEPTH));
        cull.add(doubleField(eb, "随机剔除率", 0, 1, CoolConfig.REJECTION_RATE));
        render.addEntry(cull.build());

        SubCategoryBuilder trace = eb.startSubCategory(Component.literal("路径追踪"));
        trace.add(intSlider(eb, "追踪线程数", 1, 8, CoolConfig.tracingThreads));
        trace.add(doubleField(eb, "追踪距离", 1, 16, CoolConfig.traceDistance));
        trace.add(doubleField(eb, "回退距离", 4, 32, CoolConfig.fallbackDistance));
        render.addEntry(trace.build());

        SubCategoryBuilder leaf = eb.startSubCategory(Component.literal("树叶优化"));
        leaf.add(bool(eb, "高级树叶剔除", CoolConfig.useAdvancedLeafCulling));
        leaf.add(intSlider(eb, "最小树叶连接", 1, 6, CoolConfig.minLeafConnections));
        leaf.add(bool(eb, "优化红树林", CoolConfig.OPTIMIZE_MANGROVE));
        render.addEntry(leaf.build());

        ConfigCategory particle = builder.getOrCreateCategory(Component.literal("粒子优化"));
        particle.addEntry(bool(eb, "启用粒子优化", CoolConfig.ENABLE_PARTICLE_OPTIMIZATION));

        SubCategoryBuilder lod = eb.startSubCategory(Component.literal("LOD 系统"));
        lod.add(bool(eb, "启用粒子LOD", CoolConfig.ENABLE_PARTICLE_LOD));
        lod.add(doubleField(eb, "LOD距离阈值", 4, 64, CoolConfig.LOD_DISTANCE_THRESHOLD));
        lod.add(doubleField(eb, "LOD减少因子", 0, 1, CoolConfig.LOD_REDUCTION_FACTOR));
        particle.addEntry(lod.build());

        SubCategoryBuilder timestep = eb.startSubCategory(Component.literal("时间步长"));
        timestep.add(bool(eb, "启用固定时间步长", CoolConfig.ENABLE_FIXED_TIMESTEP));
        timestep.add(doubleField(eb, "时间步长间隔", 0.001, 0.1, CoolConfig.FIXED_TIMESTEP_INTERVAL));
        particle.addEntry(timestep.build());

        SubCategoryBuilder lists = eb.startSubCategory(Component.literal("粒子列表"));
        lists.add(stringList(eb, "LOD白名单", CoolConfig.LOD_PARTICLE_WHITELIST));
        lists.add(stringList(eb, "LOD黑名单", CoolConfig.LOD_PARTICLE_BLACKLIST));
        particle.addEntry(lists.build());

        SubCategoryBuilder inactive = eb.startSubCategory(Component.literal("非活动状态优化"));
        inactive.add(bool(eb, "窗口失焦降帧率", CoolConfig.REDUCE_FPS_WHEN_INACTIVE));
        inactive.add(intSlider(eb, "失焦FPS上限", 5, 60, CoolConfig.INACTIVE_FPS_LIMIT));
        inactive.add(bool(eb, "失焦降视距", CoolConfig.REDUCE_RENDER_DISTANCE_WHEN_INACTIVE));
        inactive.add(intSlider(eb, "失焦视距", 2, 12, CoolConfig.INACTIVE_RENDER_DISTANCE));
        render.addEntry(inactive.build());

        ConfigCategory light = builder.getOrCreateCategory(Component.literal("光照性能优化"));
        light.addEntry(bool(eb, "光照优化", CoolConfig.ENABLE_FIXED_LIGHT));
        light.addEntry(bool(eb, "竹子光照优化-取消竹子无意义的光照计算", CoolConfig.BambooLight));

        ConfigCategory math = builder.getOrCreateCategory(Component.literal("数学性能"));
        ConfigCategory entity = builder.getOrCreateCategory(Component.literal("实体优化"));
        entity.addEntry(bool(eb, "禁用实体碰撞", CoolConfig.disableEntityCollisions));
        entity.addEntry(bool(eb, "实体Tick优化", CoolConfig.optimizeEntities));
        entity.addEntry(intSlider(eb, "水平范围", 1, 256, CoolConfig.horizontalRange));
        entity.addEntry(intSlider(eb, "垂直范围", 1, 256, CoolConfig.verticalRange));
        entity.addEntry(bool(eb, "忽略死亡实体", CoolConfig.ignoreDeadEntities));
        entity.addEntry(bool(eb, "清理死亡实体", CoolConfig.OPTIMIZE_ENTITY_CLEANUP));
        entity.addEntry(bool(eb, "袭击时保持袭击者Tick", CoolConfig.tickRaidersInRaid));
        ConfigCategory item = builder.getOrCreateCategory(Component.literal("物品优化"));
        item.addEntry(bool(eb, "启用物品优化", CoolConfig.OpenIO));
        item.addEntry(intSlider(eb, "最大堆叠", -1, 9999, CoolConfig.maxStackSize));
        item.addEntry(doubleField(eb, "合并半径", 0.1, 10, CoolConfig.mergeDistance));
        item.addEntry(bool(eb, "掉落物锁", CoolConfig.lockMaxedStacks));
        item.addEntry(intSlider(eb, "列表模式", 0, 2, CoolConfig.listMode));
        item.addEntry(bool(eb, "显示堆叠数", CoolConfig.showStackCount));
        item.addEntry(bool(eb, "启用自定义堆叠", CoolConfig.ENABLED));
        item.addEntry(intSlider(eb, "自定义最大堆叠", 1, 9999, CoolConfig.MAX_STACK_SIZE));
        item.addEntry(bool(eb, "物品实体Tick优化", CoolConfig.optimizeItems));

        ConfigCategory mem = builder.getOrCreateCategory(Component.literal("内存优化"));
        mem.addEntry(intSlider(eb, "清理间隔(秒)", 60, 3600, CoolConfig.MEMORY_CLEAN_INTERVAL));
        mem.addEntry(bool(eb, "GC触发", CoolConfig.ENABLE_GC));
        mem.addEntry(bool(eb, "内存泄漏修复_AE2WTLibCreativeTabLeakFix", CoolConfig.MemoryLeakFix_AE2WTLibCreativeTabLeakFix));

        mem.addEntry(bool(eb, "内存泄漏修复_ScreenshotByteBufferLeakFix", CoolConfig.MemoryLeakFix_ScreenshotByteBufferLeakFix));
        ConfigCategory chunk = builder.getOrCreateCategory(Component.literal("区块优化"));
        chunk.addEntry(bool(eb, "优化群系生成", CoolConfig.OPTIMIZE_BIOME_GENERATION));
        chunk.addEntry(bool(eb, "主动卸载区块-仅低性能机开启", CoolConfig.aggressiveChunkUnloading));
        chunk.addEntry(intSlider(eb, "区块卸载延迟(秒)", 4, 64, CoolConfig.chunkUnloadDelay));
        chunk.addEntry(bool(eb, "是否启用CTU(启用后请勿在启动时关闭)", CoolConfig.CTU));
        chunk.addEntry(intSlider(eb, "线程数", 2, 128, CoolConfig.CHUNKTHREADS));
        chunk.addEntry(intSlider(eb, "IO线程数", 2, 256, CoolConfig.CHUNKIO_THREADS));
        chunk.addEntry(intSlider(eb, "CPU最大队列数", 3072, 8192, CoolConfig.CPU_QUEUE));
        chunk.addEntry(intSlider(eb, "IO最大队列数", 1536, 8192, CoolConfig.IO_QUEUE));
        chunk.addEntry(bool(eb, "区块优化-IO", CoolConfig.CHUNK_REDIRECT_IO));
        chunk.addEntry(bool(eb, "区块优化-LIGHT", CoolConfig.CHUNk_REDIRECT_LIGHTING));
        ConfigCategory async = builder.getOrCreateCategory(Component.literal("异步优化"));
        ConfigCategory debug = builder.getOrCreateCategory(Component.literal("调试"));
        debug.addEntry(bool(eb, "调试日志", CoolConfig.DEBUG_LOGGING));

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

    private ClothConfigScreenFactory() {}
}