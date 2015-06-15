package cpup.mc.computers.network.network

import scala.collection.mutable
import scala.reflect.internal.util.WeakHashSet
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.{CPupComputers, network}
import cpup.mc.computers.network.{Bus, Network, Node}
import org.luaj.vm2.Varargs

class NetworkBus(val network: Network) extends Bus {
	override def typ = s"${CPupComputers.ref.modID}:network-bus"

	protected[network] def sensitiveNodes = network.nodes.flatMap { case node: NetworkSensitiveMode => Some(node) case _ => None }

	protected[network] def _send(lastHop: Node, from: Node, data: String*) {
		sensitiveNodes.foreach(_.onMessage(lastHop, from, data: _*))
	}

	def send(from: Node, data: String*) {
		_send(from, from, data: _*)
	}

	override def connector = new NetworkBus.Connector
}

object NetworkBus {
	implicit val create = (net: Network) => new NetworkBus(net)
	implicit val connector = () => new Connector

	class Connector extends network.Connector[NetworkBus] {
		lazy val busType = ru.typeTag[NetworkBus]

		protected[network] val _connector = this
		protected[network] def createNode = {
			new Node with network.Connector.Node[NetworkBus] with NetworkSensitiveMode {
				def connector = _connector
				override def onMessage(_from: Node, from: Node, data: String*) {
					if(_from == other(this)) return
					for(net <- Option(other(this).network)) net.bus[NetworkBus]._send(this, from, data: _*)
				}
			}
		}
		val nodeA = createNode
		val nodeB = createNode
	}
}
