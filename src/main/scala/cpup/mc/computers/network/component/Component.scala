package cpup.mc.computers.network.component

import cpup.mc.computers.network.Node

trait Component {
	def node: Node
	def slot: Int
	def typ: String
	def methods: Map[String, Method]
}
