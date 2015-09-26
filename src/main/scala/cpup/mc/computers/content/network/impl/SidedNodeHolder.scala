package cpup.mc.computers.content.network.impl

import cpup.mc.lib.util.Direction

trait SidedNodeHolder {
	def node(dir: Direction): Option[Node]
}
