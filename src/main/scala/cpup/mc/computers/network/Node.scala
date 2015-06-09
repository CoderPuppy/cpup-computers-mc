package cpup.mc.computers.network

import java.util.UUID

import scala.collection.mutable

import net.minecraft.nbt.NBTTagCompound

trait Node extends NodeHolder {
	var uuid = UUID.randomUUID

	def readFromNBT(nbt: NBTTagCompound) {
		uuid = UUID.fromString(nbt.getString("uuid"))
	}

	def writeToNBT(nbt: NBTTagCompound) {
		nbt.setString("uuid", uuid.toString)
	}

	protected[network] var _network = new Network(this)
	def network = _network

	protected[network] val _connections = mutable.Set[Node]()
	def connections = _connections.toSet

	def node = this

	// NOTE: this is very similar to part of OpenComputer's Network.searchGraph
	def visibleNodes = {
		val queue = mutable.Queue[Node]()
		queue += this
		val res = mutable.Set[Node]()
		while(queue.nonEmpty) {
			val node = queue.dequeue
			if(!res.contains(node)) {
				res += node
				queue += node
			}
		}
		res
	}

	def connect(node: Node) {
		_connections += node
		_network._connect(this, node)
	}

	def disconnect(node: Node) {
		_connections -= node
		_network._disconnect(this, node)
	}

	def remove {
		_connections.clear
		_network._remove(this)
	}

	def onJoin(net: Network) {}
	def onLeave(net: Network) {}
	def onConnect(node: Node) {}
	def onDisconnect(node: Node) {}
}
