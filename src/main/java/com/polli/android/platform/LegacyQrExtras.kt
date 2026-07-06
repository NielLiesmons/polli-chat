package com.polli.android.platform

import org.thoughtcrime.securesms.qr.BackupTransferActivity

/** QR / backup transfer constants not exposed through typealiases. */
object LegacyQrExtras {
    const val TRANSFER_MODE: String = BackupTransferActivity.TRANSFER_MODE
    const val QR_CODE: String = BackupTransferActivity.QR_CODE

    fun receiverScanQrMode(): Int =
        BackupTransferActivity.TransferMode.RECEIVER_SCAN_QR.getInt()
}
