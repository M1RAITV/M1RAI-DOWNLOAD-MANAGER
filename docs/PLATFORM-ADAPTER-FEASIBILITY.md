# Platform Adapter Feasibility — First Integration Gate

Date: 2026-11-11

## Decision

Do not register Reddit or Vimeo as a working downloader yet. Their official media paths require authorization that is not currently configured in M1RAI. A platform URL match is not permission to fetch or redistribute its media.

## Candidate review

### Reddit

Reddit's current Data API guidance says clients must authenticate with a registered OAuth token and use a descriptive User-Agent. Unauthenticated traffic may be blocked, and the published free-access rate limit applies to eligible OAuth clients. Reddit's video metadata model can expose DASH, HLS, and fallback URLs, but that data model does not remove the API authentication and policy requirements.

Implementation gate:
- Register an app and obtain an approved OAuth client configuration.
- Implement the documented OAuth flow and secure token storage; never hardcode a personal token in source.
- Fetch only public posts available to the authorized client.
- Offer only a concrete media URL returned by the API and verify its MIME type/status before exposing a download option.
- Respect deletion, retention, rate-limit, and developer-policy requirements.

### Vimeo

Vimeo documents direct video file links through its API. Access to those file fields requires an authenticated token with `public`, `private`, and `video_files` scopes. The returned download links expire, so the app must request fresh links and use the supplied redirect URL as documented.

Implementation gate:
- Register a Vimeo API app and implement user-consented OAuth.
- Request only the scopes needed for videos the user is authorized to download.
- Verify that the account and video permit file access; never infer permission from public playback.
- Refresh expiring download links on demand and verify the final response before offering a download.

## Current product truth

- Platform URL detection is implemented and covered by unit tests.
- The adapter contract and registry are implemented and covered by unit tests.
- The production adapter registry is intentionally empty.
- Direct HTTP/HTTPS file downloading is separate from social-platform extraction.
- No social platform is currently claimed as a supported media extractor.
- CI success is not physical-device verification.

## Next implementation sequence

1. Add the first official API integration only after its app registration and OAuth configuration are available.
2. Keep credentials out of Git and out of logs; use Android secure storage for user tokens.
3. Add mocked adapter tests for authorization failures, missing media, expired links, MIME validation, and valid media options.
4. Add the adapter to the registry only after tests pass and a permitted live test URL is verified.
5. Keep unsupported platforms clearly identified as detected-but-not-downloadable.

## Official references

- Reddit Data API requirements: https://support.reddithelp.com/hc/en-us/articles/16160319875092-Reddit-Data-API-Wiki
- Reddit video metadata fields: https://developers.reddit.com/docs/api/public-api/type-aliases/RedditVideo
- Vimeo file download links: https://help.vimeo.com/hc/en-us/articles/12427806914577-About-video-file-download-links-from-the-API
- Vimeo authentication and scopes: https://developer.vimeo.com/api/authentication
