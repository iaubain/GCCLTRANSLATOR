package com.carpa.library.entities;

import android.content.Context;
import android.content.res.Configuration;
import android.support.multidex.MultiDex;

import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.github.johnkil.print.PrintConfig;
import com.orm.SchemaGenerator;
import com.orm.SugarApp;
import com.orm.SugarContext;
import com.orm.SugarDb;

/**
 * Created by Hp on 6/6/2017.
 */

public class AppDb extends SugarApp {

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(getApplicationContext());

        // create table if not exists
        SchemaGenerator schemaGenerator = new SchemaGenerator(this);
        schemaGenerator.createDatabase(new SugarDb(this).getDB());

        //Downloader library
        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setReadTimeout(30_000)
                .setDatabaseEnabled(true)
                .setConnectTimeout(30_000)
                .build();
        PRDownloader.initialize(getApplicationContext(), config);
        // Method to clean up temporary resumed files which is older than the given day
        PRDownloader.cleanUp(90);

        //Font library
        PrintConfig.initDefault(getAssets(), "fonts/Akzidenz_Grotest_Condenced.ttf");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //SugarContext.terminate();
    }
}
