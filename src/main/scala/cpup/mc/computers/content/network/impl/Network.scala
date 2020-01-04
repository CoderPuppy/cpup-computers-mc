package cpup.mc.computers.content.network.impl

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.CPupComputers
import cpup.mc.lib.inspecting.Registry.IDed
import cpup.mc.lib.util.{Side, TickUtil}
import cpw.mods.fml.common.gameevent.TickEvent
import net.minecraft.client.Minecraft

class Network extends IDed {
	override def typ = s"${CPupComputers.ref.modID}:network"
//	println("creating network", uuid)

	protected[impl] val _nodes = mutable.Set[Node]()
	def nodes = _nodes.toSet

	protected[impl] val _buses = mutable.Map[ru.TypeTag[_ <: Bus], Bus]()
	def buses = _buses.toMap

	def bus[T <: Bus](implicit tt: ru.TypeTag[T], create: (Network) => T) = _buses.getOrElseUpdate(tt, create(this)).asInstanceOf[T]

	protected[impl] val _connectors = mutable.Map[Network, mutable.MultiMap[ru.TypeTag[_ <: Bus], Connector[_ <: Bus]]]()
	def connectors = _connectors.toMap.map(kv => (kv._1, kv._2.toMap))
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

	// if a.uuid == b.uuid then everything is broken
	def unifyNodePair(a: Node, b: Node) = if(a.uuid.toString > b.uuid.toString) (a, b) else (b, a)

	def connection(a: Node, b: Node, connected: Boolean) {
		if(connected) {
			if(a._network != b._network || a._network == null) {
				if(a._network != null) splitNets += a._network
				splitNodes += a
				if(b._network != null) splitNets += b._network
				splitNodes += b
			}
		} else {
			if(a._network == b._network || a._network == null || b._network == null) {
				if(a._network != null) splitNets += a._network
				splitNodes += a
				splitNodes += b
			}
		}
		directConnects.addBinding(a, (Node.RelSelector.Specific(Set(b)), connected))
		queueTick
	}

	def remove(node: Node) {
		directConnects.addBinding(node, (Node.RelSelector.Direct, false))
		netChanges(node) = () => Network.Change.Leave(node._network)
		indirectConnects.addBinding(node, (Node.RelSelector.Network, false))
		queueTick
	}

	val directConnects = new mutable.HashMap[Node, mutable.Set[(Node.RelSelector, Boolean)]] with mutable.MultiMap[Node, (Node.RelSelector, Boolean)]
	val splitNets = mutable.Set.empty[Network]
	val splitNodes = mutable.Set.empty[Node]
	val netChanges = mutable.Map.empty[Node, () => Network.Change]
	val indirectConnects = new mutable.HashMap[Node, mutable.Set[(Node.RelSelector, Boolean)]] with mutable.MultiMap[Node, (Node.RelSelector, Boolean)]
	val tickRun = mutable.Queue.empty[() => Unit]
	var tickQueued = false

	def runTick {
		val _directConnects = directConnects.flatMap { case (nodeA, ops) =>
				ops.flatMap { case (sel, connected) =>
						sel.nodes(nodeA).map { nodeB =>
							((nodeA, nodeB), connected)
						}
				}
		}
		for(((nodeA, nodeB), connected) <- _directConnects) {
			nodeA._connection(nodeB, connected)
			nodeB._connection(nodeA, connected)
		}
		directConnects.clear
		;{
			val nodeSets = findNetworks(splitNodes.toSeq: _*)
			val networks = splitNets.toSeq.take(Math.min(splitNets.size, nodeSets.size)) ++ List.fill[Network](Math.max(nodeSets.size - splitNets.size, 0)) { new Network() }
			for {
				((nodesA, netA), i) <- nodeSets.view.zip(networks).zipWithIndex
				nodeA <- nodesA
			} {
				netChanges(nodeA) = () => Network.Change(netA, nodeA._network)
				indirectConnects.addBinding(nodeA, (Node.RelSelector.Specific(nodesA), true))
				for(nodesB <- nodeSets.view.drop(i + 1)) {
					indirectConnects.addBinding(nodeA, (Node.RelSelector.Specific(nodesB), false))
				}
			}
		}
		splitNets.clear
		splitNodes.clear
		for((node, chg) <- netChanges) {
			val _chg = chg()
			for(net <- _chg.oldN) {
				net._nodes -= node
				for((_, bus) <- net._buses) {
					bus.onNodeChangeNet(node, _chg)
				}
			}
			for(net <- _chg.newN) {
				net._nodes += node
				for((_, bus) <- net._buses) {
					bus.onNodeChangeNet(node, _chg)
				}
			}
			node._network = _chg.newN.getOrElse(null)
			node.onChangeNet(_chg)
		}
		netChanges.clear
		val _indirectConnects = indirectConnects.flatMap { case (nodeA, ops) =>
			ops.flatMap { case (sel, connected) =>
				sel.nodes(nodeA).map { nodeB =>
					((nodeA, nodeB), connected)
				}
			}
		}
		for(((nodeA, nodeB), connected) <- _indirectConnects) {
			nodeA.onConnectionChange(nodeB, connected)
			nodeB.onConnectionChange(nodeA, connected)
		}
		indirectConnects.clear
		while(tickRun.nonEmpty) {
			tickRun.dequeue().apply()
		}
		tickQueued = false
	}

	def queueTick {
		if(!Side.effective.isServer) {
			CPupComputers.logger.warn("Queuing network tick from non-server thread", new Exception())
		}

		if(tickQueued) return
		tickQueued = true

		TickUtil.register(TickEvent.Type.SERVER, TickEvent.Phase.END, Side.SERVER, () => runTick)
	}

	sealed trait Change {
		def newN: Option[Network]
		def oldN: Option[Network]
	}
	object Change {
		def apply(newN: Network, oldN: Network) = (newN, oldN) match {
			case (null, null) => throw new RuntimeException("switching from null to null")
			case (newN, null) => Join(newN)
			case (null, oldN) => Leave(oldN)
			case (newN, oldN) => Switch(newN, oldN)
		}
		case class Join(net: Network) extends Change {
			assert(net != null)
			override def newN = Some(net)
			override def oldN = None
		}
		case class Leave(net: Network) extends Change {
			assert(net != null)
			override def newN = None
			override def oldN = Some(net)
		}
		case class Switch(_newN: Network, old: Network) extends Change {
			assert(_newN != null)
			assert(old != null)
			override def newN = Some(_newN)
			override def oldN = Some(old)
		}
	}
}
