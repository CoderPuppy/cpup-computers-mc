package cpup.mc.computers.network.network

import cpup.mc.computers.network.Node

trait NetworkSensitiveMode {
	def onMessage(lastHop: Node, from: Node, data: String*) {}
}
