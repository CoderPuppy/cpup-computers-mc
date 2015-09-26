package cpup.mc.computers.content.computers

import com.typesafe.config.Config
import cpup.lib.module.{CanLoad, ModuleID}
import cpup.mc.computers.content.network.{TestComponent, Network}
import cpup.mc.lib.ModLifecycleHandler
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.registry.GameRegistry
import org.slf4j.Logger

@ModuleID(id = "computers")
@CanLoad.ModuleProvided(moduleType = classOf[Network])
class Computers(config: Config, logger: Logger, network: Network) extends ModLifecycleHandler {
	override def preInit(e: FMLPreInitializationEvent) {
		GameRegistry.registerBlock(DebugConsole, DebugConsole.name)
		GameRegistry.registerTileEntity(classOf[DebugConsole.TE], DebugConsole.name)
	}
}
