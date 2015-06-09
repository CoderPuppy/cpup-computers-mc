package cpup.mc.computers.network.component

import org.luaj.vm2.Varargs

trait Method {
	def usage: String
	def call(args: Varargs): Varargs
}
