package com.polli.android.ui;

import android.content.Context;
import androidx.annotation.Nullable;
import com.b44t.messenger.DcContext;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;

/** Binds DC profile/group avatars via the same Glide path as ConversationList. */
public final class AvatarBinder {

  private AvatarBinder() {}

  public static void bind(
      AvatarImageView view,
      Context context,
      @Nullable Integer chatId,
      @Nullable Integer contactId,
      @Nullable DcContext dcContext) {
    Recipient recipient = resolveRecipient(context, chatId, contactId, dcContext);
    if (recipient != null) {
      view.setAvatar(GlideApp.with(context), recipient, false);
    }
  }

  @Nullable
  public static Recipient resolveRecipient(
      Context context,
      @Nullable Integer chatId,
      @Nullable Integer contactId,
      @Nullable DcContext dcContext) {
    try {
      DcContext dc = dcContext != null ? dcContext : DcHelper.getContext(context);
      if (chatId != null && chatId > 0) {
        return new Recipient(context, dc.getChat(chatId));
      }
      if (contactId != null && contactId != 0) {
        return new Recipient(context, dc.getContact(contactId));
      }
    } catch (Exception ignored) {
    }
    return null;
  }
}
