package cpup.mc.computers.content

import cpup.mc.computers.CPupComputers
import cpup.mc.lib.CPupModHolder
import cpup.mc.lib.client.CPupGUI
import cpup.mc.lib.content.CPupItem
import net.minecraft.client.gui.GuiScreen
import net.minecraft.inventory.Container

trait BaseGUI extends CPupGUI[CPupComputers.type] with CPupModHolder[CPupComputers.type] {
	def mod = CPupComputers
}
