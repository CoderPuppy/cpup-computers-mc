package cpup.mc.computers.content.computers

import java.io.{DataInputStream, DataOutputStream}
import java.nio.charset.Charset

import cpup.mc.computers.CPupComputers
import cpup.mc.computers.content.network.{NodeMessage, impl}
import cpup.mc.computers.content.network.impl.component.{Component, ComponentAnnotation => ComponentA, ComponentProviderNode}
import cpup.mc.computers.content.network.impl.{Node, NodeHolder}
import cpup.mc.lib.client.imgui.Form
import cpup.mc.lib.client.imgui.IMGUI.State
import cpup.mc.lib.network.{CPupNetwork, CPupMessage}
import cpup.mc.lib.util.{Side, NBTUtil}
import io.netty.buffer.{ByteBufInputStream, ByteBufOutputStream, ByteBuf, Unpooled}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import org.lwjgl.opengl.GL11

class Screen(_host: Node.Host, var maxWidth: Int, var maxHeight: Int) extends Component with NodeHolder with Serializable with Form with Buffer {
	val selfScreen = this

	val node = new Screen.NodeI(_host, this)

	protected var _curWidth  = 0
	protected var _curHeight = 0
	def curWidth = _curWidth
	def curHeight = _curHeight

	def resize(newWidth: Int, newHeight: Int) {
		val (oldWidth, oldHeight) = (_curWidth, _curHeight)
		_curWidth = newWidth
		_curHeight = newHeight
		val oldColors = colors
		val oldData = data
		colors =  Unpooled.buffer(_curWidth * _curHeight * 2)
		data   = Array.fill[Char](_curWidth * _curHeight) { ' ' }
		val  width = Math.min(oldWidth , _curWidth )
		val height = Math.min(oldHeight, _curHeight)
		if(oldColors ne null) {
			for(y <- 0 until height) {
				oldColors.getBytes(fgIndex(0, y), colors, width)
				oldColors.getBytes(bgIndex(0, y), colors, width)
			}
		}
		if(oldData ne null) {
			for(x <- 0 until width; y <- 0 until height) {
				data(charIndex(x, y)) = oldData(charIndex(x, y))
			}
		}
	}

	// FF*
	// BB*
	var colors: ByteBuf = null
	var data: Array[Char] = null

	resize(maxWidth, maxHeight)

	val palette = Array.fill[Int](240) { 0x000000 }
	@ComponentA.Method(usage = "Set a palette color")
	def setPaletteColor(idx: Int, color: Int): Byte = {
		assert(color >= 0x000000 && color <= 0xFFFFFF)
		val c = Color.Palette(idx)
		palette(idx) = color
		c.toByte
	}

	override def typ = s"${CPupComputers.ref.modID}:screen"
	override def ownerNode = node
	override lazy val methods = Component.getMethods(this)

	override def writeToNBT(nbt: NBTTagCompound) {
		super.writeToNBT(nbt)
		node.writeToNBT(NBTUtil.compound(nbt, "node"))
		// TODO: save buffer
	}

	override def readFromNBT(nbt: NBTTagCompound) {
		super.readFromNBT(nbt)
		node.readFromNBT(nbt.getCompoundTag( "node"))
		// TODO: read buffer
	}

	lazy val mc = Minecraft.getMinecraft
	lazy val fr = mc.fontRenderer

	val (charWidth, charHeight) = (6, 10)

	override def width = _curWidth * charWidth
	override def height = _curHeight * charHeight

	override def onUpdate(x: Int, y: Int, width: Int, height: Int) {
		if(Side.effective.isServer) {
			val colors = Unpooled.buffer(width * height * 2)
			val data = Array.ofDim[Char](width * height)
			Buffer.copy((curWidth, curHeight, colors, data), x, y, width, height, (width, height, colors, data), 0, 0)
			node.msgNetwork.get.send(node.host.ctx, new Screen.UpdateMessage(this, x, y, width, height, colors, data))
		}
	}

	def charPos(x: Int, y: Int) = (x * charWidth, y * charHeight)

	override def render(width: Int, height: Int, in: Boolean, state: State, prevState: State) {
		// TODO: cache stuff
		for(_x <- 0 until curWidth; _y <- 0 until curHeight) {
			val (x_, y_) = charPos(_x, _y)

	//				println("rendering", x + _x, y + _y)

			val tess = Tessellator.instance
			GL11.glDisable(GL11.GL_TEXTURE_2D)
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
			tess.startDrawingQuads
			tess.setColorOpaque_I(bg(_x, _y).getColor(this))
			tess.addVertex(x_            , y_ + charHeight, 0)
			tess.addVertex(x_ + charWidth, y_ + charHeight, 0)
			tess.addVertex(x_ + charWidth, y_             , 0)
			tess.addVertex(x_            , y_             , 0)
			tess.draw
			GL11.glEnable(GL11.GL_TEXTURE_2D)

			fr.drawString(char(_x, _y).toString, x_, y_, fg(_x, _y).getColor(this))
		}
	}
}

object Screen {
	trait Node extends impl.Node {
		def screen: Screen
	}

	class NodeI(override val host: impl.Node.Host, override val screen: Screen) extends impl.Node with Node with ComponentProviderNode {
		override def components = Set(screen)
		override def msgNetwork = Some(NodeMessage.network[Node, AnyRef](this, CPupComputers.net, Set(
			classOf[Screen.UpdateMessage]
		)))
	}

	class UpdateMessage(val screen: Screen, _x: Int, _y: Int, _width: Int, _height: Int, val colors: ByteBuf, val data: Array[Char]) extends CPupMessage[Node] {
		val x = Math.max(_x, 0)
		val y = Math.max(_y, 0)
		val width = Math.min(_width, screen.curWidth - x)
		val height = Math.min(_height, screen.curHeight - y)
		assert(colors.capacity == width * height * 2)
		assert(data.length == width * height)

		def this(player: EntityPlayer, buf: PacketBuffer, node: Node) {
			this(
				node.screen,
				buf.readInt, buf.readInt, // pos
				buf.readInt, buf.readInt, // dim
				buf.readBytes(buf.readInt), // colors
				{
					val bytes = Array.ofDim[Byte](buf.readInt)
					buf.readBytes(bytes)
					new String(bytes, CPupNetwork.charset).toCharArray
				}
//				new DataInputStream(new ByteBufInputStream(buf)).readUTF.toCharArray // data
			)
		}

		override def writeTo(buf: PacketBuffer) {
			buf.writeInt(x)
			buf.writeInt(y)
			buf.writeInt(width)
			buf.writeInt(height)
			buf.writeInt(colors.capacity)
			colors.writerIndex(colors.capacity)
			buf.writeBytes(colors, colors.capacity)
			val bytes = data.mkString.getBytes(CPupNetwork.charset)
			buf.writeInt(bytes.length)
			buf.writeBytes(bytes)
//			new DataOutputStream(new ByteBufOutputStream(buf)).writeUTF(data.mkString)
		}

		override def handle(data: Node) = {
			Buffer.copy((width, height, colors, this.data), 0, 0, width, height, (data.screen.curWidth, data.screen.curHeight, data.screen.colors, data.screen.data), x, y)
			None
		}
	}
}
