package com.curtisnewbie.services;

import android.app.Application;

/**
 * ------------------------------------
 * <p>
 * Author: Yongjie Zhuang
 * <p>
 * ------------------------------------
 * <p>
 * Custom Application class for Dependency Injection. The modules are instantiated here.
 */
public class App extends Application {
    private static AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        App.appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this.getApplicationContext()))
                .build();
    }

    /**
     * Get app component
     *
     * @return
     */
    public static AppComponent getAppComponent() {
        return appComponent;
    }
}