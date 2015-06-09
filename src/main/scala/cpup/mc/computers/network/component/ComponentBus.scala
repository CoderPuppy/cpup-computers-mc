package cpup.mc.computers.network.component

import scala.collection.mutable

import cpup.mc.computers.network.{Network, Bus}
import cpup.mc.computers.network.component

class ComponentBus(val network: Network) extends Bus {
	protected[component] val _components = mutable.Set[Component]()
	def components = _components.toSet

	def add(comp: Component) {
		_components += comp
	}

	def rm(comp: Component) {
		_components -= comp
	}
}

object ComponentBus {
	implicit val create = (net: Network) => new ComponentBus(net)
}
