package com.polli.android.connect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcChatlist;
import com.b44t.messenger.DcContext;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import com.polli.android.share.PolliShareActivity;
import com.polli.android.recipients.Recipient;
import com.polli.android.util.BitmapUtil;
import com.polli.android.util.DrawableUtil;
import com.polli.android.util.Util;

/**
 * The Signal code has a similar class called ConversationUtil.
 *
 * <p>This class uses the Sharing Shortcuts API to publish dynamic launcher shortcuts (the ones that
 * appear when you long-press on an app) and direct-sharing-shortcuts.
 *
 * <p>It replaces the class DirectShareService, because DirectShareService used the
 * ChooserTargetService API, which was replaced by the Sharing Shortcuts API.
 */
public class DirectShareUtil {

  private static final String TAG = "DirectShareUtil";
  private static final String SHORTCUT_CATEGORY = "android.shortcut.conversation";

  public static void clearShortcut(@NonNull Context context, int chatId) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Util.runOnAnyBackgroundThread(
          () -> {
            try {
              ShortcutManagerCompat.removeDynamicShortcuts(
                  context, Collections.singletonList(Integer.toString(chatId)));
            } catch (Exception e) {
              Log.e(TAG, "Clearing shortcut failed", e);
            }
          });
    }
  }

  public static void resetAllShortcuts(@NonNull Context context) {
    Util.runOnBackground(
        () -> {
          try {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context);
            triggerRefreshDirectShare(context);
          } catch (Exception e) {
            Log.e(TAG, "Resetting shortcuts failed", e);
          }
        });
  }

  public static void triggerRefreshDirectShare(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      Util.runOnBackgroundDelayed(
          () -> {
            try {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                  && context.getSystemService(ShortcutManager.class).isRateLimitingActive()) {
                return;
              }

              int maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context);
              List<ShortcutInfoCompat> currentShortcuts =
                  ShortcutManagerCompat.getDynamicShortcuts(context);
              List<ShortcutInfoCompat> newShortcuts = getChooserTargets(context);

              if (maxShortcuts > 0
                  && currentShortcuts.size() + newShortcuts.size() > maxShortcuts) {
                ShortcutManagerCompat.removeAllDynamicShortcuts(context);
              }

              boolean success = ShortcutManagerCompat.addDynamicShortcuts(context, newShortcuts);
              Log.i(TAG, "Updated dynamic shortcuts, success: " + success);
            } catch (Exception e) {
              Log.e(TAG, "Updating dynamic shortcuts failed: " + e);
            }

            // Wait  1500ms, this is called by onResume(), and we want to make sure that refreshing
            // shortcuts does not delay loading of the chatlist
          },
          1500);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private static List<ShortcutInfoCompat> getChooserTargets(Context context) {
    List<ShortcutInfoCompat> results = new LinkedList<>();
    DcContext dcContext = DcHelper.getContext(context);

    DcChatlist chatlist =
        dcContext.getChatlist(
            DcContext.DC_GCL_FOR_FORWARDING | DcContext.DC_GCL_NO_SPECIALS, null, 0);
    int max = 5;
    if (chatlist.getCnt() < max) {
      max = chatlist.getCnt();
    }
    for (int i = 0; i < max; i++) {
      DcChat chat = chatlist.getChat(i);
      if (!chat.canSend()) {
        continue;
      }

      Intent intent = new Intent(context, PolliShareActivity.class);
      intent.setAction(Intent.ACTION_SEND);
      intent.putExtra(PolliShareActivity.EXTRA_ACC_ID, dcContext.getAccountId());
      intent.putExtra(PolliShareActivity.EXTRA_CHAT_ID, chat.getId());

      Recipient recipient = new Recipient(context, chat);
      Bitmap avatar = getIconForShortcut(context, recipient);
      results.add(
          new ShortcutInfoCompat.Builder(
                  context, "chat-" + dcContext.getAccountId() + "-" + chat.getId())
              .setShortLabel(chat.getName())
              .setLongLived(true)
              .setRank(i + 1)
              .setIcon(IconCompat.createWithAdaptiveBitmap(avatar))
              .setCategories(Collections.singleton(SHORTCUT_CATEGORY))
              .setIntent(intent)
              .setActivity(new ComponentName(context, "com.polli.android.RoutingActivity"))
              .build());
    }

    return results;
  }

  public static Bitmap getIconForShortcut(@NonNull Context context, @NonNull Recipient recipient) {
    try {
      int size =
          context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
      Bitmap photo = recipient.getContactPhotoBitmap(context, size);
      if (photo != null) {
        return DrawableUtil.wrapBitmapForShortcutInfo(photo);
      }
    } catch (Exception e) {
      // fall through to the generated avatar
    }
    return getFallbackDrawable(context, recipient);
  }

  private static Bitmap getFallbackDrawable(Context context, @NonNull Recipient recipient) {
    return BitmapUtil.createFromDrawable(
        recipient.getFallbackAvatarDrawable(context, false),
        context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
        context
            .getResources()
            .getDimensionPixelSize(android.R.dimen.notification_large_icon_height));
  }
}
