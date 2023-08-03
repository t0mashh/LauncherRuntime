package pro.gravit.launcher.client.gui.scenes.login.methods;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.overlays.AbstractOverlay;
import pro.gravit.launcher.client.gui.scenes.login.LoginScene;
import pro.gravit.launcher.request.auth.details.AuthTotpDetails;
import pro.gravit.launcher.request.auth.password.AuthTOTPPassword;

import java.util.concurrent.CompletableFuture;

public class TotpAuthMethod extends AbstractAuthMethod<AuthTotpDetails> {
    private final TotpOverlay overlay;
    private final JavaFXApplication application;
    private final LoginScene.LoginSceneAccessor accessor;

    public TotpAuthMethod(LoginScene.LoginSceneAccessor accessor) {
        this.accessor = accessor;
        this.application = accessor.getApplication();
        this.overlay = application.gui.registerOverlay(TotpOverlay.class);
        this.overlay.accessor = accessor;
    }

    @Override
    public void prepare() {

    }

    @Override
    public void reset() {

    }

    @Override
    public CompletableFuture<Void> show(AuthTotpDetails details) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            accessor.showOverlay(overlay, (e) -> {
                overlay.requestFocus();
                future.complete(null);
            });
        } catch (Exception e) {
            accessor.errorHandle(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<LoginScene.LoginAndPasswordResult> auth(AuthTotpDetails details) {
        overlay.future = new CompletableFuture<>();
        overlay.hide();
        return overlay.future;
    }

    @Override
    public void onAuthClicked() {
    }

    @Override
    public void onUserCancel() {

    }

    @Override
    public CompletableFuture<Void> hide() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        return future;
    }

    @Override
    public boolean isSavable() {
        return false;
    }

    public static class TotpOverlay extends AbstractOverlay {
        private static final UserAuthCanceledException USER_AUTH_CANCELED_EXCEPTION = new UserAuthCanceledException();
        private TextField[] textFields;
        private CompletableFuture<LoginScene.LoginAndPasswordResult> future;
        private LoginScene.LoginSceneAccessor accessor;

        public TotpOverlay(JavaFXApplication application) {
            super("scenes/login/methods/totp.fxml", application);
        }

        @Override
        public String getName() {
            return "totp";
        }

        public void hide() {
            hide(0, (e) -> future.complete(null));
        }

        @Override
        protected void doInit() {
            LookupHelper.<ButtonBase>lookup(layout, "#header", "#controls", "#exit").setOnAction(e -> {
                hide(0, null);
                future.completeExceptionally(USER_AUTH_CANCELED_EXCEPTION);
            });
            Pane sub = (Pane) LookupHelper.<Button>lookup(layout, "#auth2fa", "#authButton").getGraphic();
            textFields = new TextField[6];
            for (int i = 0; i < 6; ++i) {
                textFields[i] = LookupHelper.lookup(sub, "#" + (i + 1) + "st");
                TextField field = textFields[i];
                int finalI = i;
                field.textProperty().addListener(l -> {
                    int len = field.getText().length();
                    if (len == 1) {
                        if (finalI == 5) {
                            return;
                        }
                        textFields[finalI + 1].requestFocus();
                    }
                });
                field.setOnKeyReleased((key) -> {
                    if (key.getCode() == KeyCode.BACK_SPACE && finalI != 0) {
                        textFields[finalI - 1].setText("");
                        textFields[finalI - 1].requestFocus();
                    }
                });
                if (i == 5) {
                    textFields[i].textProperty().addListener(l -> {
                        AuthTOTPPassword password = new AuthTOTPPassword();
                        password.totp = getCode();
                        future.complete(new LoginScene.LoginAndPasswordResult(null, password));
                    });
                }
            }
        }

        public void requestFocus() {
            textFields[0].requestFocus();
        }

        public String getCode() {
            StringBuilder builder = new StringBuilder();
            for (TextField field : textFields) {
                builder.append(field.getText());
            }
            return builder.toString();
        }

        @Override
        public void reset() {
            for (TextField field : textFields) {
                field.setText("");
            }
        }
    }
}
