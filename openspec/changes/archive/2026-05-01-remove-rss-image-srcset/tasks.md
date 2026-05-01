## 1. Simplify RSS photo image generation

- [x] 1.1 Rewrite `generatePhotoHtml()` in `MomentRssProvider` to return a plain `<img src="..." alt="moment photo" />` tag using the original media URL
- [x] 1.2 Remove the `genThumbUrl()` helper method from `MomentRssProvider` (no longer needed)
- [x] 1.3 Verify `processHtml()` still correctly converts relative image URLs to absolute URLs via `externalLinkProcessor`

## 2. Build and verify

- [x] 2.1 Run `./gradlew build` to confirm compilation succeeds
- [x] 2.2 Verify no unused imports remain after removing `genThumbUrl()` and `ThumbnailSize`
