package com.github.stanio.xbrz.awt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.stanio.xbrz.Xbrz;

class ScalerPool {


    private static final class ScalerKey {

        final int scale;
        final boolean hasAlpha;
        private final int hash;

        private ScalerKey(int scale, boolean hasAlpha) {
            this.scale = scale;
            this.hasAlpha = hasAlpha;

            final int prime = 31;
            int hashCode = 1;
            hashCode = prime * hashCode + (hasAlpha ? 1231 : 1237);
            hashCode = prime * hashCode + scale;
            this.hash = hashCode;
        }

        static ScalerKey of(int scale, boolean hasAlpha) {
            return new ScalerKey(scale, hasAlpha);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ScalerKey) {
                ScalerKey other = (ScalerKey) obj;
                return scale == other.scale && hasAlpha == other.hasAlpha;
            }
            return false;
        }

    }


    private static Map<ScalerKey, Xbrz> scalers = new ConcurrentHashMap<>();

    static Xbrz getScaler(int factor, boolean withAlpha) {
        return scalers.computeIfAbsent(ScalerKey.of(factor, withAlpha),
                                       key -> new Xbrz(key.scale, key.hasAlpha));
    }

}
