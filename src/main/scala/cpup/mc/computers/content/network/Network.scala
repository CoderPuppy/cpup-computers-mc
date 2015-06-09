package cpup.mc.computers.content.network

import com.typesafe.config.Config
import cpup.lib.module.{ModuleLoader, ModuleID}
import cpup.mc.computers.CPupComputers
import cpup.mc.lib.ModLifecycleHandler
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.registry.GameRegistry
import org.slf4j.Logger

@ModuleID(id = "network")
class Network(logger: Logger, config: Config) extends ModLifecycleHandler {
	lazy val waila = ModuleLoader.load[Waila](this).left.toOption

	lazy val mod = CPupComputers
	override def preInit(e: FMLPreInitializationEvent) {
		GameRegistry.registerBlock(Cable, Cable.name)
		GameRegistry.registerTileEntity(classOf[Cable.TE], Cable.name)
	}

	override def init(e: FMLInitializationEvent) {
		waila.foreach(_.init(e))
	}
}
