package eu.kanade.presentation.util

import android.content.Context
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.util.system.isOnline
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.i18n.MR
import java.net.UnknownHostException

context(context: Context)
val Throwable.formattedMessage: String
    get() {
        when (this) {
            is HttpException -> return context.stringResource(MR.strings.exception_http, code)
            is UnknownHostException -> {
                return if (!context.isOnline()) {
                    context.stringResource(MR.strings.exception_offline)
                } else {
                    context.stringResource(MR.strings.exception_unknown_host, message ?: "")
                }
            }

            is NoResultsException -> return context.stringResource(MR.strings.no_results_found)
            is SourceNotInstalledException -> return context.stringResource(MR.strings.loader_not_implemented_error)
        }
        // JSON parser received an HTML page — most likely a bad mirror URL or a
        // site returning a login/error page instead of its API response.
        if (this::class.simpleName == "JsonDecodingException" &&
            message?.contains("<!DOCTYPE", ignoreCase = true) == true
        ) {
            return "The source returned an HTML page instead of data. " +
                "If you set a custom mirror URL, it may be incompatible with this source — try resetting it."
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }
