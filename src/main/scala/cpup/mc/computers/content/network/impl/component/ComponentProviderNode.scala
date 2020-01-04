package cpup.mc.computers.content.network.impl.component

import cpup.mc.computers.content.network.impl.Network.Change
import cpup.mc.computers.content.network.impl.{Network, Node}

trait ComponentProviderNode extends Node {
	def components: Set[Component]

	protected[component] var _components = List[Component]()

	override def onChangeNet(chg: Change) {
		super.onChangeNet(chg)
		val _node = this
		_components = components.toList
		for(net <- chg.oldN; comp <- _components) net.bus[ComponentBus].add(comp)
		for(net <- chg.newN; comp <- _components) net.bus[ComponentBus].add(comp)
	}
}
