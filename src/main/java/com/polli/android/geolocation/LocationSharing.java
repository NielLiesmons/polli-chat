package com.polli.android.geolocation;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;

import com.polli.android.R;
import com.polli.android.ShareLocationDialog;
import com.polli.android.connect.DcHelper;
import com.polli.android.permissions.Permissions;

/**
 * Live location-sharing entry point (extracted from the retired Signal
 * AttachmentManager). Invoked from {@code com.polli.android.platform.PlatformAttachments}.
 */
public final class LocationSharing {
  private static final String TAG = "LocationSharing";

  private LocationSharing() {}

  public static void selectLocation(Activity activity, int chatId) {
    Context appContext = activity.getApplicationContext();
    Rpc rpc = DcHelper.getRpc(appContext);
    int accountId = DcHelper.getContext(appContext).getAccountId();

    boolean currentlySharing;
    try {
      currentlySharing = rpc.isSendingLocationsToChat(accountId, chatId);
    } catch (RpcException e) {
      Log.e(TAG, "Failed to check location streaming state", e);
      return;
    }

    if (currentlySharing) {
      if (LocationStreamingService.isRunning()) {
        LocationStreamingService.stopSharing(appContext, accountId, chatId);
        return;
      }
      // Stale — service is dead but chat layer still thinks it's sharing.
      // Clean up and fall through to the fresh start flow.
      try {
        rpc.sendLocationsToChat(accountId, chatId, 0);
      } catch (RpcException e) {
        Log.e(TAG, "Failed to stop stale location streaming", e);
      }
    }

    Permissions.with(activity)
        .ifNecessary()
        .withRationaleDialog(
            activity.getString(R.string.location_rationale), R.drawable.ic_location_on_white_24dp)
        .withPermanentDenialDialog(
            activity.getString(R.string.perm_explain_access_to_location_denied))
        .onAllGranted(
            () -> {
              ShareLocationDialog.show(
                  activity,
                  durationInSeconds ->
                      LocationStreamingService.startSharing(
                          appContext, accountId, chatId, durationInSeconds));
            })
        .request(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION)
        .execute();
  }
}
