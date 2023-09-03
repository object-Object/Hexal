package ram.talia.hexal.common.casting.actions.spells.great

import at.petrak.hexcasting.api.spell.*
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import at.petrak.hexcasting.api.spell.mishaps.MishapInvalidIota
import com.mojang.datafixers.util.Either
import net.minecraft.server.level.ServerPlayer
import ram.talia.hexal.api.HexalAPI
import ram.talia.hexal.api.config.HexalConfig
import ram.talia.hexal.api.spell.casting.IMixinCastingContext
import ram.talia.hexal.common.entities.BaseCastingWisp
import ram.talia.hexal.common.entities.IMediaEntity
import kotlin.math.ln

object OpConsumeWisp : SpellAction {
	override val argc = 1

	override val isGreat = true

	@Suppress("CAST_NEVER_SUCCEEDS")
	override fun execute(args: List<Iota>, ctx: CastingContext): Triple<RenderedSpell, Int, List<ParticleSpray>> {
		val consumed = args.getEntity(0, argc) as? IMediaEntity<*> ?: throw MishapInvalidIota.ofType(args[0], 0, "consumable_entity")

		ctx.assertEntityInRange(consumed.get())

		val mCast = ctx as? IMixinCastingContext

		val consumer: Either<BaseCastingWisp, ServerPlayer> = if (mCast != null && mCast.wisp != null) Either.left(mCast.wisp) else Either.right(ctx.caster)

		HexalAPI.LOGGER.debug("consumer: {}, {}", consumer, consumed.fightConsume(consumer))

		val cost = when (consumed.fightConsume(consumer)) {
			false  -> HexalConfig.server.consumeWispOwnCost
			true   -> (HexalConfig.server.consumeWispOthersCostPerMedia * consumed.media).toInt()
		}

		HexalAPI.LOGGER.debug("cost to consume {} is {}", consumed, cost)

		return Triple(
			Spell(consumed),
			cost,
			listOf(ParticleSpray.burst(consumed.get().position(), 1.0, (ln(10.0) * 14 * ln(consumed.media/10.0 + 1)).toInt()))
		)
	}

	private data class Spell(val consumed: IMediaEntity<*>) : RenderedSpell {
		@Suppress("CAST_NEVER_SUCCEEDS")
		override fun cast(ctx: CastingContext) {
			HexalAPI.LOGGER.debug("cast method of Spell of OpConsumeWisp triggered targeting {}", consumed)

			val mCast = ctx as? IMixinCastingContext

			if (mCast != null && mCast.wisp != null)
				mCast.wisp!!.addMedia(19 * consumed.media / 20) // using addMedia to prevent overflow.
			else if (mCast != null)
				mCast.consumedMedia += 19 * consumed.media / 20

			HexalAPI.LOGGER.debug("discarding {}", consumed)
			consumed.get().discard()
		}
	}
}