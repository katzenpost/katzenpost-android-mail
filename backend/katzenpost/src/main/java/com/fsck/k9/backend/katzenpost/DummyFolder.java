package com.fsck.k9.backend.katzenpost;


import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;


public class DummyFolder extends Folder<Message> {
    @Override
    public void open(int mode) throws MessagingException {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public int getMode() {
        return 0;
    }

    @Override
    public boolean create(FolderType type) throws MessagingException {
        return false;
    }

    @Override
    public boolean exists() throws MessagingException {
        return false;
    }

    @Override
    public int getMessageCount() throws MessagingException {
        return 0;
    }

    @Override
    public int getUnreadMessageCount() throws MessagingException {
        return 0;
    }

    @Override
    public int getFlaggedMessageCount() throws MessagingException {
        return 0;
    }

    @Override
    public Message getMessage(String uid) throws MessagingException {
        return null;
    }

    @Override
    public boolean areMoreMessagesAvailable(int indexOfOldestMessage, Date earliestDate)
            throws IOException, MessagingException {
        return false;
    }

    @Override
    public Map<String, String> appendMessages(List<? extends Message> messages) throws MessagingException {
        return null;
    }

    @Override
    public void setFlags(List<? extends Message> messages, Set<Flag> flags, boolean value) throws MessagingException {

    }

    @Override
    public void setFlags(Set<Flag> flags, boolean value) throws MessagingException {

    }

    @Override
    public String getUidFromMessageId(String messageId) throws MessagingException {
        return null;
    }

    @Override
    public void delete(boolean recurse) throws MessagingException {

    }

    @Override
    public String getServerId() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void fetch(List<Message> messages, FetchProfile fp, MessageRetrievalListener<Message> listener)
            throws MessagingException {

    }

    @Override
    public List<Message> getMessages(int start, int end, Date earliestDate, MessageRetrievalListener<Message> listener)
            throws MessagingException {
        return null;
    }
}
