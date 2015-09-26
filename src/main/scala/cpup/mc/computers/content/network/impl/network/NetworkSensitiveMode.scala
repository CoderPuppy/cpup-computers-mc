package cpup.mc.computers.content.network.impl.network

import cpup.mc.computers.content.network.impl.Node

trait NetworkSensitiveMode {
	def onMessage(lastHop: Node, from: Node, data: String*) {}
}
