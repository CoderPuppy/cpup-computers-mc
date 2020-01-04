package cpup.mc.computers.content.network.impl

import scala.collection.mutable

import cpup.mc.lib.network.Context
import cpup.mc.lib.{CPupMod, CPupModRef}
import cpup.mc.lib.content.CPupBlockContainer
import cpup.mc.lib.util.{TickUtil, NBTUtil, Side, Direction}
import cpw.mods.fml.common.gameevent.TickEvent
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

object NodeTE {
	private val loading = mutable.Set.empty[(Int, Int, Int, Int)]

	def connect(te: TileEntity with SidedNodeHolder) {
		val key = (te.getWorldObj.provider.dimensionId, te.xCoord, te.yCoord, te.zCoord)
		if(loading.contains(key)) return
		loading += key
		for {
			dir <- Direction.valid
			node <- te.node(dir)
			(_x, _y, _z) = (te.xCoord + dir.x, te.yCoord + dir.y, te.zCoord + dir.z)
			if !loading.contains((te.getWorldObj.provider.dimensionId, _x, _y, _z))
			_te <- te.getWorldObj.getTileEntity(_x, _y, _z) match {
				case n: SidedNodeHolder => Some(n)
				case _ => None
			}
			_node <- _te.node(dir.opposite)
		} _node.connection(node, true)
		loading.remove(key)
	}

	def ctx(te: TileEntity) = Context.PlayersAround(te.getWorldObj.provider.dimensionId, te.xCoord, te.yCoord, te.zCoord)

	trait Simple extends TileEntity with NodeHolder with CPupBlockContainer.TE {
		override def validate {
			super.validate
			if(Side.effective.isServer) {
				TickUtil.register(TickEvent.Type.SERVER, TickEvent.Phase.END, Side.SERVER,() => {
					connect(this)
					()
				})
			}
		}

		override def invalidate {
			super.invalidate
			if(Side.effective.isServer) node.remove
		}

		override def onChunkUnload {
			super.onChunkUnload
			if(Side.effective.isServer) node.remove
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
