package cpup.mc.computers.network.component

import cpup.mc.computers.network.{Network, Node}

trait ComponentProviderNode extends Node {
	def components: Seq[(String, Map[String, Method])]

	protected[component] var _components = Array[Component]()

	override def onJoin(net: Network) {
		super.onJoin(net)
		val _node = this
		_components = components.view.zipWithIndex.map((comp) => {
			val ((_typ, _methods), i) = comp
			new Component {
				override def node = _node
				override def slot = i
				override def typ = _typ
				override def methods = _methods
			}
		}).toArray
		for(comp <- _components) net.bus[ComponentBus].add(comp)
	}

	override def onLeave(net: Network) {
		super.onLeave(net)
		for(comp <- _components) net.bus[ComponentBus].rm(comp)
		_components = Array[Component]()
	}
}
