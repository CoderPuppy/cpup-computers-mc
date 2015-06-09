package cpup.mc.computers.content

import cpup.mc.computers.CPupComputers
import cpup.mc.lib.CPupModHolder
import cpup.mc.lib.content.CPupBlock

trait BaseBlock extends CPupBlock[CPupComputers.type] with CPupModHolder[CPupComputers.type] {
	def mod = CPupComputers
}
