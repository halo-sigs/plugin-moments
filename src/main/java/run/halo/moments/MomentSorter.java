package run.halo.moments;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.util.comparator.Comparators;

/**
 * Moment sorter.
 *
 * @author guqing
 * @since 0.0.1
 */
public enum MomentSorter {
    RELEASE_TIM;

    static final Function<Moment, String> name = moment -> moment.getMetadata().getName();

    public static Comparator<Moment> from(MomentSorter sorter, Boolean ascending) {
        if (Objects.equals(true, ascending)) {
            return from(sorter);
        }
        return from(sorter).reversed();
    }

    static Comparator<Moment> from(MomentSorter sorter) {
        if (sorter == null) {
            return relaseTimeComparator();
        }
        if (RELEASE_TIM.equals(sorter)) {
            Function<Moment, Instant> comparatorFunc =
                moment -> moment.getSpec().getReleaseTime();
            return Comparator.comparing(comparatorFunc, Comparators.nullsLow())
                .thenComparing(name);
        }

        throw new IllegalStateException("Unsupported sort value: " + sorter);
    }

    static MomentSorter convertFrom(String sort) {
        for (MomentSorter sorter : values()) {
            if (sorter.name().equalsIgnoreCase(sort)) {
                return sorter;
            }
        }
        return null;
    }

    static Comparator<Moment> relaseTimeComparator() {
        Function<Moment, Instant> comparatorFunc =
            moment -> moment.getSpec().getReleaseTime();
        return Comparator.comparing(comparatorFunc, Comparators.nullsLow())
            .thenComparing(name);
    }
}
