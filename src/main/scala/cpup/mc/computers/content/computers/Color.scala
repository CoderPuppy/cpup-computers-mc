package cpup.mc.computers.content.computers

sealed trait Color {
	def toByte: Byte
	def getColor(screen: Screen): Int
}
object Color {
	sealed abstract class Base(val color: Int) extends Color {
		override def toByte = bases.indexOf(this).toByte
		override def getColor(screen: Screen) = color
	}
	object Base {
		def unapply(v: Base) = v.color
	}

	case object     Black extends Base(0x000000)
	case object      Gray extends Base(0x4C4C4C)
	case object LightGray extends Base(0x999999)
	case object     White extends Base(0xF0F0F0)

	case object       Red extends Base(0xCC4C4C)
	case object    Orange extends Base(0xF2B233)
	case object    Yellow extends Base(0xDEDE6C)
	case object      Lime extends Base(0x7FCC19)
	case object     Green extends Base(0x57A64E)
	case object      Cyan extends Base(0x4C99B2)
	case object LightBlue extends Base(0x99B2F2)
	case object      Blue extends Base(0x3366CC)
	case object    Purple extends Base(0xB266E5)
	case object   Magenta extends Base(0xE57FD8)
	case object      Pink extends Base(0xF2B2CC)

	case object Brown     extends Base(0x7F664C)

	val bases = List[Color](Black, Gray, LightGray, White, Red, Orange, Yellow, Lime, Green, Cyan, LightBlue, Blue, Purple, Magenta, Pink, Brown)

	case class Palette(index: Int) extends Color {
		assert(index >= 0 && index <= 239)

		override def toByte = (if(index <= 111) index + 16 else -(index - 111)).toByte
		override def getColor(screen: Screen) = screen.palette(index)
	}

	def fromByte(b: Byte) = if(b >= 0 && b < bases.length) {
		bases(b)
	} else if(b >= 16) {
		Palette(b - 16)
	} else {
		Palette(-b + 111)
	}
}
