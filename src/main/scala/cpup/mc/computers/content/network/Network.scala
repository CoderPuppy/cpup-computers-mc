package cpup.mc.computers.content.network

import com.typesafe.config.Config
import cpup.lib.module.{ModuleID, ModuleLoader}
import cpup.mc.computers.network.component.{Component, ComponentProviderNode, ComponentBus}
import cpup.mc.computers.network.{Node, SidedNodeHolder}
import cpup.mc.computers.{CPupComputers, network => impl}
import cpup.mc.lib.{ModLifecycleHandler, inspecting}
import cpup.mc.lib.inspecting.MCContext
import cpup.mc.lib.inspecting.Registry.IDed
import cpup.mc.lib.util.Direction
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.registry.GameRegistry
import org.slf4j.Logger

@ModuleID(id = "network")
class Network(logger: Logger, config: Config) extends ModLifecycleHandler {
	lazy val waila = ModuleLoader.load[Waila](this).left.toOption

	lazy val mod = CPupComputers
	override def preInit(e: FMLPreInitializationEvent) {
		GameRegistry.registerBlock(Cable, Cable.name)
		GameRegistry.registerTileEntity(classOf[Cable.TE], Cable.name)

		GameRegistry.registerBlock(TestComponent, TestComponent.name)
		GameRegistry.registerTileEntity(classOf[TestComponent.TE], TestComponent.name)

		{
			import inspecting.Registry
			import inspecting.Registry.Data
			Registry.register[IDed, Any] { (obj: IDed, ctx: Any) =>
				Some(Data.Table(
					"typ" -> Data.String(obj.typ),
					"uuid" -> Data.String(obj.uuid.toString)
				))
			}
			Registry.register[SidedNodeHolder, MCContext.Side]((obj: SidedNodeHolder, ctx: MCContext.Side) => {
				obj.node(ctx.side).map(node => {
					Data.Table(
						"node" -> node.link
					)
				})
			})
			Registry.register[SidedNodeHolder, Any]((obj: SidedNodeHolder, ctx: Any) => {
				if(obj.isInstanceOf[Node]) None else Some(Data.Table(
				"nodes" -> Data.Table(Direction.valid.flatMap(side => obj.node(side).map(node => (side.toString, node.link))): _*)
				))
			})
			Registry.register[Node, Any]((obj: Node, ctx: Any) => {
				Some(Data.Table(
					"connections" -> Data.List(obj.connections.toSeq.map(node => Data.Link(s"${mod.ref.modID}:node", Data.String(node.uuid.toString))): _*),
					"network" -> obj.network.link
				))
			})
			Registry.register[ComponentProviderNode, Any] { (obj: ComponentProviderNode, ctx: Any) =>
				Some(Data.Table(
					"components" -> Data.List(obj.components.map(_.link).toSeq: _*)
				))
			}
			Registry.register[impl.Network, Any]((obj: impl.Network, ctx: Any) => {
				Some(Data.Table(
					"nodes" -> Data.List(obj.nodes.map(node => {
						Data.Link(s"${mod.ref.modID}:node", Data.String(node.uuid.toString))
					}).toSeq: _*),
					"buses" -> Data.List(obj.buses.values.map(_.link).toSeq: _*)
				))
			})
			Registry.register[ComponentBus, Any] { (obj: ComponentBus, ctx: Any) =>
				Some(Data.Table(
					"components" -> Data.List(obj.components.toSeq.map(_.link): _*)
				))
			}
//			Registry.register[Component, Any] { (obj: Component, ctx: Any) =>
//				Some(Data.Table(
//					"methods" -> Data.Table(obj.methods.map { kv => kv._1 -> Registry.inspect(kv._2, )}: _*)
//				))
//			}
		}
	}

	override def init(e: FMLInitializationEvent) {
		waila.foreach(_.init(e))
	}
}
