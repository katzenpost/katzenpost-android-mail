package com.fsck.k9.controller.katzenpost;


import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.RemoteMessageStore;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.store.katzenpost.KatzenpostStore;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.notification.NotificationController;
import timber.log.Timber;

import static com.fsck.k9.helper.ExceptionHelper.getRootCauseMessage;


public class KatzenpostMessageStore implements RemoteMessageStore {
    private final Context context;
    private final MessagingController controller;
    private final NotificationController notificationController;

    public KatzenpostMessageStore(Context context, MessagingController controller,
            NotificationController notificationController) {
        this.context = context;
        this.controller = controller;
        this.notificationController = notificationController;
    }

    @Override
    public void sync(Account account, String folder, MessagingListener listener, Folder providedRemoteFolder) {
        try {
            performSync(account, folder, listener);
        } catch (Exception e) {
            Timber.e(e, "synchronizeMailbox");
            // If we don't set the last checked, it can try too often during
            // failure conditions
            String rootMessage = getRootCauseMessage(e);

            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxFailed(account, folder, rootMessage);
            }

            Timber.e("Failed synchronizing folder %s:%s @ %tc", account.getDescription(), folder,
                    System.currentTimeMillis());
        }
    }

    private void performSync(Account account, String folder, MessagingListener listener) throws Exception {
        for (MessagingListener l : getListeners(listener)) {
            l.synchronizeMailboxStarted(account, folder);
        }

        LocalStore localStore = account.getLocalStore();
        LocalFolder localFolder = localStore.getFolder(folder);
        localFolder.open(Folder.OPEN_MODE_RW);
        localFolder.updateLastUid();

        localFolder.setLastChecked(System.currentTimeMillis());
        localFolder.setStatus(null);

        KatzenpostStore remoteStore = (KatzenpostStore) account.getRemoteStore();

        int newMessages = 0;
        while (true) {
            Timber.d("Checking for Katzenpost message...");
            String rawMessage = remoteStore.getMessage(3L);
            if (rawMessage == null) {
                Timber.d("Timeout!");
                break;
            }

            Message message = MimeMessage.parseMimeMessage(new ByteArrayInputStream(rawMessage.getBytes()), true);
            localFolder.appendMessages(Collections.singletonList(message));

            String uid = message.getUid();
            LocalMessage localMessage = localFolder.getMessage(uid);
            localMessage.setFlag(Flag.X_DOWNLOADED_FULL, true);

            for (MessagingListener l : getListeners(listener)) {
                l.synchronizeMailboxNewMessage(account, folder, localMessage);
            }

            // Notify with the localMessage so that we don't have to recalculate the content preview.
            if (controller.shouldNotifyForMessage(account, localFolder, message)) {
                notificationController.addNewMailNotification(account, localMessage, newMessages);
            }

            newMessages += 1;
        }

        notificationController.clearAuthenticationErrorNotification(account, true);

        for (MessagingListener l : getListeners(listener)) {
            l.synchronizeMailboxFinished(account, folder, 123, newMessages);
        }
    }

    private Set<MessagingListener> getListeners(MessagingListener listener) {
        return controller.getListeners(listener);
    }

}
