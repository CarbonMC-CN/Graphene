package net.carbonmc.graphene;

import com.google.common.collect.Queues;
import net.carbonmc.graphene.async.chunk.ChunkPool;
import net.carbonmc.graphene.client.GrapheneClient;
import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.gl.CleanerRunnable;
import net.carbonmc.graphene.optimization.chunk.AES;
import net.carbonmc.graphene.optimization.chunk.RE;
import net.carbonmc.graphene.command.KillMobsCommand;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;

import java.io.File;
import java.lang.ref.Cleaner;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Mod("graphene")
public class Graphene {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final String MODID = "graphene";
	public static final String VERSION = "1.7.6";
	public static volatile boolean OpenC = false;
	private ExecutorService executorService;
	private static final ConcurrentLinkedQueue<CleanerRunnable> PENDING
			= Queues.newConcurrentLinkedQueue();

	private static final Cleaner CLEANER = Cleaner.create();
	public static final ChunkPool CKU = new ChunkPool();
	public static final com.github.benmanes.caffeine.cache.Cache<ChunkPos,int[]> CHUNK_CACHE =
			com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
					.maximumSize(1024)
					.expireAfterAccess(5, TimeUnit.MINUTES)
					.build();
	public Graphene() {
		var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		var forgeEventBus = MinecraftForge.EVENT_BUS;
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CoolConfig.SPEC);
		forgeEventBus.register(this);
		modEventBus.addListener(this::setup);
		MixinBootstrap.init();
		modEventBus.addListener(this::commonSetup);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
		if (FMLEnvironment.dist == Dist.CLIENT) {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent evt) -> {
			if (evt.phase == TickEvent.Phase.END) {
				onEndClientTick();
			}
		});}


		LOGGER.info("Initializing Graphene MOD v{}", VERSION);

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> GrapheneClient::init);
		ModLoadingContext.get().registerExtensionPoint(
				IExtensionPoint.DisplayTest.class,
				() -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true)
		);
	}
	private void commonSetup(FMLCommonSetupEvent event) {
		CKU.a();
		int processors = Runtime.getRuntime().availableProcessors();
		executorService = Executors.newWorkStealingPool(Math.max(2, processors / 2));
		LOGGER.info("Registered async executor for Optimized Abs with {} threads",
				((java.util.concurrent.ForkJoinPool) executorService).getParallelism());
	}

	private void onServerStarted(ServerStartedEvent event) {
		CHUNK_CACHE.invalidateAll();
		CKU.v();
		OpenC = true;
	}

	private void onEndClientTick() {
		int drained = 0;
		while (!PENDING.isEmpty() && drained++ < 16) {
			CleanerRunnable r = PENDING.poll();
			if (r != null) r.run();
		}
	}
	private void onServerStopping(ServerStoppingEvent event) {
		LOGGER.info("Shutting down Graphene systems...");
		OpenC = false;
		if (CKU != null && !CKU.isShuttingDown()) {
			CKU.shutdownGracefully();
		}
		RE.shutdown();
		AES.cleanupSharedExecutors();
		if (executorService != null && !executorService.isShutdown()) {
			try {
				executorService.shutdown();
				if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
					List<Runnable> remaining = executorService.shutdownNow();
					if (!remaining.isEmpty()) {
						LOGGER.debug("Force shutdown {} remaining tasks", remaining.size());
					}
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
		if (CKU != null) {
			try {
				CKU.awaitShutdown();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		CHUNK_CACHE.invalidateAll();
		LOGGER.info("Graphene shutdown complete");
	}

	private void setup(final FMLCommonSetupEvent event) {
		LOGGER.info("Graphene Mod 初始化完成");
	}


	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		KillMobsCommand.register(event.getDispatcher());
	}}