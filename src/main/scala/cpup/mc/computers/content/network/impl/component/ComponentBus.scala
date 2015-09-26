package cpup.mc.computers.content.network.impl.component

import java.util.UUID

import scala.collection.mutable
import scala.reflect.internal.util.WeakHashSet
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.CPupComputers
import cpup.mc.computers.content.network.impl
import cpup.mc.computers.content.network.impl.{Bus, Network, Node}
import org.luaj.vm2.Varargs

class ComponentBus(val network: Network) extends Bus {
	override def typ = s"${CPupComputers.ref.modID}:component-bus"

	protected[component] val _components = mutable.Map.empty[UUID, Component]
	def components = _components.toMap

	protected[component] def sensitiveNodes = network.nodes.flatMap { case node: ComponentSensitiveNode => Some(node) case _ => None }

	def add(comp: Component) {
		if(_components.contains(comp.uuid)) return
		_components(comp.uuid) = comp
		sensitiveNodes.foreach(_.onAddComponent(comp))
	}

	def rm(comp: Component) {
		if(!_components.contains(comp.uuid)) return
		_components -= comp.uuid
		sensitiveNodes.foreach(_.onRmComponent(comp))
	}

	def signal(comp: Component, name: String, args: Varargs) {
		sensitiveNodes.foreach(_.onSignal(comp, name, args))
	}

	override def connector(host: Node.Host) = new ComponentBus.Connector(host)
}

object ComponentBus {
	implicit val create = (net: Network) => new ComponentBus(net)

	class Connector(val host: Node.Host) extends cpup.mc.computers.content.network.impl.Connector[ComponentBus] {
		lazy val busType = ru.typeTag[ComponentBus]

		protected[component] var transferedSignals = new WeakHashSet[(Component, String, Varargs)]

		protected[component] val _connector = this
		protected[component] def createNode = {
			val _host = host
			new Node with impl.Connector.Node[ComponentBus] with ComponentSensitiveNode {
				override def host = _host
				override def connector = _connector
				override def onAddComponent(comp: Component) {
					for(net <- Option(other(this).network)) net.bus[ComponentBus].add(comp)
				}
				override def onRmComponent(comp: Component) {
					for(net <- Option(other(this).network)) net.bus[ComponentBus].rm(comp)
				}

				override def onSignal(comp: Component, name: String, args: Varargs) {
					val key = (comp, name, args)
					if(transferedSignals.contains(key)) return
					transferedSignals += key
					for(net <- Option(other(this).network)) net.bus[ComponentBus].signal(comp, name, args)
				}
			}
		}
		val nodeA = createNode
		val nodeB = createNode
	}
}
