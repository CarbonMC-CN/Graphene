// net.carbonmc.graphene.Graphene.java
package net.carbonmc.graphene;

import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.carbonmc.graphene.client.GrapheneClient;
import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.events.ModEventHandlers;
import net.carbonmc.graphene.particles.AsyncParticleHandler;
import net.carbonmc.graphene.util.KillMobsCommand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod("graphene")
public class Graphene {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final String MODID = "graphene";
	public static final String VERSION = "1.5.0";
	private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
	public static File Graphene_EVENTS_LOG = new File("log/graphene-event-debug.log");

	public Graphene() {
		var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		var forgeEventBus = MinecraftForge.EVENT_BUS;

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CoolConfig.SPEC);
		forgeEventBus.register(this);
		modEventBus.addListener(this::setup);
		MixinBootstrap.init();
		ModEventHandlers.register(modEventBus, forgeEventBus);
		modEventBus.addListener(AsyncSystemInitializer::init);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			AsyncEventSystem.shutdown();
			AsyncParticleHandler.shutdown();
		}));

		LOGGER.info("Initializing Graphene MOD v{}", VERSION);

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> GrapheneClient::init);

		ModLoadingContext.get().registerExtensionPoint(
				IExtensionPoint.DisplayTest.class,
				() -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true)
		);
	}

	private void setup(final FMLCommonSetupEvent event) {
		LOGGER.info("Graphene Mod 初始化完成");
		LOGGER.info("Graphene-CarbonMC官方QQ群：372378451");
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		event.enqueueWork(() -> {
			AsyncEventSystem.initialize();
			ModEventProcessor.processModEvents();
		});
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		KillMobsCommand.register(event.getDispatcher());
	}
}