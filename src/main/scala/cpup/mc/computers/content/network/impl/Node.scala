package cpup.mc.computers.content.network.impl

import java.util.UUID

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.CPupComputers
import cpup.mc.computers.content.network.impl.Node.Host
import cpup.mc.lib.network.{Context, CPupNetwork}
import cpup.mc.lib.util.{TickUtil, Side}
import cpw.mods.fml.common.gameevent.TickEvent
import net.minecraft.nbt.NBTTagCompound
import cpup.mc.lib.inspecting.Registry.IDed

trait Node extends NodeHolder with IDed {
	override def typ = s"${CPupComputers.ref.modID}:node"
	def owner: Option[Node] = None
	def host: Node.Host

	def readFromNBT(nbt: NBTTagCompound) {
		changeUUID(UUID.fromString(nbt.getString("uuid")))
	}

	def writeToNBT(nbt: NBTTagCompound) {
		nbt.setString("uuid", uuid.toString)
	}

	protected[impl] var _network: Network = null
	if(Side.effective.isServer) {
		Network.netChanges(this) = () => Network.Change.Join(new Network)
		Network.queueTick
	}
	def network = _network

	protected[impl] val _connections = mutable.Set[Node]()
	def connections = _connections.toSet

	protected[impl] val _ports = mutable.Set[Node with Node.Port]()
	def ports = _ports.toSet

	def addPort(filter: (Bus, ru.TypeTag[_ <: Bus]) => Boolean = (bus, tt) => true) = {
		val _node = this
		val port = new Node with Node.Port {
			override def host = _node.host
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
		res.toSet
	}

	def connection(node: Node, connected: Boolean) {
		if(!Side.effective.isServer) return
		Network.connection(this, node, connected)
	}

	def remove {
		if(!Side.effective.isServer) return
		Network.remove(this)
	}

	protected[impl] def _connection(node: Node, connected: Boolean) {
		if(connected) {
			_connections += node
		} else {
			_connections -= node
		}
	}

	def onChangeNet(chg: Network.Change) {}
	def onConnectionChange(node: Node, connected: Boolean) {}

	def onConnect(connector: Connector[_ <: Bus]) {}
	def onDisconnect(connector: Connector[_ <: Bus]) {}
	def onBus[B <: Bus](bus: B)(implicit tt: ru.TypeTag[B]) {}

	def msgNetwork: Option[CPupNetwork[_ >: this.type <: AnyRef]] = None
}

object Node {
	trait Host {
		def ctx: Context
		def get[T](id: Symbol = 'default)(implicit tt: ru.TypeTag[T]): Option[T]
	}

	trait Port extends Node {
		def mainNode: Node
		def busFilter[B <: Bus](bus: B)(implicit tt: ru.TypeTag[_ <: B]): Boolean

		override def owner = Some(mainNode)

		protected[impl] val _connectors = mutable.Map[ru.TypeTag[_ <: Bus], Connector[_ <: Bus]]()
		def connectors = _connectors

		def connect[B <: Bus](bus: B)(implicit tt: ru.TypeTag[_ <: B]) {
			if(!_connectors.contains(tt) && busFilter(bus)) {
				val connector = bus.connector(host)
				mainNode.connection(connector.nodeA, true)
				connection(connector.nodeB, true)
				_connectors(tt) = connector
			}
		}

		override def onBus[B <: Bus](bus: B)(implicit tt: ru.TypeTag[B]) {
			connect(bus)(tt)
		}
	}

	sealed trait RelSelector {
		def nodes(node: Node): Set[Node]
	}
	object RelSelector {
		case object Direct extends RelSelector {
			override def nodes(node: Node) = node._connections.toSet
		}
		case object Network extends RelSelector {
			override def nodes(node: Node) = node._network match {
				case null => Set()
				case net => net._nodes.toSet
			}
		}
		case class Specific(set: Set[Node]) extends RelSelector {
			override def nodes(node: Node) = set
		}
		case class FuncWrap(fn: Node => Set[Node]) extends RelSelector {
			override def nodes(node: Node) = fn(node)
		}
		case object Indirect extends RelSelector {
			override def nodes(node: Node) = node.visibleNodes
		}
	}
}
