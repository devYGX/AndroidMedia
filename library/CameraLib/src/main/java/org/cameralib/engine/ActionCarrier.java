package org.cameralib.engine;


public class ActionCarrier<T> {
    private ActionCallback mTarget;
    private boolean success;
    private long actionId;
    private T extra;
    private Throwable t;


    ActionCarrier(boolean success, long actionId, T extra, Throwable t, ActionCallback target) {
        this.success = success;
        this.actionId = actionId;
        this.extra = extra;
        this.t = t;
        mTarget = target;
    }

    void postToTarget(){
        if (mTarget != null) {
            mTarget.onAction(this);
            mTarget = null;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public long getActionId() {
        return actionId;
    }

    public T getExtra() {
        return extra;
    }

    public Throwable getT() {
        return t;
    }
}
