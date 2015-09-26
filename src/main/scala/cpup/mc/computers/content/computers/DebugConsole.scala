package cpup.mc.computers.content.computers

import java.util

import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

import cpup.mc.computers.content.network.impl.component.{Component, ComponentSensitiveNode}
import cpup.mc.computers.content.network.impl.network.NetworkSensitiveMode
import cpup.mc.computers.content.network.impl.{Node, NodeTE}
import cpup.mc.computers.content.{BaseBlockContainer, BaseGUI, BaseTE}
import cpup.mc.lib.client.imgui
import cpup.mc.lib.client.imgui.{IMGUI, Label, Widget}
import cpup.mc.lib.inspecting.Request
import cpup.mc.lib.util.{Side, NBTUtil}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.gui.{GuiButton, GuiScreen}
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import org.luaj.vm2.Varargs
import org.lwjgl.input.{Keyboard => OKeyboard, Mouse}
import org.lwjgl.opengl.GL11

object DebugConsole extends Block(Material.iron) with BaseBlockContainer {
	def selfBlock = this

	name = "debug-console"

	class TE extends BaseTE with NodeTE.Simple with Node.Host {
		def selfTE = this
		val luaGPU = new GPU(this, 80, 50)
		val luaScreen = new Screen(this, 80, 50)
		val keyboard = new Keyboard(this)
		val signalO = new Output(this, 80, 25)
		val messageO = new Output(this, 80, 25)

		override val node = new Node with NetworkSensitiveMode with ComponentSensitiveNode {
			def host = selfTE

			override def onMessage(lastHop: Node, from: Node, data: String*) {
				messageO.write(s"${from.uuid}: ${data.mkString(" - ")}\n")
			}

			override def onSignal(comp: Component, name: String, args: Varargs) {
				signalO.write(s"${comp.uuid}: ${(0 until args.narg).map(args.arg).map(LuaPrettyPrint.prettyPrint(_)).mkString(" - ")}\n")
			}
		}

		node.connect(luaGPU.node)
		node.connect(luaScreen.node)
		node.connect(keyboard.node)
		node.connect(signalO.node)
		node.connect(messageO.node)
		luaGPU.bind(luaScreen, 0, 0, 80, 50)

		override def ctx = NodeTE.ctx(this)
		override def get[T](id: Symbol)(implicit tt: ru.TypeTag[T]) = None

		override def writeToNBT(nbt: NBTTagCompound) {
			super.writeToNBT(nbt)
			luaGPU.writeToNBT(NBTUtil.compound(nbt, "lua-gpu"))
			luaScreen.writeToNBT(NBTUtil.compound(nbt, "lua-screen"))
			keyboard.writeToNBT(NBTUtil.compound(nbt, "keyboard"))
			signalO.writeToNBT(NBTUtil.compound(nbt, "signal-output"))
			messageO.writeToNBT(NBTUtil.compound(nbt, "message-output"))
		}

		override def readFromNBT(nbt: NBTTagCompound): Unit = super.readFromNBT(nbt)
	}

	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
		super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
		onBlockClicked(world, x, y, z, player)
		true
//		println("activated")
//		false
	}

	override def onBlockClicked(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) {
		super.onBlockClicked(world, x, y, z, player)
		if(Side.effective.isServer) {
			val te = world.getTileEntity(x, y, z).asInstanceOf[TE]
			te.node.onMessage(te.node, te.node, "hi")
		}
		mod.gui.open(player, world, x, y, z, GUI)
	}

	override def hasTileEntity(metadata: Int) = true
	override def createNewTileEntity(world: World, meta: Int) = new TE

	class Output(_host: Node.Host, val width: Int, val height: Int) extends Serializable {
		val node = new Node {
			override def host = _host
		}
		val gpu = new GPU(_host, width, height)
		val screen = new Screen(_host, width, height)
		var fg: Color = Color.White
		var bg: Color = Color.Black
		var cursorX = 0
		var cursorY = 0

		node.connect(gpu.node)
		node.connect(screen.node)
		gpu.bind(screen, 0, 0, width, height)

		def write(text: String) {
			val lines = (text + "s").split("\n")
			lines(lines.length - 1) = {
				val str = lines.last
				str.substring(0, str.length - 1)
			}
			for((line, i) <- lines.view.zipWithIndex) {
				gpu.write(cursorX, cursorY, fg, bg, line)
				if(i == lines.length - 1) {
					cursorX += line.length
				} else {
					cursorY += 1
					cursorX = 0
				}
			}
		}

		def writeToNBT(nbt: NBTTagCompound) {
			node.writeToNBT(NBTUtil.compound(nbt, "node"))
			gpu.writeToNBT(NBTUtil.compound(nbt, "gpu"))
			screen.writeToNBT(NBTUtil.compound(nbt, "screen"))
		}

		def readFromNBT(nbt: NBTTagCompound) {
			node.readFromNBT(nbt.getCompoundTag("node"))
			gpu.readFromNBT(nbt.getCompoundTag("gpu"))
			screen.readFromNBT(nbt.getCompoundTag("screen"))
		}
	}

	object GUI extends BaseGUI {
		override def name = selfBlock.name

		override def container(player: EntityPlayer, world: World, x: Int, y: Int, z: Int) = null
		override def clientGUI(player: EntityPlayer, world: World, x: Int, y: Int, z: Int) = new Screen(world.getTileEntity(x, y, z).asInstanceOf[TE])

		class Screen(val te: TE) extends GuiScreen {
			override def actionPerformed(btn: GuiButton) {
				super.actionPerformed(btn) match {
					case _ =>
				}
			}

			override def initGui {
				super.initGui
			}

			val gui = new IMGUI

			override def handleMouseInput {
				super.handleMouseInput
				val mx = Mouse.getEventX * this.width / this.mc.displayWidth
				val my = this.height - Mouse.getEventY * this.height / this.mc.displayHeight - 1
				val btn = Mouse.getEventButton
				val down = Mouse.getEventButtonState
				gui.updateMouse(mx, my, btn, down)
			}

			override def handleKeyboardInput {
				super.handleKeyboardInput
				gui.updateKeyboard(OKeyboard.getEventKey, OKeyboard.getEventCharacter, OKeyboard.getEventKeyState, OKeyboard.isRepeatEvent)
			}

			override def drawScreen(mx: Int, my: Int, tick: Float) {
				GL11.glPushMatrix
				GL11.glDisable(GL11.GL_LIGHTING)
				GL11.glDisable(GL11.GL_FOG)
//				val tess = Tessellator.instance
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
//				GL11.glDisable(GL11.GL_TEXTURE_2D)
//				tess.startDrawingQuads
//				tess.setColorOpaque_I(0x888888)
//				tess.addVertex(0, height, 0)
//				tess.addVertex(width, height, 0)
//				tess.addVertex(width, 0, 0)
//				tess.addVertex(0, 0, 0)
//				tess.draw
//				GL11.glEnable(GL11.GL_TEXTURE_2D)
				super.drawScreen(mx, my, tick)
				gui.pushMatrix
				gui(Widget(te.luaScreen))
				gui(Widget(te.messageO.screen, x = width / 2))
				gui(Widget(te.signalO.screen, x = width / 2, y = height / 2))
				gui.popMatrix
				gui.tick
				GL11.glPopMatrix
			}

			override def doesGuiPauseGame = false
		}
	}
}
