package com.gregdev.whirldroid.whirlpool;

import android.content.Context;

public class WhirlpoolApiFactory {

    private static WhirlpoolApiFactory factory;
    private WhirlpoolApi whirlpoolApi;

    public static WhirlpoolApiFactory getFactory() {
        if (factory == null) {
            factory = new WhirlpoolApiFactory();
        }

        return factory;
    }

    public WhirlpoolApi getApi(Context context) {
        if (whirlpoolApi == null) {
            whirlpoolApi = new WhirlpoolApi(context);
        }

        return whirlpoolApi;
    }

}
