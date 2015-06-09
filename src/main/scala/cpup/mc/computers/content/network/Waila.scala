package cpup.mc.computers.content.network

import java.util

import com.typesafe.config.Config
import cpup.lib.module.ModuleID
import cpup.mc.computers.network.NodeHolder
import cpup.mc.lib.ModLifecycleHandler
import cpup.mc.lib.util.waila.WailaDataProvider
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLInterModComms}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor, IWailaDataProvider, IWailaRegistrar}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import org.slf4j.Logger

@ModuleID(id = "waila")
class Waila(logger: Logger, config: Config) extends ModLifecycleHandler {
	override def init(e: FMLInitializationEvent) {
		FMLInterModComms.sendMessage("Waila", "register", s"cpup.mc.computers.content.network.WailaHandler.init")
	}
}

object WailaHandler {
	def init(registrar: IWailaRegistrar) {
		def register[T](provider: IWailaDataProvider, stack: Boolean = false, head: Boolean = false, body: Boolean = false, tail: Boolean = false, nbt: Boolean = false)(implicit manifest: Manifest[T]) {
			val cla = manifest.runtimeClass
			if(nbt) registrar.registerNBTProvider(provider, cla)
			if(stack) registrar.registerStackProvider(provider, cla)
			if(head) registrar.registerHeadProvider(provider, cla)
			if(body) registrar.registerBodyProvider(provider, cla)
			if(tail) registrar.registerTailProvider(provider, cla)
		}

		register[NodeHolder](new WailaDataProvider {
			override def getNBTData(player: EntityPlayerMP, te: TileEntity, tag: NBTTagCompound, world: World, x: Int, y: Int, z: Int) = {
				val node = te.asInstanceOf[NodeHolder].node
				node.writeToNBT(tag)
				tag
			}

			override def getWailaBody(stack: ItemStack, data: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = {
				data.add(s"Address: ${accessor.getNBTData.getString("uuid")}")
				data
			}
		}, nbt = true, body = true)
	}
}
