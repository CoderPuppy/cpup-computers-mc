package cpup.mc.computers.content.computers

import cpup.mc.computers.CPupComputers
import cpup.mc.computers.content.network.impl.component.{Method, Component}
import cpup.mc.computers.content.network.impl.{Node, NodeHolder}
import cpup.mc.lib.util.NBTUtil
import net.minecraft.nbt.NBTTagCompound

class Keyboard(_host: Node.Host) extends Component with NodeHolder with Serializable {
	val node = new Node {
		override def host = _host
	}

	override def typ = s"${CPupComputers.ref.modID}:keyboard"
	override def ownerNode = node
	override def methods = Map()

	override def writeToNBT(nbt: NBTTagCompound) {
		super.writeToNBT(nbt)
		node.writeToNBT(NBTUtil.compound(nbt, "node"))
	}

	override def readFromNBT(nbt: NBTTagCompound) {
		super.readFromNBT(nbt)
		node.readFromNBT(nbt.getCompoundTag("node"))
	}
}
