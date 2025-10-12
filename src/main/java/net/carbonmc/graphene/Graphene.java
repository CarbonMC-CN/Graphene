package net.carbonmc.graphene;
import net.carbonmc.graphene.client.GrapheneClient;
import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.command.KillMobsCommand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;


@Mod("graphene")
public class Graphene {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final String MODID = "graphene";
	public static final String VERSION = "2.0.5";
	public Graphene() {
		var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		var forgeEventBus = MinecraftForge.EVENT_BUS;
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CoolConfig.SPEC);
		forgeEventBus.register(this);
		modEventBus.addListener(this::setup);
		MixinBootstrap.init();
		MinecraftForge.EVENT_BUS.addListener(this::onClientSetup);
		LOGGER.info("Initializing Graphene MOD v{}", VERSION);

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> GrapheneClient::init);
		ModLoadingContext.get().registerExtensionPoint(
				IExtensionPoint.DisplayTest.class,
				() -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true)
		);
	}

	public void onClientSetup(FMLClientSetupEvent event) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			GrapheneClient.stop();
		}));
	}

	private void setup(final FMLCommonSetupEvent event) {
		LOGGER.info("Graphene初始化完成");
	}


	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		KillMobsCommand.register(event.getDispatcher());
	}}