package com.polli.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import com.polli.android.onboarding.WelcomeActivity;
import org.thoughtcrime.securesms.R;
import com.polli.android.connect.DcHelper;
import com.polli.android.service.GenericForegroundService;
import com.polli.android.util.DynamicTheme;
import com.polli.android.util.Prefs;
import com.polli.android.util.ViewUtil;

/**
 * Base for Polli View-based (AppCompat) activities. Combines dynamic theming and edge-to-edge
 * chrome with the account-configured gate (redirect to onboarding when the app is not yet set up).
 * Replaces the former Signal BaseActionBarActivity / PassphraseRequiredActionBarActivity pair.
 */
public abstract class PolliBaseActivity extends AppCompatActivity {

  protected DynamicTheme dynamicTheme = new DynamicTheme();

  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
  }

  @Override
  protected final void onCreate(Bundle savedInstanceState) {
    onPreCreate();

    if (allowInLockedMode()) {
      chromeCreate(savedInstanceState);
      onCreate(savedInstanceState, true);
      return;
    }

    if (GenericForegroundService.isForegroundTaskStarted()) {
      chromeCreate(savedInstanceState);
      finish();
      return;
    }

    if (!DcHelper.isConfigured(getApplicationContext())) {
      startActivity(new Intent(this, WelcomeActivity.class));
      chromeCreate(savedInstanceState);
      finish();
    } else {
      chromeCreate(savedInstanceState);
    }

    if (!isFinishing()) {
      onCreate(savedInstanceState, true);
    }
  }

  private void chromeCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (ViewUtil.isEdgeToEdgeSupported()) {
      EdgeToEdge.enable(this);
      WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
          .setAppearanceLightStatusBars(false);
    }
  }

  /** Screen bring-up hook; {@code ready} is always true (kept for source compatibility). */
  protected void onCreate(Bundle savedInstanceState, boolean ready) {}

  /**
   * "Locked Mode" is when the account is not configured (Welcome screen) or when sharing a backup.
   * The app is locked to that activity; tapping the app icon or notifications must not replace the
   * stack. Override to allow pushing this activity in those situations (e.g. logs, offline help).
   */
  protected boolean allowInLockedMode() {
    return false;
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    ViewUtil.adjustToolbarForE2E(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (Prefs.isScreenSecurityEnabled(this)) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    dynamicTheme.onResume(this);
  }

  public void makeSearchMenuVisible(final Menu menu, final MenuItem searchItem) {
    for (int i = 0; i < menu.size(); ++i) {
      MenuItem item = menu.getItem(i);
      int id = item.getItemId();
      if (id == R.id.menu_search_up || id == R.id.menu_search_down) {
        item.setVisible(true);
      } else if (item != searchItem) {
        item.setVisible(false);
      }
    }
  }

  protected <T extends Fragment> T initFragment(@IdRes int target, @NonNull T fragment) {
    return initFragment(target, fragment, null);
  }

  protected <T extends Fragment> T initFragment(
      @IdRes int target, @NonNull T fragment, @Nullable Bundle extras) {
    Bundle args = new Bundle();
    if (extras != null) {
      args.putAll(extras);
    }
    fragment.setArguments(args);
    getSupportFragmentManager()
        .beginTransaction()
        .replace(target, fragment)
        .commitAllowingStateLoss();
    return fragment;
  }
}
