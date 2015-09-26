package cpup.mc.computers.content.network

import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.content.network.impl.Node.Host
import cpup.mc.computers.content.network.impl.{Node, NodeTE}
import cpup.mc.computers.content.{BaseBlockContainer, BaseTE}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.world.World

object Cable extends Block(Material.iron) with BaseBlockContainer {
	name = "cable"

	class TE extends BaseTE with NodeTE.Simple with Node.Host {
		def selfTE = this

		override val node = new Node {
			override def host = selfTE
		}

		override def ctx = NodeTE.ctx(this)
		override def get[T](id: Symbol)(implicit tt: ru.TypeTag[T]) = None
	}

	override def createNewTileEntity(world: World, meta: Int) = new TE
}
