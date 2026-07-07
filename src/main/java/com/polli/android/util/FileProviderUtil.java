package com.polli.android.util;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import java.io.File;
import com.polli.android.BuildConfig;

public class FileProviderUtil {

  private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";

  public static Uri getUriFor(@NonNull Context context, @NonNull File file)
      throws IllegalStateException, NullPointerException {
    return FileProvider.getUriForFile(context, AUTHORITY, file);
  }
}
