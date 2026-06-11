package com.budgetquest.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for BudgetQuest.
 *
 * The [HiltAndroidApp] annotation triggers Hilt's code generation, creating the
 * application-level dependency container that every Activity, ViewModel and
 * Repository draws from. This class is instantiated by the Android runtime before
 * any other component, so it is the first place our own code runs.
 */
@HiltAndroidApp
class BudgetQuestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Logged once per process start — useful for confirming the app launched
        // and the Hilt dependency graph was initialised correctly.
        Log.i(TAG, "BudgetQuest application started and dependency graph initialised")
    }

    companion object {
        private const val TAG = "BudgetQuestApp"
    }
}
