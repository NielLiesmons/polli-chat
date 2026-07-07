package com.polli.android.connect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.preference.PreferenceManager;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;
import com.polli.android.engine.PolliEngineHost;
import com.polli.android.navigation.AppNav;
import java.io.File;
import com.polli.android.ApplicationContext;
import com.polli.android.onboarding.WelcomeActivity;

public class AccountManager {

  private static final String TAG = "AccountManager";
  private static final String LAST_ACCOUNT_ID = "last_account_id";
  private static AccountManager self;

  private void resetDcContext(Context context) {
    ApplicationContext appContext = (ApplicationContext) context.getApplicationContext();
    appContext.setDcContext(ApplicationContext.getDcAccounts().getSelectedAccount());
    PolliEngineHost.onAccountSwitch(appContext.getDcContext().getAccountId());
    DcHelper.setStockTranslations(context);
    DirectShareUtil.resetAllShortcuts(appContext);
  }

  // public api

  public static AccountManager getInstance() {
    if (self == null) {
      self = new AccountManager();
    }
    return self;
  }

  public void migrateToDcAccounts(Context context, DcAccounts dcAccounts) {
    try {
      int selectAccountId = 0;

      File[] files = context.getFilesDir().listFiles();
      if (files != null) {
        for (File file : files) {
          // old accounts have the pattern "messenger*.db"
          if (!file.isDirectory()
              && file.getName().startsWith("messenger")
              && file.getName().endsWith(".db")) {
            int accountId = dcAccounts.migrateAccount(file.getAbsolutePath());
            if (accountId != 0) {
              String selName =
                  PreferenceManager.getDefaultSharedPreferences(context)
                      .getString("curr_account_db_name", "messenger.db");
              if (file.getName().equals(selName)) {
                // postpone selection as it will otherwise be overwritten by the next
                // migrateAccount() call
                // (if more than one account needs to be migrated)
                selectAccountId = accountId;
              }
            }
          }
        }
      }

      if (selectAccountId != 0) {
        dcAccounts.selectAccount(selectAccountId);
      }
    } catch (Exception e) {
      Log.e(TAG, "Error in migrateToDcAccounts()", e);
    }
  }

  public void switchAccount(Context context, int accountId) {
    DcHelper.getAccounts(context).selectAccount(accountId);
    resetDcContext(context);
  }

  // add accounts

  public int beginAccountCreation(Context context) {
    Rpc rpc = DcHelper.getRpc(context);
    DcAccounts accounts = DcHelper.getAccounts(context);
    DcContext selectedAccount = accounts.getSelectedAccount();
    if (selectedAccount.isOk()) {
      PreferenceManager.getDefaultSharedPreferences(context)
          .edit()
          .putInt(LAST_ACCOUNT_ID, selectedAccount.getAccountId())
          .apply();
    }

    int id = 0;
    try {
      id = rpc.addAccount();
    } catch (RpcException e) {
      Log.e(TAG, "Error calling rpc.addAccount()", e);
    }
    resetDcContext(context);
    return id;
  }

  public boolean canRollbackAccountCreation(Context context) {
    return DcHelper.getAccounts(context).getAll().length > 1;
  }

  public void rollbackAccountCreation(Activity activity) {
    DcAccounts accounts = DcHelper.getAccounts(activity);

    DcContext selectedAccount = accounts.getSelectedAccount();
    if (selectedAccount.isConfigured() == 0) {
      accounts.removeAccount(selectedAccount.getAccountId());
    }

    int lastAccountId =
        PreferenceManager.getDefaultSharedPreferences(activity).getInt(LAST_ACCOUNT_ID, 0);
    if (lastAccountId == 0 || !accounts.getAccount(lastAccountId).isOk()) {
      lastAccountId = accounts.getSelectedAccount().getAccountId();
    }
    switchAccountAndStartActivity(activity, lastAccountId);
  }

  public void switchAccountAndStartActivity(Activity activity, int destAccountId) {
    if (destAccountId == 0) {
      beginAccountCreation(activity);
    } else {
      switchAccount(activity, destAccountId);
    }

    activity.finishAffinity();
    if (destAccountId == 0) {
      activity.startActivity(new Intent(activity, WelcomeActivity.class));
    } else {
      activity.startActivity(AppNav.homeIntent(activity.getApplicationContext()));
    }
  }

  /** Reload home after the active profile changes (Polli Compose path). */
  public void onProfileSwitched(Activity activity) {
    Intent intent = AppNav.homeIntent(activity.getApplicationContext());
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    activity.startActivity(intent);
    activity.finish();
  }

  public void deleteProfile(Activity activity, int profileId) {
    DcAccounts accounts = DcHelper.getAccounts(activity);
    boolean selected = profileId == accounts.getSelectedAccount().getAccountId();
    DcHelper.getNotificationCenter(activity).removeAllNotifications(profileId);
    accounts.removeAccount(profileId);
    if (selected) {
      DcContext selAcc = accounts.getSelectedAccount();
      if (selAcc.isOk()) {
        switchAccount(activity, selAcc.getAccountId());
        onProfileSwitched(activity);
      } else {
        switchAccountAndStartActivity(activity, 0);
      }
    }
  }

  public void addAccountFromSecondDevice(Activity activity, String backupQr) {
    DcAccounts accounts = DcHelper.getAccounts(activity);
    if (accounts.getSelectedAccount().isConfigured() == 1) {
      // the selected account is already configured, create a new one
      beginAccountCreation(activity);
    }

    activity.finishAffinity();
    Intent intent = new Intent(activity, WelcomeActivity.class);
    intent.putExtra(WelcomeActivity.BACKUP_QR_EXTRA, backupQr);
    activity.startActivity(intent);
  }
}
