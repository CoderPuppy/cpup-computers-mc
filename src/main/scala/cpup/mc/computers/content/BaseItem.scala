package cpup.mc.computers.content

import cpup.mc.computers.CPupComputers
import cpup.mc.lib.CPupModHolder
import cpup.mc.lib.content.{CPupItem, CPupBlock}

trait BaseItem extends CPupItem with CPupModHolder[CPupComputers.type] {
	def mod = CPupComputers
}
