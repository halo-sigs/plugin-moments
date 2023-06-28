package run.halo.moments;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import run.halo.app.extension.router.IListRequest;

/**
 * A query object for {@link Moment} list.
 *
 * @author LIlGG
 * @since 1.0.0
 */
public class MomentQuery extends IListRequest.QueryListRequest {

    public MomentQuery(MultiValueMap<String, String> queryParams) {
        super(queryParams);
    }

    @Nullable
    @Schema(description = "Moments filtered by keyword.")
    public String getKeyword() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("keyword"), null);
    }

    @Schema(description = "Owner name.")
    public String getOwnerName() {
        String ownerName = queryParams.getFirst("ownerName");
        return StringUtils.isBlank(ownerName) ? null : ownerName;
    }

    @Schema(description = "Moment tag.")
    public String getTag() {
        return StringUtils.defaultIfBlank(queryParams.getFirst("tag"), null);
    }

    @Nullable
    public Moment.MomentVisible getVisible() {
        String visible = queryParams.getFirst("visible");
        return Moment.MomentVisible.from(visible);
    }

    @Schema(description = "Moment collation.")
    public MomentSorter getSort() {
        String sort = queryParams.getFirst("sort");
        return MomentSorter.convertFrom(sort);
    }

    @Schema
    public Instant getStartDate() {
        String startDate = queryParams.getFirst("startDate");
        return convertInstantOrNull(startDate);
    }

    @Schema
    public Instant getEndDate() {
        String endDate = queryParams.getFirst("endDate");
        return convertInstantOrNull(endDate);
    }

    @Schema(description = "ascending order If it is true; otherwise, it is in descending order.")
    public Boolean getSortOrder() {
        String sortOrder = queryParams.getFirst("sortOrder");
        return convertBooleanOrNull(sortOrder);
    }

    private Boolean convertBooleanOrNull(String value) {
        return StringUtils.isBlank(value) ? null : Boolean.parseBoolean(value);
    }

    private Instant convertInstantOrNull(String timeStr) {
        return StringUtils.isBlank(timeStr) ? null : Instant.parse(timeStr);
    }
    
    
}
