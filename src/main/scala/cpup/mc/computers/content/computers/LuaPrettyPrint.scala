package cpup.mc.computers.content.computers

import scala.annotation.tailrec
import scala.collection.mutable

import org.luaj.vm2.{LuaTable, LuaString, LuaBoolean, LuaDouble, LuaInteger, LuaNumber, LuaNil, LuaValue}
import cpup.mc.lib.inspecting.Registry.Data

object LuaPrettyPrint {
	def prettyPrint(v: LuaValue, _visited: Set[LuaValue] = Set.empty[LuaValue]): String = {
		if(_visited.contains(v)) throw new StackOverflowError("recursive serialization")
		val visited = _visited + v
		v match {
			case v: LuaNil => "nil"
			case v: LuaInteger => v.v.toString
			case v: LuaDouble => v.todouble.toString
			case v: LuaBoolean => v.v.toString
			case v: LuaString => Data.String(v.tojstring).toString
			case v: LuaTable => {
				val list = mutable.ListBuffer.empty[LuaValue]
				val kvs = mutable.Map.empty[LuaValue, LuaValue]
				var key = LuaValue.NIL
				do {
					val args = v.inext(key)
					key = args.arg(0)
					if(key.checkint == list.length - 1)
						list += args.arg(1)
					else
						kvs(key) = args.arg(1)
				} while(!key.isnil)
				do {
					val args = v.next(key)
					key = args.arg(0)
					kvs(key) = args.arg(1)
				} while(!key.isnil)
				if(list.isEmpty && kvs.isEmpty) {
					"{}"
				} else {
					val res = mutable.StringBuilder.newBuilder
					res ++= "{\n"
					for(v <- list) {
						res ++= prettyPrint(v, visited).split("\n").map("  " + _).mkString("\n")
						res ++= ";\n"
					}
					for(kv <- kvs) {
						res += '['
						res ++= prettyPrint(kv._1, visited).split("\n").map("  " + _).mkString("\n")
						res ++= "] = "
						res ++= prettyPrint(kv._2, visited).split("\n").map("  " + _).mkString("\n")
						res ++= ";\n"
					}
					res += '}'
					res.mkString
				}
			}
		}
	}
	def apply(v: LuaValue) = prettyPrint(v)
}
