package org.cameralib.engine;


public abstract class ActionCallback<T> {

    private final long mId;

    public ActionCallback(long id) {
        mId = id;
    }

    public ActionCallback() {
        this(System.currentTimeMillis());
    }

    public final long getId() {
        return mId;
    }

    public abstract void onAction(ActionCarrier<T> carrier);
}
