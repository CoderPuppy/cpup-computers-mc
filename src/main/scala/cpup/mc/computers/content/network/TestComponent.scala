package cpup.mc.computers.content.network

import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.Ref
import cpup.mc.computers.content.network.impl.Node.Host
import cpup.mc.computers.content.network.impl.component.{Component, ComponentAnnotation => ComponentA, ComponentProviderNode}
import cpup.mc.computers.content.network.impl.{Node, NodeTE}
import cpup.mc.computers.content.{BaseBlockContainer, BaseTE}
import cpup.mc.lib.network.Context
import cpw.mods.fml.server.FMLServerHandler
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.util.ChatComponentText
import net.minecraft.world.World

object TestComponent extends Block(Material.iron) with BaseBlockContainer {
	name = "test-component"

	@ComponentA(mod = Ref.modID, name = "test-component")
	class TE extends BaseTE with NodeTE.Simple with Node.Host {
		def selfTE = this
		override val node = new Node with ComponentProviderNode {
			override def host = selfTE
			override lazy val components: Set[Component] = Set(Component.fromAnnotations(this, selfTE))
		}

		@ComponentA.Method(usage = "Say hello")
		def sayHi(to: String = "World") {
			FMLServerHandler.instance.getServer.addChatMessage(new ChatComponentText(s"Hello $to!"))
		}

		override def ctx = NodeTE.ctx(this)
		override def get[T](id: Symbol)(implicit tt: ru.TypeTag[T]) = None
	}

	override def createNewTileEntity(world: World, meta: Int) = new TE
}
