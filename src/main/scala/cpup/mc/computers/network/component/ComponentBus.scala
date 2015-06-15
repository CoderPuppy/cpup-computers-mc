package cpup.mc.computers.network.component

import scala.collection.mutable
import scala.reflect.internal.util.WeakHashSet
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.{CPupComputers, network}
import cpup.mc.computers.network.{Bus, Network, Node}
import org.luaj.vm2.Varargs

class ComponentBus(val network: Network) extends Bus {
	override def typ = s"${CPupComputers.ref.modID}:component-bus"

	protected[component] val _components = mutable.Set[Component]()
	def components = _components.toSet

	protected[component] def sensitiveNodes = network.nodes.flatMap { case node: ComponentSensitiveNode => Some(node) case _ => None }

	def add(comp: Component) {
		if(_components.contains(comp)) return
		_components += comp
		sensitiveNodes.foreach(_.onAddComponent(comp))
	}

	def rm(comp: Component) {
		if(!_components.contains(comp)) return
		_components -= comp
		sensitiveNodes.foreach(_.onRmComponent(comp))
	}

	def signal(comp: Component, name: String, args: Varargs) {
		sensitiveNodes.foreach(_.onSignal(comp, name, args))
	}

	override def connector = new ComponentBus.Connector
}

object ComponentBus {
	implicit val create = (net: Network) => new ComponentBus(net)
	implicit val connector = () => new Connector

	class Connector extends network.Connector[ComponentBus] {
		lazy val busType = ru.typeTag[ComponentBus]

		protected[component] var transferedSignals = new WeakHashSet[(Component, String, Varargs)]

		protected[component] val _connector = this
		protected[component] def createNode = {
			new Node with network.Connector.Node[ComponentBus] with ComponentSensitiveNode {
				def connector = _connector
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
