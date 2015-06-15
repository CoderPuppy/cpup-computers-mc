package cpup.mc.computers.network

import cpup.mc.lib.util.Direction

trait SidedNodeHolder {
	def node(dir: Direction): Option[Node]
}
