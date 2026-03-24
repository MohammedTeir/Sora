package mihon.core.migration.migrations

import eu.kanade.domain.source.service.SourcePreferences
import logcat.LogPriority
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.domain.extensionrepo.exception.SaveExtensionRepoException
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

class SetupDefaultExtensionReposMigration : Migration {
    override val version: Float = Migration.ALWAYS

    private val defaultRepos = listOf(
        DefaultRepo(
            baseUrl = "https://raw.githubusercontent.com/komikku-app/extensions/repo",
            name = "Komikku Extensions",
            shortName = "Komikku",
            website = "https://github.com/komikku-app/extensions",
        ),
        DefaultRepo(
            baseUrl = "https://raw.githubusercontent.com/MiguelMA3/mihon-extensions/refs/heads/mypack",
            name = "MiguelMA3 Extensions",
            shortName = "MiguelMA3",
            website = "https://github.com/MiguelMA3/mihon-extensions",
        ),
        DefaultRepo(
            baseUrl = "https://raw.githubusercontent.com/ThePBone/tachiyomi-extensions-revived/repo",
            name = "Tachiyomi Extensions Revived",
            shortName = "Revived",
            website = "https://github.com/ThePBone/tachiyomi-extensions-revived",
        ),
    )

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val sourcePreferences = migrationContext.get<SourcePreferences>() ?: return@withIOContext false
        val repository = migrationContext.get<ExtensionRepoRepository>() ?: return@withIOContext false

        if (sourcePreferences.hasSeededDefaultExtensionRepos().get()) {
            return@withIOContext true
        }

        for ((index, repo) in defaultRepos.withIndex()) {
            try {
                repository.upsertRepo(
                    repo.baseUrl,
                    repo.name,
                    repo.shortName,
                    repo.website,
                    "NOFINGERPRINT-default-${index + 1}",
                )
            } catch (e: SaveExtensionRepoException) {
                logcat(LogPriority.ERROR, e) {
                    "Error seeding default extension repo: ${repo.baseUrl}"
                }
            }
        }

        sourcePreferences.hasSeededDefaultExtensionRepos().set(true)
        return@withIOContext true
    }

    private data class DefaultRepo(
        val baseUrl: String,
        val name: String,
        val shortName: String,
        val website: String,
    )
}
