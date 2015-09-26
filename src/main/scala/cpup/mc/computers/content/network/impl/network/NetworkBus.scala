package cpup.mc.computers.content.network.impl.network

import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.CPupComputers
import cpup.mc.computers.content.network.impl
import cpup.mc.computers.content.network.impl.{Bus, Network, Node}

class NetworkBus(val network: Network) extends Bus {
	override def typ = s"${CPupComputers.ref.modID}:network-bus"

	protected[network] def sensitiveNodes = network.nodes.flatMap { case node: NetworkSensitiveMode => Some(node) case _ => None }

	protected[network] def _send(lastHop: Node, from: Node, data: String*) {
		sensitiveNodes.foreach(_.onMessage(lastHop, from, data: _*))
	}

	def send(from: Node, data: String*) {
		_send(from, from, data: _*)
	}

	override def connector(host: Node.Host) = new NetworkBus.Connector(host)
}

object NetworkBus {
	implicit val create = (net: Network) => new NetworkBus(net)

	class Connector(val host: Node.Host) extends impl.Connector[NetworkBus] {
		lazy val busType = ru.typeTag[NetworkBus]

		protected[network] final val _connector = this
		protected[network] def createNode = {
			val _host = host
			new Node with impl.Connector.Node[NetworkBus] with NetworkSensitiveMode {
				override def host = _host
				override def connector = _connector
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
