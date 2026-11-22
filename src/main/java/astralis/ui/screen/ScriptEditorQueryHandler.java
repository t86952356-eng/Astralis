package astralis.ui.screen;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.util.Objects;

public class ScriptEditorQueryHandler extends CefMessageRouterHandlerAdapter {
    final ScriptEditorScreen this$0;

    ScriptEditorQueryHandler(ScriptEditorScreen screen) {
        Objects.requireNonNull(screen);
        this.this$0 = screen;
        super();
    }

    @Override
    public boolean onQuery(
            CefBrowser browser,
            CefFrame frame,
            long queryId,
            String request,
            boolean persistent,
            CefQueryCallback callback) {
        if (this.this$0.browser == browser
                && frame != null
                && frame.isMain()
                && !persistent
                && !this.this$0.isNotEasyModeUrl(frame.getURL())) {
            this.this$0.handleBridgeRequest(request, callback);
            return true;
        } else {
            return false;
        }
    }
}
