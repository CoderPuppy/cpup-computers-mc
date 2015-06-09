package cpup.mc.computers

import cpup.mc.computers.content.Content
import cpup.mc.computers.content.network.Network
import cpup.mc.lib.CPupMod
import cpw.mods.fml.common.Mod

@Mod(modid = Ref.modID, modLanguage = "scala", dependencies = "required-after:cpup-mc")
object CPupComputers extends CPupMod[Ref.type] {
	override def ref = Ref
	override final val content = Content

	loadModule[Network]
}
