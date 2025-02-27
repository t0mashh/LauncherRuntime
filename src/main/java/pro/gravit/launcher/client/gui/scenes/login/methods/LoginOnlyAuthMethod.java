package pro.gravit.launcher.client.gui.scenes.login.methods;

import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.AbstractVisualComponent;
import pro.gravit.launcher.client.gui.impl.ContextHelper;
import pro.gravit.launcher.client.gui.scenes.login.LoginAuthButtonComponent;
import pro.gravit.launcher.client.gui.scenes.login.LoginScene;
import pro.gravit.launcher.request.auth.details.AuthLoginOnlyDetails;
import pro.gravit.utils.helper.LogHelper;

import java.util.concurrent.CompletableFuture;

public class LoginOnlyAuthMethod extends AbstractAuthMethod<AuthLoginOnlyDetails> {
    private final LoginOnlyOverlay overlay;
    private final JavaFXApplication application;
    private final LoginScene.LoginSceneAccessor accessor;

    public LoginOnlyAuthMethod(LoginScene.LoginSceneAccessor accessor) {
        this.accessor = accessor;
        this.application = accessor.getApplication();
        this.overlay = new LoginOnlyOverlay(application);
    }

    @Override
    public void prepare() {
    }

    @Override
    public void reset() {
        overlay.reset();
    }

    @Override
    public CompletableFuture<Void> show(AuthLoginOnlyDetails details) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            ContextHelper.runInFxThreadStatic(() -> {
                accessor.showContent(overlay);
                future.complete(null);
            }).exceptionally((th) -> {
                LogHelper.error(th);
                return null;
            });
        } catch (Exception e) {
            accessor.errorHandle(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<LoginScene.LoginAndPasswordResult> auth(AuthLoginOnlyDetails details) {
        overlay.future = new CompletableFuture<>();
        String login = overlay.login.getText();
        if (login != null && !login.isEmpty()) {
            return CompletableFuture.completedFuture(new LoginScene.LoginAndPasswordResult(login, null));
        }
        return overlay.future;
    }

    @Override
    public void onAuthClicked() {
        overlay.future.complete(overlay.getResult());
    }

    @Override
    public void onUserCancel() {
        overlay.future.completeExceptionally(LoginOnlyOverlay.USER_AUTH_CANCELED_EXCEPTION);
    }

    @Override
    public CompletableFuture<Void> hide() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isOverlay() {
        return false;
    }

    public class LoginOnlyOverlay extends AbstractVisualComponent {
        private static final UserAuthCanceledException USER_AUTH_CANCELED_EXCEPTION = new UserAuthCanceledException();
        private TextField login;
        private CompletableFuture<LoginScene.LoginAndPasswordResult> future;

        public LoginOnlyOverlay(JavaFXApplication application) {
            super("scenes/login/methods/loginonly.fxml", application);
        }

        @Override
        public String getName() {
            return "loginonly";
        }

        public LoginScene.LoginAndPasswordResult getResult() {
            String rawLogin = login.getText();
            return new LoginScene.LoginAndPasswordResult(rawLogin, null);
        }

        @Override
        protected void doInit() {
            login = LookupHelper.lookup(layout, "#login");
            login.textProperty().addListener(l -> accessor.getAuthButton().setState(login.getText().isEmpty()
                                                                                            ? LoginAuthButtonComponent.AuthButtonState.UNACTIVE
                                                                                            : LoginAuthButtonComponent.AuthButtonState.ACTIVE));
            if (application.runtimeSettings.login != null) {
                login.setText(application.runtimeSettings.login);
                accessor.getAuthButton().setState(LoginAuthButtonComponent.AuthButtonState.ACTIVE);
            } else {
                accessor.getAuthButton().setState(LoginAuthButtonComponent.AuthButtonState.UNACTIVE);
            }
        }

        @Override
        protected void doPostInit() {

        }


        @Override
        public void reset() {
            if(login == null) return;
            login.setText("");
        }

        @Override
        public void disable() {

        }

        @Override
        public void enable() {

        }
    }
}
