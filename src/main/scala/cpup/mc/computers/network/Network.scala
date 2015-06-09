package cpup.mc.computers.network

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.network.component.Component

class Network(__nodes: Node*) {
	println("creating network", this)
	protected[network] val _nodes = mutable.Set[Node]()
	def nodes = _nodes.toSet

	protected[network] val _buses = mutable.Map[ru.TypeTag[_ <: Bus], Bus]()
	def buses = _buses.toMap

	def bus[T <: Bus](implicit tt: ru.TypeTag[T], create: (Network) => T) = _buses.getOrElseUpdate(tt, create(this)).asInstanceOf[T]

	__nodes.foreach(_take)

	protected[network] def _take(node: Node) {
		if(_nodes.contains(node)) return
		val oldNetwork = node._network
		println(this, "taking", node.uuid, oldNetwork)
		if(oldNetwork != null) oldNetwork._remove(node)
		_nodes += node
		node._network = this
		node.onJoin(this)
		for(_node <- _nodes if node != _node) {
			node.onConnect(_node)
			_node.onConnect(node)
		}
	}

	protected[network] def _connect(a: Node, b: Node) {
		if(a.network != this && b.network != this) throw new RuntimeException("neither node is in this network")
		if(a.network != b.network) combine(if(a.network == this) b.network else a.network)
	}

	protected[network] def _disconnect(a: Node, b: Node) {
		split(Network.findNetworks(a, b): _*)
	}

	protected[network] def _remove(node: Node) {
		println("removing", node.uuid, this)
		_nodes -= node
		node._network = null
		for(_node <- _nodes) {
			node.onDisconnect(_node)
			_node.onDisconnect(node)
		}
		node.onLeave(this)
	}

	def combine(other: Network) {
		for(node <- other._nodes) _take(node)
	}

	// NOTE: this is very similar to OpenComputer's Network#handleSplit
	def split(nodeSets: Set[Node]*) = {
		if(nodeSets.isEmpty) throw new RuntimeException("cannot split into 0 networks")
		_nodes.clear
		_nodes ++= nodeSets.head
		val nets = this :: nodeSets.tail.map(set => new Network(set.toSeq: _*)).toList
		nets
	}
}

object Network {
	// NOTE: this is very similar to part of OpenComputer's Network.searchGraphs
	def findNetworks(seeds: Node*) = {
		val claimed = mutable.Set[Node]()
		seeds.flatMap(seed => {
			if(claimed.contains(seed)) None
			else {
				val net = seed.visibleNodes.toSet
				claimed ++= net
				Some(net)
			}
		})
	}
}
