# Platform Adapter Contract

The URL detector identifies the hosting platform. It does not prove that a page contains downloadable media.

Each production adapter implements `PlatformMediaAdapter` and returns one of four explicit outcomes:

- `Ready`: concrete media options with verified MIME type, filename, quality (when known), and a resolved downloadable URL.
- `Unsupported`: no permitted and reliable download path is available.
- `RequiresUserAccess`: the source requires a legitimate user-authorized access flow. The app must not bypass it.
- `Failed`: the adapter encountered an error and can provide a safe explanation.

The registry maps one platform to one adapter. It does not fall back to scraping another platform or fabricate media options.

## Media option requirements

A `MediaDownloadOption` must describe an actual resolved resource. A page URL must never be passed off as a direct media URL. Quality labels are optional and must be omitted when not verified.

## Security and permission rules

- Do not bypass DRM, paywalls, authentication, private content, or access controls.
- Do not expose cookies, session tokens, or private headers in logs or UI.
- Do not present a download action unless the adapter has verified a permitted resource.
- Keep platform adapters isolated and independently tested.

## Initial state

The registry starts empty. This is intentional: URL detection is implemented, but no social platform media extractor is claimed as production-ready yet. Direct HTTP/HTTPS file downloads remain a separate existing capability.
