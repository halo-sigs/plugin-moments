## Context

The `MomentRssProvider` in `src/main/java/run/halo/moments/rss/MomentRssProvider.java` generates RSS feed items for moments. When a moment contains photos, `generatePhotoHtml()` builds an `<img>` tag with `srcset` containing multiple thumbnail sizes. The current implementation has a bug where `srcset` is concatenated directly into the tag string without the `srcset="..."` attribute wrapper, producing invalid HTML. Additionally, most RSS readers do not support responsive images or `srcset`, making this feature unnecessary complexity.

## Goals / Non-Goals

**Goals:**
- Simplify `generatePhotoHtml()` to output a plain `<img>` tag with only `src` and `alt` attributes.
- Remove the `genThumbUrl()` helper method that is only used for RSS thumbnail generation.
- Keep the `processHtml()` logic that converts relative image URLs to absolute URLs via `externalLinkProcessor`.

**Non-Goals:**
- No changes to the theme-facing frontend or Finder API image handling.
- No changes to video/audio media HTML in RSS.
- No changes to the attachment or thumbnail storage system.

## Decisions

**Decision: Remove `srcset` entirely instead of fixing the attribute syntax.**

- Rationale: Even if the syntax were fixed, most RSS aggregators (Feedly, Inoreader, Reeder, etc.) do not support `srcset`. The complexity of generating multiple thumbnail URLs for an RSS-only feature is not justified.
- Alternative considered: Fix the `srcset` string format by adding `srcset="..."` wrapper. Rejected because the benefit to RSS consumers is marginal.

**Decision: Use the original attachment URL as `src` instead of a thumbnail URL.**

- Rationale: RSS readers typically display images at their natural size or scale them via CSS. Using the original URL avoids an extra API round-trip through the thumbnail service. The existing `processHtml()` already converts relative URLs to absolute, so the image will still be accessible.
- Alternative considered: Keep using `ThumbnailSize.M` as `src`. Rejected because the original URL is sufficient for RSS and avoids dependency on the thumbnail API.

## Risks / Trade-offs

- [Risk] RSS feed images may load slower for large photos. → Mitigation: This is acceptable for RSS; readers typically cache images, and the original URL is the source of truth.
- [Risk] Some very old RSS readers might not handle large images well. → Mitigation: This is a general limitation of RSS, not specific to this change. The previous thumbnail approach did not fundamentally solve this.
