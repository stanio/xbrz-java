/*
 * Copyright (C) 2023 by Stanio <stanio AT yahoo DOT com>
 * Released under BSD Zero Clause License: https://spdx.org/licenses/0BSD
 */
package io.github.stanio.xbrz.awt;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;

/**
 * Allows to stop production to currently registered consumers.  The main
 * use-case is for filtered animation sources.  When a filtered (animation)
 * image needs to be replaced with a differently configured filtered source,
 * one may invoke {@code removeAllConsumers()} on a previous source to stop
 * filtering.
 * <p>
 * This filter also registers weak {@code ImageConsumer} proxies with the
 * original producer, so they may get discarded/unregistered automatically
 * when not reachable from other sources.</p>
 */
class SuspendableFilteredSource implements ImageProducer {

    final ImageProducer imageSource;
    final ImageFilter filterFactory;

    final WeakHashMap<ImageConsumer, WeakConsumerProxy> proxies;

    SuspendableFilteredSource(ImageProducer source, ImageFilter filter) {
        imageSource = source;
        filterFactory = filter;
        proxies = new WeakHashMap<>(1);
    }

    private WeakConsumerProxy addFilterProxy(ImageConsumer consumer) {
        WeakConsumerProxy proxy = new WeakConsumerProxy(consumer);
        proxies.put(consumer, proxy);
        return proxy;
    }

    @Override
    public synchronized void addConsumer(ImageConsumer consumer) {
        WeakConsumerProxy proxy = proxies.get(consumer);
        if (proxy == null) {
            proxy = addFilterProxy(consumer);
            imageSource.addConsumer(proxy.filter);
        }
    }

    @Override
    public synchronized boolean isConsumer(ImageConsumer consumer) {
        return proxies.containsKey(consumer);
    }

    @Override
    public synchronized void removeConsumer(ImageConsumer consumer) {
        WeakConsumerProxy proxy = proxies.remove(consumer);
        if (proxy != null) {
            imageSource.removeConsumer(proxy.filter);
        }
    }

    @Override
    public synchronized void startProduction(ImageConsumer consumer) {
        WeakConsumerProxy proxy = proxies.get(consumer);
        if (proxy == null) {
            proxy = addFilterProxy(consumer);
        }
        imageSource.startProduction(proxy.filter);
    }

    @Override
    public synchronized void requestTopDownLeftRightResend(ImageConsumer consumer) {
        WeakConsumerProxy proxy = proxies.get(consumer);
        if (proxy != null) {
            proxy.filter.resendTopDownLeftRight(imageSource);
        }
    }

    synchronized void stopProduction() {
        suspendProduction();
        proxies.clear();
    }

    synchronized void suspendProduction() {
        proxies.forEach((k, proxy) ->
                imageSource.removeConsumer(proxy.filter));
    }

    synchronized void resumeProduction() {
        proxies.forEach((k, proxy) ->
                imageSource.startProduction(proxy.filter));
    }


    class WeakConsumerProxy
            extends WeakReference<ImageConsumer>
            implements ImageConsumer {

        final ImageFilter filter;

        WeakConsumerProxy(ImageConsumer referent) {
            super(referent);
            filter = filterFactory.getFilterInstance(this);
        }

        void nullSafe(Consumer<ImageConsumer> task) {
            ImageConsumer consumer = get();
            if (consumer == null) {
                imageSource.removeConsumer(filter);
            } else {
                task.accept(consumer);
            }
        }

        @Override
        public void setDimensions(int width, int height) {
            nullSafe(consumer -> consumer.setDimensions(width, height));
        }

        @Override
        public void setProperties(Hashtable<?, ?> props) {
            nullSafe(consumer -> consumer.setProperties(props));
        }

        @Override
        public void setColorModel(ColorModel model) {
            nullSafe(consumer -> consumer.setColorModel(model));
        }

        @Override
        public void setHints(int hintflags) {
            nullSafe(consumer -> consumer.setHints(hintflags));
        }

        @Override
        public void setPixels(int x, int y, int w, int h,
                ColorModel model, byte[] pixels, int off, int scansize) {
            nullSafe(consumer -> consumer
                    .setPixels(x, y, w, h, model, pixels, off, scansize));
        }

        @Override
        public void setPixels(int x, int y, int w, int h,
                ColorModel model, int[] pixels, int off, int scansize) {
            nullSafe(consumer -> consumer
                    .setPixels(x, y, w, h, model, pixels, off, scansize));
        }

        @Override
        public void imageComplete(int status) {
            nullSafe(consumer -> consumer.imageComplete(status));
        }

    } // class WeakConsumerProxy


}
