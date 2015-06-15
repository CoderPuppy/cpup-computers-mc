package cpup.mc.computers.content.computers

import com.typesafe.config.Config
import cpup.lib.module.{CanLoad, ModuleID}
import cpup.mc.computers.content.network.Network
import org.slf4j.Logger

@ModuleID(id = "computers")
@CanLoad.ModuleProvided(moduleType = classOf[Network])
class Computers(config: Config, logger: Logger, network: Network) {

}
