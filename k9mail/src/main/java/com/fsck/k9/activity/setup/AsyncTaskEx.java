package com.fsck.k9.activity.setup;


import android.os.AsyncTask;


public abstract class AsyncTaskEx<Params, Progress, Result, Ex extends Exception> extends AsyncTask<Params, Progress, Result> {
    Ex exception;

    @Override
    protected final Result doInBackground(Params[] params) {
        try {
            return doInBackgroundEx(params);
        } catch (Exception e) {
            exception = (Ex) e;
            return null;
        }
    }

    @Override
    protected final void onPostExecute(Result result) {
        if (exception != null) {
            onPostExecuteEx(exception);
        } else {
            onPostExecuteEx(result);
        }
    }

    protected abstract Result doInBackgroundEx(Params[] params) throws Ex;

    protected abstract void onPostExecuteEx(Ex e);

    protected abstract void onPostExecuteEx(Result e);
}
