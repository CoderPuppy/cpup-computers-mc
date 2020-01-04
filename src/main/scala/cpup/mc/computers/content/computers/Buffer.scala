package cpup.mc.computers.content.computers

import io.netty.buffer.ByteBuf

trait Buffer {
	def curWidth: Int
	def curHeight: Int
	def colors: ByteBuf
	def data: Array[Char]

	def fgIndex =  Buffer.fgIndex(curWidth)_
	def bgIndex = Buffer.bgIndex(curWidth)_
	def charIndex = Buffer.charIndex(curWidth)_

	def char(x: Int, y: Int) = data(charIndex(x, y))
	def fg(x: Int, y: Int) = Color.fromByte(colors.getByte(fgIndex(x, y)))
	def bg(x: Int, y: Int) = Color.fromByte(colors.getByte(bgIndex(x, y)))

	def onUpdate(x: Int, y: Int, width: Int, height: Int) {}

	def copyTo(_x: Int, _y: Int, _width: Int, _height: Int, other: Buffer, _dx: Int, _dy: Int) {
		val (_, _, width, height, dx, dy) = Buffer.copy((curWidth, curHeight, colors, data), _x, _y, _width, _height, (other.curWidth, other.curHeight, other.colors, other.data), _dx, _dy)
		other.onUpdate(dx, dy, width, height)
	}

	def write(x_ : Int, y_ : Int, fg: Color, bg: Color, text: String) {
		val x = Math.max(x_, 0)
		val y = Math.max(y_, 0)
		val _width = Math.min(text.length, curWidth - x)
		for(i <- 0 until _width) {
			colors.setByte(fgIndex(x + i, y), fg.toByte)
			colors.setByte(bgIndex(x + i, y), bg.toByte)
			data(charIndex(x + i, y)) = text(i)
		}
		onUpdate(x, y, _width, 1)
	}

	def fill(x_ : Int, y_ : Int, width_ : Int, height_ : Int, fg: Color, bg: Color, char: Char) {
		val x = Math.max(x_, 0)
		val y = Math.max(y_, 0)
		val _width = Math.min(width_, curWidth - x)
		val _height = Math.min(height_, curHeight - y)
		for(_x <- x until x + _width; _y <- y until y + _width) {
			colors.setByte(fgIndex(_x, _y), fg.toByte)
			colors.setByte(bgIndex(_x, _y), bg.toByte)
			data(charIndex(_x, _y)) = char
		}
		onUpdate(x, y, _width, _height)
	}
}

object Buffer {
	def fgIndex(width: Int)(x: Int, y: Int) =  y * 2      * width + x
	def bgIndex(width: Int)(x: Int, y: Int) = (y * 2 + 1) * width + x
	def charIndex(width: Int)(x: Int, y: Int) = y * width + x
	def copy(from: (Int, Int, ByteBuf, Array[Char]), _x: Int, _y: Int, _width: Int, _height: Int, dest: (Int, Int, ByteBuf, Array[Char]), _dx: Int, _dy: Int): (Int, Int, Int, Int, Int, Int) = {
		val x = Math.max(_x, 0)
		val y = Math.max(_y, 0)
		val dx = Math.max(_dx, 0)
		val dy = Math.max(_dy, 0)
		val width = Math.min(Math.min(_width, dest._1 - dx), from._1 - x)
		val height = Math.min(Math.min(_height, dest._2 - dy), from._2 - y)
		for(_y <- 0 until height) {
			val idx = fgIndex(from._1)(x, y + _y) + width
			from._3.getBytes(fgIndex(from._1)(x, y + _y), dest._3, fgIndex(dest._1)(dx, dy + _y), width)
			from._3.getBytes(bgIndex(from._1)(x, y + _y), dest._3, bgIndex(dest._1)(dx, dy + _y), width)
			for(_x <- 0 until width) {
				dest._4(charIndex(dest._1)(dx + _x, dy + _y)) = from._4(charIndex(from._1)(x + _x, y + _y))
			}
		}
		(x, y, width, height, dx, dy)
	}
}
