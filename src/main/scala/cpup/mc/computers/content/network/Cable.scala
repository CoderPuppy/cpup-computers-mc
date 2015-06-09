package cpup.mc.computers.content.network

import scala.collection.mutable

import cpup.mc.computers.content.{BaseBlockContainer, BaseTE}
import cpup.mc.computers.network.{NodeHolder, Node}
import cpup.mc.lib.content.CPupBlockContainer
import cpup.mc.lib.util.{NBTUtil, Direction}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

object Cable extends Block(Material.iron) with BaseBlockContainer {
	name = "cable"

	class TE extends BaseTE with CPupBlockContainer.TE with NodeHolder {
		val node = new Node {}
		val connected = mutable.Map[Direction, Node]()

		def getNode(dir: Direction) = Option(world.getTileEntity(x + dir.x, y + dir.y, z + dir.z)).flatMap {
			case te: NodeHolder => Some(te.node)
			case _ => None
		}

		def connect(dir: Direction, _node: Node) {
			println("connecting to", _node.uuid, dir)
			connected(dir) = _node
			node.connect(_node)
		}

		def disconnect(dir: Direction) {
			for(oldNode <- connected.get(dir)) {
				println("disconnecting from", oldNode.uuid, dir)
				node.disconnect(oldNode)
			}
			connected -= dir
		}

		override def invalidate {
			super.invalidate
			node.remove
		}

		override def onNeighborChange(dir: Direction) {
			val oldNodeOpt = connected.get(dir)
			val nodeOpt = getNode(dir)
			if(nodeOpt != oldNodeOpt) {
				disconnect(dir)
				for(_node <- nodeOpt) connect(dir, _node)
			}
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

	override def createNewTileEntity(world: World, meta: Int) = new TE
}
