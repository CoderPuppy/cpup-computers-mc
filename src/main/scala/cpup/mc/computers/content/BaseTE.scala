package cpup.mc.computers.content

import cpup.mc.computers.CPupComputers
import cpup.mc.lib.CPupModHolder
import cpup.mc.lib.content.CPupTE

trait BaseTE extends CPupTE[CPupComputers.type] with CPupModHolder[CPupComputers.type] {
	def mod = CPupComputers
}
