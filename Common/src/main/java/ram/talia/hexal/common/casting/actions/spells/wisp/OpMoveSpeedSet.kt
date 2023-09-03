package ram.talia.hexal.common.casting.actions.spells.wisp

import at.petrak.hexcasting.api.spell.*
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import ram.talia.hexal.api.config.HexalConfig
import ram.talia.hexal.api.spell.casting.IMixinCastingContext
import ram.talia.hexal.api.spell.mishaps.MishapNoWisp
import ram.talia.hexal.common.entities.TickingWisp
import kotlin.math.ln
import kotlin.math.min

object OpMoveSpeedSet : SpellAction {
    override val argc = 1

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun execute(args: List<Iota>, ctx: CastingContext): Triple<RenderedSpell, Int, List<ParticleSpray>> {
        val newMax = args.getPositiveDouble(0, OpMoveTargetSet.argc)
        val newMult = newMax / TickingWisp.BASE_MAX_SPEED_PER_TICK

        val mCast = ctx as? IMixinCastingContext

        if (mCast == null || !mCast.hasWisp() || mCast.wisp !is TickingWisp)
            throw MishapNoWisp()

        val tWisp = mCast.wisp as TickingWisp
        val oldMax = tWisp.maximumMoveMultiplier

        // cost scales quadratically with newMult, but as with Impulse a player can reduce the cost requirement with
        // many small increases to the max (limited to a minimum cost of moveSpeedSetCost if the maximum is being updated at
        // all to not incentivise very laggy stuff). If the newMult is older than the old max, there is no cost;
        // reducing speed is not penalised.
        val cost = if (newMult > oldMax) {
            (HexalConfig.server.moveSpeedSetCost * min(1.0, (newMult - oldMax) * (newMult - oldMax))).toInt()
        } else 0

        return Triple(
                Spell(tWisp, newMult),
                cost,
                listOf(ParticleSpray.burst(mCast.wisp!!.position(), min(1.0, ln(newMult))))
        )
    }

    private data class Spell(val wisp: TickingWisp, val newMult: Double) : RenderedSpell {
        override fun cast(ctx: CastingContext) {
            wisp.currentMoveMultiplier = newMult.toFloat()
        }
    }
}