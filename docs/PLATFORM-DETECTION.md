# Platform URL Detection

## Purpose

M1RAI identifies the platform behind a pasted or shared public URL before attempting any media analysis. This layer is intentionally separate from media extraction and downloading.

## Current detection registry

The initial registry covers YouTube, Facebook, Instagram, TikTok, X, Snapchat, Pinterest, Reddit, Threads, LinkedIn, Twitch, Discord, Vimeo, Dailymotion, Rumble, Tumblr, Douyin, Kuaishou, Bilibili, Weibo, VK, OK.ru, Likee, SnackVideo, and Bluesky.

Common short-link domains are included where they are known and stable. Domain matching is exact or subdomain-based; lookalike suffixes are not accepted.

## Explicit exclusions

WhatsApp, Telegram, and Messenger are excluded from M1RAI's social-media downloader targets because their ordinary user-facing apps already provide native download/save flows for the content in scope. These services should not be presented as supported extraction targets.

## Important distinction

A detected platform is not a confirmed downloader. The result of this layer is only one of:

- Detected platform
- Explicitly excluded platform
- Unrecognized URL
- Invalid URL

A future platform adapter must independently establish whether a public item can be analyzed and downloaded through a permitted, technically reliable route. UI must not offer formats, qualities, or a download action until the adapter confirms them.

## Next integration steps

1. Connect detection to the paste/share flow and show the platform identity.
2. Add a separate adapter contract for metadata analysis and available download variants.
3. Implement and test adapters individually, starting with the platforms where reliable public workflows exist.
4. Keep private content, authentication bypass, DRM circumvention, and access-control bypass out of scope.
