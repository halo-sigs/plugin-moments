package run.halo.moments;

import static run.halo.app.extension.index.query.QueryFactory.all;
import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.contains;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.greaterThanOrEqual;
import static run.halo.app.extension.index.query.QueryFactory.lessThanOrEqual;
import static run.halo.app.extension.router.selector.SelectorUtil.labelAndFieldSelectorToListOptions;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.PageRequest;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.router.SortableRequest;
import run.halo.app.extension.router.selector.FieldSelector;

/**
 * A query object for {@link Moment} list.
 *
 * @author LIlGG
 * @author guqing
 * @since 1.0.0
 */
public class MomentQuery extends SortableRequest {
    private final MultiValueMap<String, String> queryParams;

    public MomentQuery(ServerWebExchange exchange) {
        super(exchange);
        this.queryParams = exchange.getRequest().getQueryParams();
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

    /**
     * Build {@link ListOptions} from query params.
     *
     * @return a list options.
     */
    public ListOptions toListOptions() {
        var listOptions =
            labelAndFieldSelectorToListOptions(getLabelSelector(), getFieldSelector());
        var query = all();
        if (StringUtils.isNotBlank(getOwnerName())) {
            query = and(query, equal("spec.owner", getOwnerName()));
        }
        if (StringUtils.isNotBlank(getTag())) {
            query = and(query, equal("spec.tags", getTag()));
        }
        if (getVisible() != null) {
            query = and(query, equal("spec.visible", getVisible().name()));
        }

        if (getStartDate() != null) {
            query = and(query, greaterThanOrEqual("spec.releaseTime", getStartDate().toString()));
        }
        if (getEndDate() != null) {
            query = and(query, lessThanOrEqual("spec.releaseTime", getEndDate().toString()));
        }

        if (listOptions.getFieldSelector() != null
            && listOptions.getFieldSelector().query() != null) {
            query = and(query, listOptions.getFieldSelector().query());
        }
        if (StringUtils.isNotBlank(getKeyword())) {
            query = and(query, contains("spec.owner", getKeyword()));
        }
        listOptions.setFieldSelector(FieldSelector.of(query));
        return listOptions;
    }

    public PageRequest toPageRequest() {
        var sort = getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by("spec.releaseTime").descending();
        }
        return PageRequestImpl.of(getPage(), getSize(), sort);
    }

    private Instant convertInstantOrNull(String timeStr) {
        return StringUtils.isBlank(timeStr) ? null : Instant.parse(timeStr);
    }
}
