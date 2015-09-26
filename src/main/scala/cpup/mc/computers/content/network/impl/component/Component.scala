package cpup.mc.computers.content.network.impl.component

import java.util.UUID

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

import cpup.lib.reflect.ReflectUtil
import cpup.mc.computers.content.network.impl.Node
import cpup.mc.lib.inspecting.Registry.IDed
import net.minecraft.nbt.NBTTagCompound

trait Component extends IDed with Serializable {
	def ownerNode: Node
	def nodes: List[Node] = List()
	def methods: Map[String, Method]

	def writeToNBT(nbt: NBTTagCompound) {
		nbt.setString("uuid", uuid.toString)
	}

	def readFromNBT(nbt: NBTTagCompound) {
		changeUUID(UUID.fromString(nbt.getString("uuid")))
	}
}

object Component {
	def fromAnnotations[C](_node: Node, obj: C)(implicit tt: ru.TypeTag[C]) = {
		val rm = ru.runtimeMirror(getClass.getClassLoader)
		implicit val classTag = ClassTag[C](rm.runtimeClass(tt.tpe))
		val mirror = rm.reflect(obj)
		val tree = tt.tpe.typeSymbol.annotations.map(_.tree).find(_.tpe =:= ru.typeOf[ComponentAnnotation]).get
		val data = ReflectUtil.annotation(tree)._2

		val _nodes = tt.tpe.members.map(_.asTerm).filter { decl =>
			ReflectUtil.findAnnotation[ComponentAnnotation.InternalNode](decl) match {
				case Some(a) if decl.typeSignature.paramLists.isEmpty && decl.typeSignature.finalResultType <:< ru.typeOf[Node] => true
				case _ => false
			}
		}.map { decl =>
			if(decl.isMethod)
				mirror.reflectMethod(decl.asMethod).apply()
			else if(decl.isVar || decl.isVal)
				mirror.reflectField(decl).get
			else
				throw new RuntimeException("bad")
		}.toList.asInstanceOf[List[Node]]

		for {
			_decl <- tt.tpe.members if _decl.isTerm
			decl = _decl.asTerm if decl.isVar && decl.typeSignature =:= ru.typeOf[Node]
			anno <- ReflectUtil.findAnnotation[ComponentAnnotation.Node](decl)
			m = mirror.reflectField(decl)
		} {
			m.set(_node)
		}

		val _methods = getMethods(obj)

		new Component {
			override def ownerNode = _node

			override def typ = s"${data("mod").asInstanceOf[String]}:${data("name").asInstanceOf[String]}"

			override def nodes = _nodes

			override def methods = _methods
		}
	}

	def getMethods[C](obj: C)(implicit tt: ru.TypeTag[C]) = tt.tpe.members.flatMap { decl =>
		(decl, ReflectUtil.findAnnotation[ComponentAnnotation.Method](decl)) match {
			case (d, Some(a)) => Some((decl.name.toString, Method.fromAnnotated(obj, d.name.toString)))
			case _ => None
		}
	}.toMap
}
