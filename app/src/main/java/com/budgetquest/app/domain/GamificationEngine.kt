package com.budgetquest.app.domain

import com.budgetquest.app.data.local.entity.UserEntity
import java.util.Calendar

/**
 * Stateless rules engine for the gamification layer.
 *
 * Implemented as an `object` (singleton) because it holds no state — it is a pure
 * collection of scoring rules. Keeping these calculations in one place means the
 * "how many XP / what level / what title" logic can be unit-tested in isolation
 * (see GamificationEngineTest) and reused by any screen without duplication.
 */
object GamificationEngine {

    // Tunable reward constants — central place to balance the game economy.
    const val XP_PER_EXPENSE = 50
    const val XP_PER_DAILY_BUDGET_SAVED = 100
    const val XP_FOR_STREAK_7_DAYS = 200

    /** Levels are earned every 500 XP; level is 1-based (0 XP == level 1). */
    fun calculateLevel(xp: Int): Int {
        return (xp / 500) + 1
    }

    /** Total XP threshold required to reach the next level. */
    fun getXPForNextLevel(level: Int): Int {
        return level * 500
    }

    /**
     * Recomputes a user's daily streak. If the user's last activity was exactly
     * yesterday the streak continues (+1); any longer gap resets it to 1. Hitting a
     * multiple of 7 days awards a bonus to reward sustained engagement.
     */
    fun updateStreak(user: UserEntity): UserEntity {
        val lastActivity = Calendar.getInstance().apply { timeInMillis = user.lastActivityDate }
        val now = Calendar.getInstance()

        // Check if yesterday
        lastActivity.add(Calendar.DAY_OF_YEAR, 1)
        val isConsecutive = lastActivity.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                lastActivity.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        val newStreak = if (isConsecutive) user.streakCount + 1 else 1
        return user.copy(
            streakCount = newStreak,
            lastActivityDate = now.timeInMillis,
            xp = user.xp + (if (newStreak % 7 == 0) XP_FOR_STREAK_7_DAYS else 0)
        )
    }

    /** Maps a numeric level to its themed rank title shown in the UI. */
    fun getLevelName(level: Int): String {
        return when {
            level < 5 -> "Budget Novice"
            level < 10 -> "Frugal Squire"
            level < 20 -> "Finance Knight"
            else -> "Financial Expert"
        }
    }
}
