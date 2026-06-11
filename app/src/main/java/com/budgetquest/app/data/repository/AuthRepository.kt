package com.budgetquest.app.data.repository

import android.util.Log
import com.budgetquest.app.data.local.dao.BudgetDao
import com.budgetquest.app.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for all authentication and user-account operations.
 *
 * It is the single source of truth for credentials: the rest of the app never
 * touches the [BudgetDao] for user data directly. Passwords are never stored or
 * logged in plain text — they are hashed with BCrypt (a deliberately slow,
 * salted algorithm) before persistence, and only the resulting hash is kept.
 *
 * Marked [Singleton] so Hilt provides one shared instance across the app.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    /**
     * Creates a new account. Returns false if the username is already taken so the
     * UI can surface a friendly error instead of crashing on a unique-key conflict.
     * Runs on [Dispatchers.IO] because database access must never block the UI thread.
     */
    suspend fun register(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "register() called for username='$username'")

        val existingUser = budgetDao.getUserByUsername(username)
        if (existingUser != null) {
            // Username collision — abort before hashing to save work.
            Log.w(TAG, "register() failed: username '$username' already exists")
            return@withContext false
        }

        // Hash the password with a freshly generated salt. We log only that hashing
        // occurred, never the password or the hash itself.
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        val newUser = UserEntity(
            username = username,
            passwordHash = passwordHash,
            createdAt = System.currentTimeMillis()
        )
        budgetDao.insertUser(newUser)
        Log.i(TAG, "register() succeeded: new account created for '$username'")
        true
    }

    /**
     * Validates a login attempt. The supplied password is re-hashed and compared
     * against the stored hash via [BCrypt.checkpw]; we never decrypt anything.
     * On success the previous session is cleared so only one user is "logged in".
     */
    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "login() attempt for username='$username'")

        val user = budgetDao.getUserByUsername(username)
        if (user == null) {
            Log.w(TAG, "login() failed: no account found for '$username'")
            return@withContext false
        }

        return@withContext if (BCrypt.checkpw(password, user.passwordHash)) {
            budgetDao.clearLoginStatus()                 // ensure only one active session
            budgetDao.updateUser(user.copy(isLoggedIn = true))
            Log.i(TAG, "login() succeeded for '$username'")
            true
        } else {
            Log.w(TAG, "login() failed: incorrect password for '$username'")
            false
        }
    }

    /** Ends the current session by clearing the logged-in flag for all users. */
    suspend fun logout() = withContext(Dispatchers.IO) {
        Log.i(TAG, "logout() called — clearing active session")
        budgetDao.clearLoginStatus()
    }

    /** Exposes the currently logged-in user as a reactive Flow for the UI layer. */
    fun getCurrentUser() = budgetDao.getUser()

    suspend fun updateXP() {
        // Reserved for future XP-only updates; XP is currently persisted via updateUser().
    }

    /** Persists changes to a user record (XP, level, streaks, goals, etc.). */
    suspend fun updateUser(user: UserEntity) {
        Log.d(TAG, "updateUser() persisting changes for userId=${user.id} (xp=${user.xp}, level=${user.level})")
        budgetDao.updateUser(user)
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
