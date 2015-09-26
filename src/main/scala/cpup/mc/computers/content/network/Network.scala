package cpup.mc.computers.content.network

import com.typesafe.config.Config
import cpup.lib.inspecting.Context
import cpup.lib.module.{ModuleID, ModuleLoader}
import cpup.mc.computers.content.network.impl.component.{Component, ComponentProviderNode, ComponentBus}
import cpup.mc.computers.content.network.impl.{Node, SidedNodeHolder}
import cpup.mc.computers.CPupComputers
import cpup.mc.lib.{ModLifecycleHandler, inspecting}
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
			Registry.register[IDed] { (obj: IDed, ctx: Context) =>
				Some(Data.Table(
					"typ" -> Data.String(obj.typ),
					"uuid" -> Data.String(obj.uuid.toString)
				))
			}
			Registry.register[SidedNodeHolder]((obj: SidedNodeHolder, ctx: Context) => {
				ctx[Direction]('side).flatMap(obj.node).map(node => {
					Data.Table(
						"node" -> node.link
					)
				})
			})
			Registry.register[SidedNodeHolder]((obj: SidedNodeHolder, ctx: Context) => {
				if(obj.isInstanceOf[Node]) None else Some(Data.Table(
					"nodes" -> Data.Table(Direction.valid.filter { side =>
						!ctx[Direction]('side).contains(side) && ctx[Direction]('side).flatMap(obj.node) != obj.node(side)
					}.flatMap { side =>
						obj.node(side).map { node =>
							(side.toString, node.link)
						}
					}: _*)
				))
			})
			Registry.register[Node]((obj: Node, ctx: Context) => {
				Some(Data.Table(
					"connections" -> Data.List(obj.connections.toSeq.map(node => Data.Link(s"${mod.ref.modID}:node", Data.String(node.uuid.toString))): _*),
					"network" -> obj.network.link
				))
			})
			Registry.register[ComponentProviderNode] { (obj: ComponentProviderNode, ctx: Context) =>
				Some(Data.Table(
					"components" -> Data.List(obj.components.map(_.link).toSeq: _*)
				))
			}
			Registry.register[impl.Network]((obj: impl.Network, ctx: Context) => {
				Some(Data.Table(
					"nodes" -> Data.List(obj.nodes.map(node => {
						Data.Link(s"${mod.ref.modID}:node", Data.String(node.uuid.toString))
					}).toSeq: _*),
					"buses" -> Data.List(obj.buses.values.map(_.link).toSeq: _*)
				))
			})
			Registry.register[ComponentBus] { (obj: ComponentBus, ctx: Context) =>
				Some(Data.Table(
					"components" -> Data.List(obj.components.values.map(_.link).toSeq: _*)
				))
			}
			Registry.register[Component] { (obj: Component, ctx: Context) =>
				Some(Data.Table(
					"methods" -> Data.Table(obj.methods.map { kv =>
						kv._1 -> Data.String(kv._2.usage)
					}.toSeq: _*)
				))
			}
		}
	}

	override def init(e: FMLInitializationEvent) {
		waila.foreach(_.init(e))
	}
}
