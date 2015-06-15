package cpup.mc.computers.network

import scala.collection.mutable

import cpup.mc.lib.{CPupMod, CPupModRef}
import cpup.mc.lib.content.CPupBlockContainer
import cpup.mc.lib.util.{NBTUtil, Side, Direction}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

trait NodeTE extends TileEntity with SidedNodeHolder {
	val connected = mutable.Map[Direction, Node]()

	def getNode(dir: Direction) = Option(getWorldObj.getTileEntity(xCoord + dir.x, yCoord + dir.y, zCoord + dir.z)).flatMap {
		case te: SidedNodeHolder => te.node(dir.opposite)
		case _ => None
	}

	def connect(dir: Direction, _node: Node) {
		connected(dir) = _node
		node(dir).foreach(_.connect(_node))
	}

	def disconnect(dir: Direction) {
		for(oldNode <- connected.get(dir)) {
			node(dir).foreach(_.disconnect(oldNode))
		}
		connected -= dir
	}

	def updateConnection(dir: Direction) {
		if(Side.effective == Side.SERVER) {
			val oldNodeOpt = connected.get(dir)
			val nodeOpt = getNode(dir)
			if(nodeOpt != oldNodeOpt) {
				disconnect(dir)
				for(_node <- nodeOpt) connect(dir, _node)
			}
		}
	}


}

object NodeTE {
	trait Simple extends NodeTE with NodeHolder with CPupBlockContainer.TE {
		def createNode: Node

		override val node = if(Side.effective == Side.SERVER) createNode else null

		override def onNeighborChange(dir: Direction) {
			updateConnection(dir)
		}

		override def invalidate {
			super.invalidate
			if(Side.effective == Side.SERVER) node.remove
		}

		override def onChunkUnload {
			super.onChunkUnload
			if(Side.effective == Side.SERVER) node.remove
		}

		override def readFromNBT(nbt: NBTTagCompound) {
			super.readFromNBT(nbt)
			node.readFromNBT(nbt.getCompoundTag("node"))
		}

		override def writeToNBT(nbt: NBTTagCompound) {
			super.writeToNBT(nbt)
			node.writeToNBT(NBTUtil.compound(nbt, "node"))
		}
	}
}
