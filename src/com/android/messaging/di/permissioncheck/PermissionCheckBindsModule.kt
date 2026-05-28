package com.android.messaging.di.permissioncheck

import com.android.messaging.data.permissioncheck.RequiredPermissionsChecker
import com.android.messaging.data.permissioncheck.RequiredPermissionsCheckerImpl
import com.android.messaging.domain.permissioncheck.usecase.DeterminePermissionRequest
import com.android.messaging.domain.permissioncheck.usecase.DeterminePermissionRequestImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PermissionCheckBindsModule {

    @Binds
    @Reusable
    abstract fun bindRequiredPermissionsChecker(
        impl: RequiredPermissionsCheckerImpl,
    ): RequiredPermissionsChecker

    @Binds
    @Reusable
    abstract fun bindDeterminePermissionRequest(
        impl: DeterminePermissionRequestImpl,
    ): DeterminePermissionRequest
}
