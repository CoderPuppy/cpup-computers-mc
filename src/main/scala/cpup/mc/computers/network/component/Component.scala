package cpup.mc.computers.network.component

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

import cpup.lib.reflect.ReflectUtil
import cpup.mc.computers.network.Node
import cpup.mc.lib.inspecting.Registry.IDed

trait Component extends IDed {
	def ownerNode: Node
	def node: Option[Node] = None
	def methods: Map[String, Method]
}

object Component {
	def fromAnnotations[C](_node: Node, obj: C)(implicit tt: ru.TypeTag[C]) = {
		val rm = ru.runtimeMirror(getClass.getClassLoader)
		implicit val classTag = ClassTag[C](rm.runtimeClass(tt.tpe))
		val mirror = rm.reflect(obj)
		val tree = tt.tpe.typeSymbol.annotations.map(_.tree).find(_.tpe =:= ru.typeOf[ComponentAnnotation]).get
		val data = ReflectUtil.annotation(tree)._2
		println(data("mod").asInstanceOf[String], data("name").asInstanceOf[String])

		println(tt.tpe.members.flatMap { decl =>
			(decl, ReflectUtil.findAnnotation[ComponentAnnotation.Node](decl)) match {
				case (d, Some(a)) if d.asTerm.isVar && d.typeSignature =:= ru.typeOf[Node] => Some((d, a))
				case _ => None
			}
		})
		println(tt.tpe.members.flatMap { decl =>
			(decl, ReflectUtil.findAnnotation[ComponentAnnotation.InternalNode](decl)) match {
				case (d, Some(a)) if d.typeSignature.paramLists.isEmpty && d.typeSignature.finalResultType =:= ru.typeOf[Node] =>
					Some((d, a))
				case _ => None
			}
		})
		new Component {
			override def ownerNode = _node
			override def typ = s"${data("mod").asInstanceOf[String]}:${data("name").asInstanceOf[String]}"

			override def methods = tt.tpe.members.flatMap { decl =>
				(decl, ReflectUtil.findAnnotation[ComponentAnnotation.Method](decl)) match {
					case (d, Some(a)) => Some((decl.name.toString, Method.fromAnnotated(obj, d.name.toString)))
					case _ => None
				}
			}.toMap
		}
	}
}
