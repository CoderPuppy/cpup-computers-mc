package cpup.mc.computers.network.component

import org.luaj.vm2.Varargs

trait ComponentSensitiveNode {
	def onAddComponent(comp: Component) {}
	def onRmComponent(comp: Component) {}
	def onSignal(comp: Component, name: String, args: Varargs) {}
}
