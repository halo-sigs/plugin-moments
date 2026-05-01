## ADDED Requirements

### Requirement: RSS photo images use plain img tags
The system SHALL generate photo images in RSS feed items as plain `<img>` tags without `srcset`, thumbnails, or responsive image markup.

#### Scenario: Moment with photo in RSS feed
- **WHEN** a moment containing a photo is included in the RSS feed
- **THEN** the RSS item description contains an `<img>` tag with `src` and `alt` attributes only
- **AND** the `src` attribute points to the original attachment URL (not a thumbnail URL)
- **AND** no `srcset` attribute is present

## REMOVED Requirements

### Requirement: RSS photo images with srcset
**Reason**: Most RSS readers do not support `srcset`, and the previous implementation produced invalid HTML markup.
**Migration**: No action required; RSS consumers will continue to display images via the `src` attribute.
