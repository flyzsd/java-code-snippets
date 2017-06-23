package sandbox.java.swt.wmplayer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.ole.win32.OleEvent;
import org.eclipse.swt.ole.win32.OleListener;
import org.eclipse.swt.widgets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This simple Java class demonstrates how to create and control Windows Media Player using SWT on Windows OS.
 * 
 * @author shudong
 *
 */
public class WMPlayerTest {
    private static final Logger Logger = LoggerFactory.getLogger(WMPlayerTest.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<Shell> future = es.submit(new Callable<Shell>() {
            @Override
            public Shell call() throws Exception {
                Display display = new Display();
                Shell shell = new Shell(display, SWT.NO_TRIM);
                shell.setLayout(new FillLayout());
                return shell;
            }
        });
        final Shell shell = future.get();

        Future<WMPlayer> future2 = es.submit(new Callable<WMPlayer>() {
            @Override
            public WMPlayer call() throws Exception {
                WMPlayer player = new WMPlayer(shell);
                player.uiMode("none");
                player.stretchToFit(true);
                player.enableContextMenu(false);
                player.autoStart(false);
                player.setMode("loop", false);
                return player;
            }
        });

        final WMPlayer player = future2.get();

        Future<?> future3 = es.submit(new Runnable() {
            @Override
            public void run() {
                shell.open();
                Display display = shell.getDisplay();
                while (!shell.isDisposed()) {
                    if (!display.readAndDispatch())
                        display.sleep();
                }
                display.dispose();
            }
        });

        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                player.oleControlSite.addEventListener(WMPlayerConstant.WMP_EVENT_PLAYSTATECHANGE, new OleListener() {
                    @Override
                    public void handleEvent(OleEvent event) {
                        if (event.arguments.length > 0) {
                            switch (event.arguments[0].getInt()) {
                                case WMPlayerConstant.WMP_PLAYSTATE_STOPPED:
                                    Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_STOPPED");
                                    player.dispose();
                                    shell.close();
                                    break;
                            }
                        }
                    }
                });
                player.URL("media/BigBuckBunny_640x360.mp4");
                player.play();
            }
        });

        future3.get();
        es.shutdownNow();
    }
}