package ram.talia.hexal.common.casting.actions.spells.gates

import at.petrak.hexcasting.api.spell.ConstMediaAction
import at.petrak.hexcasting.api.spell.asActionResult
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import ram.talia.hexal.api.getGate

object OpGetNumMarkedGate : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, ctx: CastingContext): List<Iota> {
        val gate = args.getGate(0, argc)

        return gate.numMarked.asActionResult
    }
}