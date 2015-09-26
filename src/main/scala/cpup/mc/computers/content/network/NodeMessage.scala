package cpup.mc.computers.content.network

import java.io.{DataOutputStream, DataInputStream}
import java.util.UUID

import cpup.mc.computers.CPupComputers
import cpup.mc.computers.content.network.impl.Node
import cpup.mc.lib.inspecting.Registry
import cpup.mc.lib.network.{CPupMessage, CPupNetwork}
import io.netty.buffer.{ByteBufOutputStream, ByteBufInputStream}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.PacketBuffer

class NodeMessage[DATA <: AnyRef](val uuid: UUID, val msg: CPupNetwork.Message, val player: EntityPlayer = null) extends CPupMessage[DATA] {
	def this(player: EntityPlayer, buf: PacketBuffer, data: DATA) = {
		this(
			UUID.fromString(new DataInputStream(new ByteBufInputStream(buf)).readUTF),
			{
				val msg = new CPupNetwork.Message
				msg.fromBytes(buf)
				msg
			},
			player
		)
	}

	override def writeTo(buf: PacketBuffer) {
		new DataOutputStream(new ByteBufOutputStream(buf)).writeUTF(uuid.toString)
		msg.toBytes(buf)
	}

	override def handle(data: DATA) = {
		Registry.ided(s"${CPupComputers.ref.modID}:node", uuid)
			.flatMap {
				case n: Node => n.msgNetwork.asInstanceOf[Option[CPupNetwork[AnyRef]]]
				case _ => None
			}
			.flatMap(_.handle(player, msg))
			.map(new NodeMessage(uuid, _, player))
	}
}

object NodeMessage {
	def network[NODE <: Node, DATA <: AnyRef](node: NODE, net: CPupNetwork[DATA], msgs: Set[Class[_ <: CPupMessage[_ >: NODE <: AnyRef]]])(implicit manifest: Manifest[NODE]) = {
		new CPupNetwork[NODE](s"${net.name}.<node:${node.uuid}>", node, _net => (ctx, msg) => {
			net.send(ctx, new NodeMessage[DATA](node.uuid, msg))
		}, msgs)
	}
}
