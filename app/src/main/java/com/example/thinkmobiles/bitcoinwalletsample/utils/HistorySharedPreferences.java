package com.example.thinkmobiles.bitcoinwalletsample.utils;

import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

/**
 * Created by Lynx on 4/11/2017.
 */

@SharedPref(SharedPref.Scope.UNIQUE)
public interface HistorySharedPreferences {

    @DefaultString("")
    String getHistory();    // comma-separated history

}
