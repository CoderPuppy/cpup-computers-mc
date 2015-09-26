package cpup.mc.computers.content.network.impl

import cpup.mc.lib.util.Direction

trait NodeHolder extends SidedNodeHolder {
	def node: Node
	override def node(dir: Direction) = Some(node)
}
