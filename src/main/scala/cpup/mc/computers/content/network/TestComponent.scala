package cpup.mc.computers.content.network

import cpup.mc.computers.{Ref, CPupComputers}
import cpup.mc.computers.content.{BaseBlockContainer, BaseTE}
import cpup.mc.computers.network.component.{ComponentAnnotation => ComponentA, Component, ComponentProviderNode}
import cpup.mc.computers.network.{Node, NodeTE}
import cpup.mc.lib.util.Side
import cpw.mods.fml.server.FMLServerHandler
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.util.ChatComponentText
import net.minecraft.world.World

object TestComponent extends Block(Material.iron) with BaseBlockContainer {
	name = "test-component"

	@ComponentA(mod = Ref.modID, name = "test-component")
	class TE extends BaseTE with NodeTE.Simple {
		def selfTE = this
		override def createNode = new Node with ComponentProviderNode {
			override lazy val components: Set[Component] = Set(Component.fromAnnotations(this, selfTE))
		}

		@ComponentA.InternalNode
		val internalNode = if(Side.effective == Side.SERVER) new Node {} else null

		@ComponentA.InternalNode
		def _internalNode = if(Side.effective == Side.SERVER) new Node {} else null

		@ComponentA.Method(usage = "Say hello")
		def sayHi(to: String = "World") {
			FMLServerHandler.instance.getServer.addChatMessage(new ChatComponentText(s"Hello $to!"))
		}
	}

	override def createNewTileEntity(world: World, meta: Int) = new TE
}
