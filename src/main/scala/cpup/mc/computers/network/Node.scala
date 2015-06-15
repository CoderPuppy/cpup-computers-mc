package cpup.mc.computers.network

import java.util.UUID

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.CPupComputers
import net.minecraft.nbt.NBTTagCompound
import cpup.mc.lib.inspecting.Registry.IDed

trait Node extends NodeHolder with IDed {
	override def typ = s"${CPupComputers.ref.modID}:node"
	def owner: Option[Node] = None

	def readFromNBT(nbt: NBTTagCompound) {
		changeUUID(UUID.fromString(nbt.getString("uuid")))
	}

	def writeToNBT(nbt: NBTTagCompound) {
		nbt.setString("uuid", uuid.toString)
	}

	protected[network] var _network = new Network(this)
	def network = _network

	protected[network] val _connections = mutable.Set[Node]()
	def connections = _connections.toSet

	protected[network] val _ports = mutable.Set[Node with Node.Port]()
	def ports = _ports.toSet

	def addPort(filter: (Bus, ru.TypeTag[_ <: Bus]) => Boolean = (bus, tt) => true) = {
		val _node = this
		val port = new Node with Node.Port {
			override def mainNode = _node
			override def busFilter[B <: Bus](bus: B)(implicit tt: ru.TypeTag[_ <: B]) = filter(bus, tt)
		}
		_ports += port
		for((busType, bus) <- _network._buses) port.connect(bus)(busType)
		port
	}

	def addPort(filter: Set[ru.TypeTag[_ <: Bus]]): Node.Port = addPort { (bus, tt) => filter.exists { _tt => tt.tpe <:< _tt.tpe } }

	def node = this

	// NOTE: this is very similar to part of OpenComputer's Network.searchGraph
	def visibleNodes = {
		val queue = mutable.Queue[Node]()
		queue += this
		val res = mutable.Set[Node]()
		while(queue.nonEmpty) {
			val node = queue.dequeue
			if(!res.contains(node)) {
				res += node
				queue ++= node.connections -- res
			}
		}
		res
	}

	def connect(node: Node) {
		_connections += node
		node._connections += this
		_network._connect(this, node)
	}

	def disconnect(node: Node) {
		_connections -= node
		node._connections -= this
		_network._disconnect(this, node)
	}

	def remove {
		for(con <- _connections) disconnect(con)
		_unregister
	}

	def onJoin(net: Network) {}
	def onLeave(net: Network) {}
	def onConnect(node: Node) {}
	def onDisconnect(node: Node) {}
	def onConnect(connector: Connector[_ <: Bus]) {}
	def onDisconnect(connector: Connector[_ <: Bus]) {}
	def onConnect(network: Network) {}
	def onDisconnect(network: Network) {}
	def onBus[B <: Bus](bus: B)(implicit tt: ru.TypeTag[B]) {}
}

object Node {
	trait Port extends Node {
		def mainNode: Node
		def busFilter[B <: Bus](bus: B)(implicit tt: ru.TypeTag[_ <: B]): Boolean

		override def owner = Some(mainNode)

		protected[network] val _connectors = mutable.Map[ru.TypeTag[_ <: Bus], Connector[_ <: Bus]]()
		def connectors = _connectors

		def connect[B <: Bus](bus: B)(implicit tt: ru.TypeTag[_ <: B]) {
			if(!_connectors.contains(tt) && busFilter(bus)) {
				val connector = bus.connector
				mainNode.connect(connector.nodeA)
				connect(connector.nodeB)
				_connectors(tt) = connector
			}
		}

		override def onBus[B <: Bus](bus: B)(implicit tt: ru.TypeTag[B]) {
			connect(bus)(tt)
		}
	}
}
