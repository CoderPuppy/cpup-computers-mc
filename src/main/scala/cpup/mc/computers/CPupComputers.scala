package cpup.mc.computers

import cpup.mc.computers.content.Content
import cpup.mc.computers.content.computers.{Computers, DebugConsole}
import cpup.mc.computers.content.network.{Network, NodeMessage}
import cpup.mc.lib.CPupMod
import cpup.mc.lib.client.CPupGUIManager
import cpup.mc.lib.network.CPupNetwork
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.FMLPreInitializationEvent

@Mod(modid = Ref.modID, modLanguage = "scala", dependencies = "required-after:cpup-mc")
object CPupComputers extends CPupMod[Ref.type] {
	override def ref = Ref
	override final val content = Content

	lazy val gui = new CPupGUIManager[this.type](this, List(DebugConsole.GUI))

	@EventHandler
	override def preInit(e: FMLPreInitializationEvent) {
		super.preInit(e)
		gui.register
	}

	val net = new CPupNetwork[AnyRef](ref.modID, new AnyRef, CPupNetwork.simpleNetwork[this.type](this), Set(
		classOf[NodeMessage[AnyRef]]
	))


	loadModule[Network]
	loadModule[Computers]
}
