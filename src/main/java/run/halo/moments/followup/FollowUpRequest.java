package run.halo.moments.followup;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record FollowUpRequest(
    @Schema(requiredMode = REQUIRED, description = "Plain-text reflection",
        minLength = 1, maxLength = 1000)
    String content
) {
}
