package run.halo.moments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import run.halo.app.extension.Metadata;

class TagMomentIndexerTest {

    @Nested
    class TagIndexerTest {

        private TagMomentIndexer.TagIndexer tagIndexer;

        @BeforeEach
        void setUp() {
            tagIndexer = new TagMomentIndexer.TagIndexer();
        }

        @Test
        void add() {
            tagIndexer.add(createMoment("m-1", Set.of("t1", "t2", "t3")));
            tagIndexer.add(createMoment("m-2", Set.of("t2", "t3", "t4")));

            assertThat(tagIndexer.tagMomentCache.get("t1")).containsOnly("m-1");
            assertThat(tagIndexer.tagMomentCache.get("t2")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t3")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t4")).containsOnly("m-2");
        }

        @Test
        void delete() {
            Moment m1 = createMoment("m-1", Set.of("t1", "t2", "t3"));
            tagIndexer.add(m1);

            Moment m2 = createMoment("m-2", Set.of("t2", "t3", "t4"));
            tagIndexer.add(m2);

            assertThat(tagIndexer.tagMomentCache.get("t1")).containsOnly("m-1");
            assertThat(tagIndexer.tagMomentCache.get("t2")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t3")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t4")).containsOnly("m-2");

            tagIndexer.delete(m1);
            assertThat(tagIndexer.tagMomentCache.get("t1")).isEmpty();
            assertThat(tagIndexer.tagMomentCache.get("t2")).containsOnly("m-2");
            assertThat(tagIndexer.tagMomentCache.get("t3")).containsOnly("m-2");
            assertThat(tagIndexer.tagMomentCache.get("t4")).containsOnly("m-2");
        }

        @Test
        void update() {
            Moment m1 = createMoment("m-1", Set.of("t1", "t2", "t3"));
            tagIndexer.add(m1);

            Moment m2 = createMoment("m-2", Set.of("t2", "t3", "t4"));
            tagIndexer.add(m2);

            assertThat(tagIndexer.tagMomentCache.get("t1")).containsOnly("m-1");
            assertThat(tagIndexer.tagMomentCache.get("t2")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t3")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t4")).containsOnly("m-2");

            m1.getSpec().setTags(Set.of("t1", "t2", "t4"));
            tagIndexer.update(m1);

            // {t1=[m-1], t2=[m-1, m-2], t3=[m-2], t4=[m-1, m-2]}
            assertThat(tagIndexer.tagMomentCache.get("t1")).containsOnly("m-1");
            assertThat(tagIndexer.tagMomentCache.get("t2")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t3")).containsOnly("m-2");
            assertThat(tagIndexer.tagMomentCache.get("t4")).containsOnly("m-1", "m-2");

            m2.getSpec().setTags(Set.of("t2", "t4", "t5"));
            tagIndexer.update(m2);

            // {t1=[m-1], t2=[m-1, m-2], t3=[], t4=[m-1, m-2], t5=[m-2]}
            assertThat(tagIndexer.tagMomentCache.get("t1")).containsOnly("m-1");
            assertThat(tagIndexer.tagMomentCache.get("t2")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t3")).isEmpty();
            assertThat(tagIndexer.tagMomentCache.get("t4")).containsOnly("m-1", "m-2");
            assertThat(tagIndexer.tagMomentCache.get("t5")).containsOnly("m-2");

        }

        Moment createMoment(String name, Set<String> tags) {
            Moment moment = new Moment();
            moment.setMetadata(new Metadata());
            moment.getMetadata().setName(name);

            moment.setSpec(new Moment.MomentSpec());
            moment.getSpec().setTags(tags);
            return moment;
        }
    }
}