package cpup.mc.computers.network

import cpup.mc.lib.inspecting.Registry.IDed

trait Bus extends IDed {
	def network: Network

	def connector: Connector[_ <: Bus]

	def onJoin(node: Node) {}
	def onLeave(node: Node) {}
	def onConnect(connector: Connector[_ <: Bus]) {}
	def onDisconnect(connector: Connector[_ <: Bus]) {}
	def onConnect(network: Network) {}
	def onDisconnect(network: Network) {}
}
