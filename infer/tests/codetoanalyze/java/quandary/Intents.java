/*
 * Copyright (c) 2015 - present Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package codetoanalyze.java.quandary;

import java.io.IOException;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import com.facebook.infer.builtins.InferTaint;

import org.xmlpull.v1.XmlPullParserException;

class IntentSubclass extends Intent {
}

abstract class ContextSubclass extends Context {
}

class MyActivity extends Activity {

  @Override
  // intent is modeled as tainted
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    startService(intent);
  }

  @Override
  // intent is modeled as tainted
  public void onNewIntent(Intent intent) {
    startService(intent);
  }
}

public class Intents {

  private native int rand();

  public void callAllActivitySinksBad(Activity activity, String uri) throws
    SendIntentException, IOException, URISyntaxException, XmlPullParserException {
    Intent intent = (Intent) InferTaint.inferSecretSource();

    activity.bindService(intent, null, 0);
    activity.sendBroadcast(intent);
    activity.sendBroadcastAsUser(intent, null);
    activity.sendOrderedBroadcast(intent, null);
    activity.sendOrderedBroadcastAsUser(intent, null, null, null, null, 0, null, null);
    activity.sendStickyBroadcast(intent);
    activity.sendStickyBroadcastAsUser(intent, null);
    activity.sendStickyOrderedBroadcast(intent, null, null, 0, null, null);
    activity.sendStickyOrderedBroadcastAsUser(intent, null, null, null, 0, null, null);
    activity.startActivities(new Intent[] { intent });
    activity.startActivity(intent);
    activity.startActivityForResult(intent, 0);
    activity.startActivityIfNeeded(intent, 0);
    activity.startActivityFromChild(null, intent, 0);
    activity.startActivityFromFragment(null, intent, 0);
    activity.startIntentSender(null, intent, 0, 0, 0);
    activity.startIntentSenderForResult(null, 0, intent, 0, 0, 0);
    activity.startIntentSenderFromChild(null, null, 0, intent, 0, 0, 0);
    activity.startService(intent);
    activity.stopService(intent); // 20 sinks, 20 expected reports
  }

  public void callAllIntentSinks() throws IOException, URISyntaxException, XmlPullParserException {
    String taintedString = (String) InferTaint.inferSecretSource();
    Intent.parseUri(taintedString, 0);
    Intent.getIntent(taintedString);
    Intent.getIntentOld(taintedString);

    Uri taintedUri = (Uri) InferTaint.inferSecretSource();
    Intent i = new Intent();
    i.setClassName(taintedString, "");
    i.setData(taintedUri);
    i.setDataAndNormalize(taintedUri);
    i.setDataAndType(taintedUri, "");
    i.setDataAndTypeAndNormalize(taintedUri, "");
    i.setPackage(taintedString); // 9 sinks, 9 expected reports
  }

  // make sure the rules apply to subclasses of Intent and Context too
  void subclassCallBad(IntentSubclass intent, ContextSubclass context) {
    String taintedString = (String) InferTaint.inferSecretSource();
    intent.setAction(taintedString);
    context.startActivity(intent);
  }

  void reuseIntentBad(Activity activity) {
    activity.startActivity(activity.getIntent());
  }

  Activity mActivity;

  void extraToDataBad() {
    Intent taintedIntent = (Intent) InferTaint.inferSecretSource();
    String extra = taintedIntent.getStringExtra("foo");

    Intent newIntent1 = new Intent();
    mActivity.startActivity(newIntent1.setData(Uri.parse(extra))); // should report
    Intent newIntent2 = new Intent();
    newIntent2.setData(Uri.parse(extra));
    mActivity.startActivity(newIntent2); // should report
  }

  void extraToExtraOk() {
    Intent taintedIntent = (Intent) InferTaint.inferSecretSource();
    String extra = taintedIntent.getStringExtra("foo");

    Intent newIntent = new Intent();
    newIntent.putExtra("foo", extra);
    mActivity.startActivity(newIntent);
  }

}
