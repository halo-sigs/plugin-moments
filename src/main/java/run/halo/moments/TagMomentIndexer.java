package run.halo.moments;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.Unstructured;
import run.halo.app.extension.Watcher;
import run.halo.app.extension.controller.RequestSynchronizer;

@Component
public class TagMomentIndexer {
    private final RequestSynchronizer synchronizer;

    private final TagIndexer tagIndexer;

    private final MomentWatcher momentWatcher;

    public TagMomentIndexer(ExtensionClient client) {
        tagIndexer = new TagIndexer();
        this.momentWatcher = new MomentWatcher();
        this.synchronizer = new RequestSynchronizer(true,
            client,
            new Moment(),
            momentWatcher,
            this::checkExtension);
    }

    @NonNull
    public Set<String> getByTagName(String tagName) {
        return tagIndexer.getByIndex(tagName);
    }

    @NonNull
    public Set<String> listAllTags() {
        return tagIndexer.keySet();
    }

    public void destroy() throws Exception {
        if (momentWatcher != null) {
            momentWatcher.dispose();
        }
        if (synchronizer != null) {
            synchronizer.dispose();
        }
    }

    public void start() {
        if (!synchronizer.isStarted()) {
            synchronizer.start();
        }
    }

    class MomentWatcher implements Watcher {
        private Runnable disposeHook;
        private boolean disposed = false;

        @Override
        public void onAdd(Extension extension) {
            if (!checkExtension(extension)) {
                return;
            }
            handleIndicates(extension, tagIndexer::add);
        }

        @Override
        public void onUpdate(Extension oldExt, Extension newExt) {
            if (!checkExtension(newExt)) {
                return;
            }
            handleIndicates(newExt, tagIndexer::update);
        }

        @Override
        public void onDelete(Extension extension) {
            if (!checkExtension(extension)) {
                return;
            }
            handleIndicates(extension, tagIndexer::delete);
        }

        @Override
        public void registerDisposeHook(Runnable dispose) {
            this.disposeHook = dispose;
        }

        @Override
        public void dispose() {
            if (isDisposed()) {
                return;
            }
            this.disposed = true;
            if (this.disposeHook != null) {
                this.disposeHook.run();
            }
        }

        @Override
        public boolean isDisposed() {
            return this.disposed;
        }

        void handleIndicates(Extension extension, Consumer<Moment> consumer) {
            Moment moment = convertTo(extension);
            consumer.accept(moment);
        }
    }

    private Moment convertTo(Extension extension) {
        if (extension instanceof Moment) {
            return (Moment) extension;
        }
        return Unstructured.OBJECT_MAPPER.convertValue(extension, Moment.class);
    }

    private boolean checkExtension(Extension extension) {
        return !momentWatcher.isDisposed()
            && extension.getMetadata().getDeletionTimestamp() == null
            && isMoment(extension);
    }

    private boolean isMoment(Extension extension) {
        return GroupVersionKind.fromExtension(Moment.class).equals(extension.groupVersionKind());
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
                tagMomentCache.put(getObjectKey(moment), indexKey);
            }
        }

        public synchronized Set<String> getByIndex(String indexKey) {
            Set<String> momentNames = tagMomentCache.get(indexKey);
            return Set.copyOf(momentNames);
        }

        public synchronized void delete(Moment moment) {
            Set<String> indexKeys = indexFunc.apply(moment);
            for (String indexKey : indexKeys) {
                String objectKey = getObjectKey(moment);
                tagMomentCache.remove(indexKey, objectKey);
            }
        }

        public synchronized void update(Moment moment) {
            delete(moment);
            add(moment);
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
