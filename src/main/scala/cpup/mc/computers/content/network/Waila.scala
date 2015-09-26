package cpup.mc.computers.content.network

import java.util

import com.typesafe.config.Config
import cpup.lib.module.ModuleID
import cpup.mc.computers.content.network.impl.NodeHolder
import cpup.mc.computers.content.network.impl.component.ComponentProviderNode
import cpup.mc.lib.ModLifecycleHandler
import cpup.mc.lib.util.waila.WailaDataProvider
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLInterModComms}
import mcp.mobius.waila.api.{IWailaConfigHandler, IWailaDataAccessor, IWailaDataProvider, IWailaRegistrar}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagString, NBTTagList, NBTTagCompound}
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT
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
				tag.setString("uuid", node.uuid.toString)
				tag.setString("network", node.network.uuid.toString)
				node match {
					case n: ComponentProviderNode => {
						val l = new NBTTagList
						for(c <- n.components) l.appendTag({
							val nbt = new NBTTagCompound
							nbt.setString("typ", c.typ)
							nbt.setString("uuid", c.uuid.toString)
							nbt
						})
						tag.setTag("components", l)
					}

					case _ =>
				}
				tag
			}

			override def getWailaBody(stack: ItemStack, data: util.List[String], accessor: IWailaDataAccessor, config: IWailaConfigHandler) = {
				val nbt = accessor.getNBTData
				data.add(s"Address: ${nbt.getString("uuid")}")
				data.add(s"Network: ${nbt.getString("network")}")
				nbt.getTag("components") match {
					case l: NBTTagList => {
						for(i <- 0 until l.tagCount) {
							val c = l.getCompoundTagAt(i)
							data.add(s"$i - ${c.getString("typ")}")
						}
					}
					case _ =>
				}
				data
			}
		}, nbt = true, body = true)
	}
}
