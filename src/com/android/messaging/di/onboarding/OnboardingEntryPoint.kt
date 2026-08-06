package com.android.messaging.di.onboarding

import android.app.role.RoleManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface OnboardingEntryPoint {
    fun roleManager(): RoleManager
}
