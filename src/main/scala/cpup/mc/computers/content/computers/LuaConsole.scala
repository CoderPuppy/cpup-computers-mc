package cpup.mc.computers.content.computers

import cpup.mc.computers.content.{BaseBlockContainer, BaseTE}
import cpup.mc.computers.network.{Node, NodeTE}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.world.World

object LuaConsole extends Block(Material.iron) with BaseBlockContainer {
	class TE extends BaseTE with NodeTE.Simple {
		override def createNode = new Node {}
	}

	override def createNewTileEntity(world: World, meta: Int) = new TE
}
