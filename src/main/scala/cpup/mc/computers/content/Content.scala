package cpup.mc.computers.content

import cpup.mc.computers.CPupComputers
import cpup.mc.lib.content.CPupContent
import cpw.mods.fml.common.event.FMLPreInitializationEvent

object Content extends CPupContent[CPupComputers.type] {
	override def mod = CPupComputers
}
