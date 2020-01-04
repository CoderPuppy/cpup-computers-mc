package cpup.mc.computers.content.computers

import java.io.{DataInputStream, DataOutputStream}

import scala.collection.mutable

import cpup.mc.computers.CPupComputers
import cpup.mc.computers.content.network.impl.component.{ComponentAnnotation => ComponentA, ComponentSensitiveNode, ComponentBus, Component, ComponentProviderNode}
import cpup.mc.computers.content.network.impl.{Network, Node, NodeHolder}
import cpup.mc.computers.content.network.{NodeMessage, impl}
import cpup.mc.lib.network.{CPupMessage, CPupNetwork}
import cpup.mc.lib.util.{TickUtil, Side}
import io.netty.buffer.{ByteBufInputStream, ByteBufOutputStream, Unpooled}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.PacketBuffer

class GPU(_host: Node.Host, val width: Int, val height: Int) extends Component with NodeHolder with Serializable with Buffer {
	def selfGPU = this
	val node = new Node with ComponentProviderNode with ComponentSensitiveNode {
		override def components = Set(selfGPU)

		override def onRmComponent(comp: Component) {
			comp match {
				case s: Screen => selfGPU.bindings.remove(s)
				case _ =>
			}
		}

		override def host = _host
	}

	def curWidth = width
	def curHeight = height

	// FF*
	// BB*
	val colors = Unpooled.buffer(width * height * 2)
	val data = Array.fill[Char](width * height) { ' ' }

	val bindings = mutable.Map.empty[Screen, (Int, Int, Int, Int)]

	@ComponentA.Method(usage = "Bind the screen to part of the buffer")
	def bind(screen: Screen, x: Int, y: Int, width: Int, height: Int) {
		if(Side.effective.isServer)
			assert(node.network.bus[ComponentBus].components.contains(screen.uuid))
		bindings(screen) = (x, y, width, height)
	}

	@ComponentA.Method(usage = "Unbind a screen")
	def unbind(screen: Screen) {
		bindings.remove(screen)
	}

	override def onUpdate(x: Int, y: Int, width: Int, height: Int) {
		for((screen, (_x, _y, _width, _height)) <- bindings) {
			if(_x <= x +  width && x <= _x +  _width &&
			   _y <= y + height && y <= _y + _height) {
				val sx = Math.max(x, _x)
				val sy = Math.max(y, _y)
				val ox = sx - _x
				val oy = sy - _y
				val owidth = Math.min(Math.max(x + width, _x + _width) - sx, screen.curWidth - ox)
				val oheight = Math.min(Math.max(y + height, _y + _height) - sy, screen.curHeight - oy)
				copyTo(sx, sy, owidth, oheight, screen, ox, oy)
//				for(y <- 0 until oheight) {
//					colors.getBytes(fgIndex(sx, sy + y), screen.colors, screen.fgIndex(ox, oy + y), owidth)
//					colors.getBytes(bgIndex(sx, sy + y), screen.colors, screen.bgIndex(ox, oy + y), owidth)
//					for(x <- 0 until owidth) {
//						screen.data(screen.charIndex(ox + x, oy + y)) = data(charIndex(sx + x, sy + y))
//					}
//				}
			}
		}
	}

	@ComponentA.Method(usage = "Write data to the GPU's buffer")
	override def write(x: Int, y: Int, fg: Color, bg: Color, text: String) = super.write(x, y, fg, bg, text)

	@ComponentA.Method(usage = "Copy data from one part of the buffer to another part")
	def copy(x: Int, y: Int, width: Int, height: Int, ox: Int, oy: Int) = super.copyTo(x, y, width, height, this, ox, oy)

	@ComponentA.Method(usage = "Fill a section of the buffer")
	override def fill(x: Int, y: Int, width: Int, height: Int, fg: Color, bg: Color, char: Char) = super.fill(x, y, width, height, fg, bg, char)

	override def typ = s"${CPupComputers.ref.modID}:gpu"
	override def ownerNode = node
	override lazy val methods = Component.getMethods(this)
}
