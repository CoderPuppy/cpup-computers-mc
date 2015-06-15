package cpup.mc.computers.network.component

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

import cpup.lib.reflect.ReflectUtil
import org.luaj.vm2.Varargs

trait Method {
	def usage: String
	def call(args: Varargs): Varargs
}

object Method {
	def fromAnnotated[I](inst: I, name: String)(implicit tt: ru.TypeTag[I]) = {
		val method = tt.tpe.member(ru.TermName(name)).asMethod
		val rm = ru.runtimeMirror(getClass.getClassLoader)
		implicit val classTag = ClassTag[I](rm.runtimeClass(tt.tpe))
		val mirror = rm.reflect(inst)
		if(method.isOverloaded) {
			println("alternatives", method.alternatives)
			???
		} else {
			println(method)
			val anno = ReflectUtil.findAnnotation[ComponentAnnotation.Method](method).get
			assert(method.paramLists.length <= 1, "must have exactly one parameter list")
			val params = method.paramLists.headOption.map { params =>
				params.map(_.asTerm).zipWithIndex
					.map { p =>
						p._1.name.toString + ": " + p._1.typeSignature.finalResultType.toString + (if(p._1.isParamWithDefault) {
							val member = tt.tpe.member(ru.TermName(s"${method.name}$$default$$${p._2 + 1}"))
							// TODO: better serialization
							" = " + mirror.reflectMethod(member.asMethod).apply().toString
						} else "")
					}
					.mkString(", ")
			}.getOrElse("")
			val _usage = s"($params) - ${anno("usage").asInstanceOf[String]}"
			println(_usage)
			new Method {
				override val usage = _usage

				override def call(args: Varargs): Varargs = ???
			}
		}
	}
}
