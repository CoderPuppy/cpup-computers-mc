package cpup.mc.computers.content.network.impl.component

import cpup.mc.computers.content.network.impl.{Network, Node}

trait ComponentProviderNode extends Node {
	def components: Set[Component]

	protected[component] var _components = List[Component]()

	override def onJoin(net: Network) {
		super.onJoin(net)
		val _node = this
		_components = components.toList
		for(comp <- _components) net.bus[ComponentBus].add(comp)
	}

	override def onLeave(net: Network) {
		super.onLeave(net)
		for(comp <- _components) net.bus[ComponentBus].rm(comp)
		_components = List[Component]()
	}
}
