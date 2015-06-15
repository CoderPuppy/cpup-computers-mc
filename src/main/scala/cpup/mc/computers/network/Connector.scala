package cpup.mc.computers.network

import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.network.{Node => NNode}

trait Connector[B <: Bus] {
	def nodeA: Node
	def nodeB: Node
	def busType: ru.TypeTag[B]

	def other(node: Node) = {
		if(node != nodeA && node != nodeB) throw new RuntimeException("this isn't either of the nodes")
		if(node == nodeA) nodeB else nodeA
	}
	def other(net: Network) = {
		if(net != nodeA.network && net != nodeB.network) throw new RuntimeException("this isn't either of the networks")
		if(net == nodeA.network) nodeB else nodeA
	}
}

object Connector {
	trait Node[B <: Bus] extends NNode {
		def connector: Connector[B]
	}
}
