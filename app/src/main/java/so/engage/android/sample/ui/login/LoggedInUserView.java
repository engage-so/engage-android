package so.engage.android.sample.ui.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private String displayName;
    private String displayId;
    //... other data fields that may be accessible to the UI

    LoggedInUserView(String displayId, String displayName) {
        this.displayName = displayName;
        this.displayId = displayId;
    }

    String getDisplayId() {
        return displayId;
    }

    String getDisplayName() {
        return displayName;
    }
}