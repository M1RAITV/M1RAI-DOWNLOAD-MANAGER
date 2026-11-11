package com.m1rai.downloadmanager

/**
 * A media option is exposed only after an adapter has verified a concrete,
 * permitted downloadable resource. A detected page URL alone is not enough.
 */
internal data class MediaDownloadOption(
    val id: String,
    val title: String,
    val mediaType: MediaType,
    val mimeType: String,
    val qualityLabel: String?,
    val downloadUrl: String,
    val fileName: String
)

internal enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE,
    FILE
}

internal sealed interface AdapterAnalysis {
    data class Ready(
        val platform: MediaPlatform,
        val options: List<MediaDownloadOption>
    ) : AdapterAnalysis

    data class Unsupported(
        val platform: MediaPlatform,
        val reason: String
    ) : AdapterAnalysis

    data class RequiresUserAccess(
        val platform: MediaPlatform,
        val reason: String
    ) : AdapterAnalysis

    data class Failed(
        val platform: MediaPlatform,
        val reason: String
    ) : AdapterAnalysis
}

/**
 * Platform adapters must be independent. Implementations must not bypass
 * private access, DRM, paywalls, authentication, or platform protections.
 */
internal interface PlatformMediaAdapter {
    val platform: MediaPlatform
    suspend fun analyze(publicUrl: String): AdapterAnalysis
}

/**
 * Only explicitly registered adapters can claim support. The initial registry
 * intentionally contains no production media extractors; detection remains
 * available while unimplemented platforms are honestly reported as unsupported.
 */
internal class PlatformAdapterRegistry(
    adapters: Collection<PlatformMediaAdapter> = emptyList()
) {
    private val adaptersByPlatform = adapters.associateBy { it.platform }

    suspend fun analyze(
        platform: MediaPlatform,
        publicUrl: String
    ): AdapterAnalysis {
        val adapter = adaptersByPlatform[platform]
            ?: return AdapterAnalysis.Unsupported(
                platform,
                "A tested downloader is not available for this platform yet."
            )
        return adapter.analyze(publicUrl)
    }

    fun registeredPlatforms(): Set<MediaPlatform> = adaptersByPlatform.keys
}
