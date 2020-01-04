package cpup.mc.computers.content.network.impl

import cpup.mc.lib.inspecting.Registry.IDed

trait Bus extends IDed {
	def network: Network

	def connector(host: Node.Host): Connector[_ <: Bus]

	def onNodeChangeNet(node: Node, chg: Network.Change) {}
	def onConnect(connector: Connector[_ <: Bus]) {}
	def onDisconnect(connector: Connector[_ <: Bus]) {}
	def onConnect(network: Network) {}
	def onDisconnect(network: Network) {}
}
