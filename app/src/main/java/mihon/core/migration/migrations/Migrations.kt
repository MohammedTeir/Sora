package mihon.core.migration.migrations

import mihon.core.migration.Migration

val migrations: List<Migration>
    get() = listOf(
        SetupBackupCreateMigration(),
        SetupLibraryUpdateMigration(),
        SetupDefaultExtensionReposMigration(),
        TrustExtensionRepositoryMigration(),
        CategoryPreferencesCleanupMigration(),
    )
