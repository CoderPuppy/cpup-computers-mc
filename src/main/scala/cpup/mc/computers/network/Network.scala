package cpup.mc.computers.network

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.CPupComputers
import cpup.mc.lib.inspecting.Registry.IDed
import cpup.mc.lib.util.{Side, TickUtil}
import cpw.mods.fml.common.gameevent.TickEvent

class Network(__nodes: Node*) extends IDed {
	override def typ = s"${CPupComputers.ref.modID}:network"
//	println("creating network", uuid)

	protected[network] val _nodes = mutable.Set[Node]()
	def nodes = _nodes.toSet

	protected[network] val _buses = mutable.Map[ru.TypeTag[_ <: Bus], Bus]()
	def buses = _buses.toMap

	def bus[T <: Bus](implicit tt: ru.TypeTag[T], create: (Network) => T) = _buses.getOrElseUpdate(tt, create(this)).asInstanceOf[T]

	protected[network] val _connectors = mutable.Map[Network, mutable.MultiMap[ru.TypeTag[_ <: Bus], Connector[_ <: Bus]]]()
	def connectors = _connectors.toMap.map(kv => (kv._1, kv._2.toMap))

	__nodes.foreach(_take)

	protected[network] def _take(node: Node) {
		if(_nodes.contains(node)) {
			if(node._network != this) throw new RuntimeException("bad")
			return
		}
		val oldNetwork = node._network
//		if(oldNetwork != null)
//			println(s"moving ${node.uuid} to $uuid from ${oldNetwork.uuid}")
//		else
//			println(s"adding ${node.uuid} to $uuid")
		if(oldNetwork != null) oldNetwork._remove(node)
		_nodes += node
		node._network = this
		node.onJoin(this)
		for(_node <- _nodes if node != _node) {
			node.onConnect(_node)
			_node.onConnect(node)
		}
		_buses.values.foreach(_.onJoin(node))
		node match {
			case n: Connector.Node[_] => {
				val connector = n.connector
				val other = connector.other(n)
				val sConnectors = _connectors
					.getOrElseUpdate(connector.other(node).network, {
					new mutable.HashMap[ru.TypeTag[_ <: Bus], mutable.Set[Connector[_ <: Bus]]] with mutable.MultiMap[ru.TypeTag[_ <: Bus], Connector[_ <: Bus]]
				})
					.addBinding(connector.busType, connector)
				nodes.foreach(_.onConnect(connector))
				_buses.values.foreach(_.onConnect(connector))
				if(sConnectors(connector.busType).size == 1) {
					nodes.foreach(_.onConnect(other.network))
					_buses.values.foreach(_.onConnect(other.network))
				}
			}

			case _ =>
		}
	}

	protected[network] def _connect(a: Node, b: Node) {
//		println(s"connecting ${a.uuid} and ${b.uuid}")
		if(a.network != this && b.network != this) throw new RuntimeException("neither node is in this network")
		if(a.network != b.network) combine(if(a.network == this) b.network else a.network)
	}

	protected[network] val _splitNodes = mutable.Set.empty[Node]

	protected[network] def _disconnect(a: Node, b: Node) {
//		println(s"trying to split ${a.uuid} and ${b.uuid}")
		if(_splitNodes.isEmpty)
			TickUtil.register(TickEvent.Type.SERVER, TickEvent.Phase.END, Side.SERVER, () => {
				split(Network.findNetworks(_splitNodes.toSeq: _*): _*)
				_splitNodes.clear
				()
			})
		_splitNodes += b
		_splitNodes += a
	}

	protected[network] def _remove(node: Node) {
//		println(s"removing ${node.uuid} from $uuid")
		_nodes -= node
		node._network = null
		for(_node <- _nodes) {
			node.onDisconnect(_node)
			_node.onDisconnect(node)
		}
		node.onLeave(this)
		node match {
			case n: Connector.Node[_] => {
				val connector = n.connector
				val other = connector.other(n)
				val sConnectors = _connectors
					.getOrElseUpdate(connector.other(n).network, {
						new mutable.HashMap[ru.TypeTag[_ <: Bus], mutable.Set[Connector[_ <: Bus]]] with mutable.MultiMap[ru.TypeTag[_ <: Bus], Connector[_ <: Bus]]
					})
					.removeBinding(connector.busType, connector)
				nodes.foreach(_.onDisconnect(connector))
				_buses.values.foreach(_.onDisconnect(connector))
				if(sConnectors(connector.busType).isEmpty) {
					nodes.foreach(_.onDisconnect(other.network))
					_buses.values.foreach(_.onDisconnect(other.network))
				}
			}

			case _ =>
		}
		_buses.values.foreach(_.onLeave(node))
	}

	def combine(other: Network) {
//		println(s"combining $uuid and ${other.uuid}")
		for(node <- other._nodes.toList) {
			_take(node)
		}
	}

	// NOTE: this is very similar to OpenComputer's Network#handleSplit
	def split(nodeSets: Set[Node]*) = {
		if(nodeSets.isEmpty) throw new RuntimeException("cannot split into 0 networks")
		_nodes.clear
		_nodes ++= nodeSets.head
		val nets = this :: nodeSets.tail.map(set => new Network(set.toSeq: _*)).toList
//		println(s"split into ${nets.map(net => s"${net.uuid}(${net.nodes.map(_.uuid).mkString(", ")})").mkString(", ")}")
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
		}).filter(_.nonEmpty)
	}
}
