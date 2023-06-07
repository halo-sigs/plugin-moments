package run.halo.moments;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;

@Component
@RequiredArgsConstructor
public class TagMomentIndexer implements Reconciler<Reconciler.Request> {
    private static final String FINALIZER_NAME = "tag-moment-protection";

    private final ExtensionClient client;

    private final TagIndexer tagIndexer = new TagIndexer();

    @NonNull
    public Set<String> getByTagName(String tagName) {
        return tagIndexer.getByIndex(tagName);
    }

    @NonNull
    public Set<String> listAllTags() {
        return tagIndexer.keySet();
    }

    @Override
    public Result reconcile(Request request) {
        client.fetch(Moment.class, request.name()).ifPresent(moment -> {
            if (moment.getMetadata().getDeletionTimestamp() != null) {
                tagIndexer.delete(moment);
                removeFinalizer(request.name());
                return;
            }
            addFinalizer(moment);
            tagIndexer.update(moment);
        });
        return Result.doNotRetry();
    }

    void addFinalizer(Moment oldMoment) {
        Set<String> oldFinalizers = oldMoment.getMetadata().getFinalizers();
        if (oldFinalizers != null && oldFinalizers.contains(FINALIZER_NAME)) {
            return;
        }
        client.fetch(Moment.class, oldMoment.getMetadata().getName()).ifPresent(moment -> {
            Set<String> finalizers = moment.getMetadata().getFinalizers();
            if (finalizers == null) {
                finalizers = new HashSet<>();
            }
            finalizers.add(FINALIZER_NAME);
            moment.getMetadata().setFinalizers(finalizers);
            client.update(moment);
        });
    }

    void removeFinalizer(String name) {
        client.fetch(Moment.class, name).ifPresent(moment -> {
            Set<String> finalizers = moment.getMetadata().getFinalizers();
            if (finalizers == null || !finalizers.contains(FINALIZER_NAME)) {
                return;
            }
            finalizers.remove(FINALIZER_NAME);
            client.update(moment);
        });
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new Moment())
            .build();
    }

    static class TagIndexer {
        final IndexFunc<Moment> indexFunc = moment -> {
            Set<String> tags = moment.getSpec().getTags();
            return tags != null ? Set.copyOf(tags) : Set.of();
        };
        final SetMultimap<String, String> tagMomentCache = HashMultimap.create();

        public synchronized void add(Moment moment) {
            Set<String> indexKeys = indexFunc.apply(moment);
            for (String indexKey : indexKeys) {
                tagMomentCache.put(indexKey, getObjectKey(moment));
            }
        }

        public synchronized Set<String> getByIndex(String indexKey) {
            Set<String> momentNames = tagMomentCache.get(indexKey);
            return Set.copyOf(momentNames);
        }

        public synchronized void delete(Moment moment) {
            Set<String> indexKeys = indexFunc.apply(moment);
            for (String indexKey : indexKeys) {
                tagMomentCache.remove(indexKey, getObjectKey(moment));
            }
        }

        public synchronized void update(Moment moment) {
            Set<String> indexKeys = indexFunc.apply(moment);
            String objectKey = getObjectKey(moment);
            // find old index
            Set<String> oldIndexKeys = new HashSet<>();
            for (String indexKey : tagMomentCache.keySet()) {
                if (tagMomentCache.get(indexKey).contains(objectKey)) {
                    oldIndexKeys.add(indexKey);
                }
            }
            // remove old index
            for (String indexKey : oldIndexKeys) {
                tagMomentCache.remove(indexKey, objectKey);
            }
            // add new index
            for (String indexKey : indexKeys) {
                tagMomentCache.put(indexKey, objectKey);
            }
        }

        public synchronized Set<String> keySet() {
            return Set.copyOf(tagMomentCache.keySet());
        }

        @FunctionalInterface
        interface IndexFunc<T> {
            Set<String> apply(T obj);
        }

        private String getObjectKey(Moment obj) {
            Assert.notNull(obj, "Moment must not be null");
            Assert.notNull(obj.getMetadata(), "Object metadata must not be null");
            Assert.notNull(obj.getMetadata().getName(), "Object name must not be null");
            return obj.getMetadata().getName();
        }
    }
}
