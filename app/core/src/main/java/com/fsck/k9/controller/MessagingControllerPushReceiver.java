package com.fsck.k9.controller;

import android.content.Context;

import com.fsck.k9.mail.power.WakeLock;
import timber.log.Timber;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Folder;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.service.SleepService;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MessagingControllerPushReceiver implements PushReceiver {
    final Account account;
    final MessagingController controller;
    final Context context;

    public MessagingControllerPushReceiver(Context context, Account nAccount, MessagingController nController) {
        account = nAccount;
        controller = nController;
        this.context = context;
    }

    public void messagesFlagsChanged(Folder folder, List<Message> messages) {
        syncFolder(folder.getServerId(), folder);
    }
    public void messagesArrived(Folder folder, List<Message> messages) {
        syncFolder(folder.getServerId(), folder);
    }
    public void messagesRemoved(Folder folder, List<Message> messages) {
        syncFolder(folder.getServerId(), folder);
    }

    public void syncFolder(String folderServerId, Folder folder) {
        Timber.v("syncFolder(%s)", folderServerId);

        final CountDownLatch latch = new CountDownLatch(1);
        controller.synchronizeMailbox(account, folderServerId, new SimpleMessagingListener() {
            @Override
            public void synchronizeMailboxFinished(Account account, String folderServerId,
            int totalMessagesInMailbox, int numNewMessages) {
                latch.countDown();
            }

            @Override
            public void synchronizeMailboxFailed(Account account, String folderServerId,
            String message) {
                latch.countDown();
            }
        }, folder);

        Timber.v("syncFolder(%s) about to await latch release", folderServerId);

        try {
            latch.await();
            Timber.v("syncFolder(%s) got latch release", folderServerId);
        } catch (Exception e) {
            Timber.e(e, "Interrupted while awaiting latch release");
        }
    }

    @Override
    public void sleep(WakeLock wakeLock, long millis) {
        SleepService.sleep(context, millis, wakeLock, K9.PUSH_WAKE_LOCK_TIMEOUT);
    }

    public void pushError(String errorMessage, Exception e) {
        String errMess = errorMessage;

        controller.notifyUserIfCertificateProblem(account, e, true);
        if (errMess == null && e != null) {
            errMess = e.getMessage();
        }
        Timber.e(e, errMess);
    }

    @Override
    public void authenticationFailed() {
        controller.handleAuthenticationFailure(account, true);
    }

    public String getPushState(String folderServerId) {
        LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folderServerId);
            localFolder.open(Folder.OPEN_MODE_RW);
            return localFolder.getPushState();
        } catch (Exception e) {
            Timber.e(e, "Unable to get push state from account %s, folder %s", account.getDescription(), folderServerId);
            return null;
        } finally {
            if (localFolder != null) {
                localFolder.close();
            }
        }
    }

    public void setPushActive(String folderServerId, boolean enabled) {
        for (MessagingListener l : controller.getListeners()) {
            l.setPushActive(account, folderServerId, enabled);
        }
    }
}
