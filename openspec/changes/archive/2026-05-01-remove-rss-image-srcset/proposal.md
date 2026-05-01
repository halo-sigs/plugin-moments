## Why

The `MomentRssProvider` generates RSS feed items with responsive image markup including `srcset` and thumbnail URLs. Most RSS readers do not support `srcset` or responsive images, and the current implementation produces invalid HTML markup (missing `srcset="..."` attribute wrapper). Simplifying the image output to a plain `<img>` tag improves RSS compatibility and fixes the broken markup.

## What Changes

- Simplify `generatePhotoHtml()` in `MomentRssProvider` to output a plain `<img src="...">` tag.
- Remove `srcset` attribute generation from `generatePhotoHtml()`.
- Remove `genThumbUrl()` helper method (no longer needed for RSS).
- Keep image link processing (`processHtml()`) that converts relative URLs to absolute via `externalLinkProcessor`.
- Video and audio media HTML in RSS remains unchanged.

## Capabilities

### New Capabilities
<!-- No new capabilities introduced -->

### Modified Capabilities
<!-- No spec-level requirement changes; this is an implementation simplification of existing RSS output -->

## Impact

- Affected file: `src/main/java/run/halo/moments/rss/MomentRssProvider.java`
- RSS feed consumers will receive simpler `<img>` tags without `srcset`.
- No API or database schema changes.
- No frontend changes.
