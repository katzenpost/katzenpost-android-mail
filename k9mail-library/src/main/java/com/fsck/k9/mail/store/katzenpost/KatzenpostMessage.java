package com.fsck.k9.mail.store.katzenpost;


import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.internet.MimeMessage;

class KatzenpostMessage extends MimeMessage {
    KatzenpostMessage(String uid, Folder folder) {
        this.mUid = uid;
        this.mFolder = folder;
    }
}
