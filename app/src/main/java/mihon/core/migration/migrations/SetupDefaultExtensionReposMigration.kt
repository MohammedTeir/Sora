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
            baseUrl = "https://raw.githubusercontent.com/mahmoud-teir/extensions/repo",
            name = "Mahmoud Extensions",
            shortName = "Mahmoud",
            website = "https://github.com/mahmoud-teir/extensions",
        ),
    )

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val sourcePreferences = migrationContext.get<SourcePreferences>() ?: return@withIOContext false
        val repository = migrationContext.get<ExtensionRepoRepository>() ?: return@withIOContext false

        val allRepos = repository.getAll()
        val targetRepo = defaultRepos.first()

        // Check if our new default is already present
        if (allRepos.any { it.baseUrl == targetRepo.baseUrl }) {
            return@withIOContext true
        }

        // Delete legacy default repos (Komikku, MiguelMA3, Revived) or any that shouldn't be there
        // As requested: "delete all default Extension repo"
        for (repo in allRepos) {
            repository.deleteRepo(repo.baseUrl)
        }

        // Seed the new default
        try {
            repository.upsertRepo(
                targetRepo.baseUrl,
                targetRepo.name,
                targetRepo.shortName,
                targetRepo.website,
                "NOFINGERPRINT-default-mahmoud",
            )
        } catch (e: SaveExtensionRepoException) {
            logcat(LogPriority.ERROR, e) {
                "Error seeding default extension repo: ${targetRepo.baseUrl}"
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
