package cpup.mc.computers.content.network

import scala.collection.mutable

import cpup.mc.computers.content.{BaseBlockContainer, BaseTE}
import cpup.mc.computers.network.{NodeTE, NodeHolder, Node}
import cpup.mc.lib.content.CPupBlockContainer
import cpup.mc.lib.util.{Side, NBTUtil, Direction}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

object Cable extends Block(Material.iron) with BaseBlockContainer {
	name = "cable"

	class TE extends BaseTE with NodeTE.Simple {
		override def createNode = new Node {}
	}

	override def createNewTileEntity(world: World, meta: Int) = new TE
}
