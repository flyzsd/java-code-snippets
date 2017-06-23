package sandbox.java.splashscreen;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.SplashScreen;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * java -splash:images/splash.gif <class name> or specify 'SplashScreen-Image:
 * <image name>' inside JAR manifest file
 * 
 * @author shudong
 *
 */
public class Main {

    public static void main(String[] args) {
        // system start up
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash == null) {
            System.out.println("No SplashScreen configured. Try 'java -splash:images/splash.gif <class name>'");
            return;
        }

        Graphics2D g2d = splash.createGraphics();
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        for (int i = 0; i < 10; i++) {
            long delay = i;
            ses.schedule(new Runnable() {
                @Override
                public void run() {
                    if (delay == 9) {
                        g2d.dispose();
                        splash.close();
                    } else {
                        // perform any update on splash screen overlay image
                        g2d.setComposite(AlphaComposite.Clear);
                        g2d.fillRect(120, 140, 200, 40);
                        g2d.setPaintMode();
                        g2d.setColor(Color.BLACK);
                        g2d.drawString("Loading ...", 120, 150);
                        // updates the splash window with current contents of
                        // the
                        // overlay image
                        splash.update();
                    }
                }
            }, i, TimeUnit.SECONDS);
        }
        ses.shutdown();

        Display display = Display.getDefault();
        Shell shell = new Shell(display);
        // show the application screen
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
    }
}
