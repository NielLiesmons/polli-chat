package com.polli.android.mms;

import android.content.Context;
import androidx.annotation.NonNull;
import com.b44t.messenger.DcMsg;
import com.polli.android.attachments.DcAttachment;

public class StickerSlide extends Slide {

  public StickerSlide(@NonNull Context context, @NonNull DcMsg dcMsg) {
    super(context, new DcAttachment(dcMsg));
  }

  @Override
  public boolean hasSticker() {
    return true;
  }
}
