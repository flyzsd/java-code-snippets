package sandbox.java.swt.wmplayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.OleEvent;
import org.eclipse.swt.ole.win32.OleFrame;
import org.eclipse.swt.ole.win32.OleListener;
import org.eclipse.swt.ole.win32.Variant;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OLE Windows Media Player
 * 
 * http://msdn.microsoft.com/en-us/library/bb821536.aspx
 * 
 * @author shudong
 */
public class WMPlayer {
    private static final Logger Logger = LoggerFactory.getLogger(WMPlayer.class);

    private final OleFrame oleFrame;
    final OleControlSite oleControlSite;
    private final OleAutomation olePlayer;
    private final OleAutomation oleSetting;
    private final OleAutomation oleControl;

    WMPlayer(Composite parent) {
        this(parent, SWT.NO_TRIM);
    }

    WMPlayer(Composite parent, int style) {
        oleFrame = new OleFrame(parent, style);
        oleControlSite = new OleControlSite(oleFrame, style, "WMPlayer.OCX");
        Logger.info("ActiveX Control program ID = {}", oleControlSite.getProgramID());
        olePlayer = new OleAutomation(oleControlSite);
        Logger.info("Player.versionInfo = {}", versionInfo());
        Logger.info("Player.URL = {}", URL());
        Logger.info("Player.enabled = {}", enabled());
        Logger.info("Player.fullScreen = {}", fullScreen());
        Logger.info("Player.enableContextMenu = {}", enableContextMenu());
        Logger.info("Player.uiMode = {}", uiMode());
        Logger.info("Player.stretchToFit = {}", stretchToFit());

        int[] ids = olePlayer.getIDsOfNames(new String[] { "settings" });
        if (ids != null) {
            oleSetting = olePlayer.getProperty(ids[0]).getAutomation();
            Logger.info("Player.settings.autoStart = {}", autoStart());
            Logger.info("Player.settings.mute = {}", mute());
            Logger.info("Player.settings.volume = {}", volume());
            Logger.info("Player.settings.getMode(autoRewind) = {}", getMode("autoRewind"));
            Logger.info("Player.settings.getMode(loop) = {}", getMode("loop"));
            Logger.info("Player.settings.getMode(showFrame) = {}", getMode("showFrame"));
            Logger.info("Player.settings.getMode(shuffle) = {}", getMode("shuffle"));
        } else {
            throw new UnsupportedOperationException("player.settings property not supported");
        }

        ids = olePlayer.getIDsOfNames(new String[] { "controls" });
        if (ids != null) {
            oleControl = olePlayer.getProperty(ids[0]).getAutomation();
        } else {
            throw new UnsupportedOperationException("player.controls property not supported");
        }

        oleControlSite.addEventListener(WMPlayerConstant.WMP_EVENT_PLAYSTATECHANGE, new OleListener() {
            @Override
            public void handleEvent(OleEvent event) {
                if (event.arguments.length > 0) {
                    switch (event.arguments[0].getInt()) {
                        case WMPlayerConstant.WMP_PLAYSTATE_UNDEFINED:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_UNDEFINED");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_STOPPED:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_STOPPED");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_PAUSED:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_PAUSED");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_PLAYING:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_PLAYING");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_SCANFWD:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_SCANFWD");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_SCANREV:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_SCANREV");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_BUFFERING:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_BUFFERING");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_WAITING:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_WAITING");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_MEDIA_END:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_MEDIA_END");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_TRANSITIONING:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_TRANSITIONING");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_READY:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_READY");
                            break;
                        case WMPlayerConstant.WMP_PLAYSTATE_RECONNECTING:
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", "WMP_PLAYSTATE_RECONNECTING");
                            break;
                        default:
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < event.arguments.length; i++) {
                                if (sb.length() != 0) {
                                    sb.append(", ");
                                }
                                sb.append(i + " : " + event.arguments[i]);
                            }
                            Logger.info("WMP_EVENT_PLAYSTATECHANGE - {}", sb.toString());
                    }
                }
            }
        });
        int result = oleControlSite.doVerb(OLE.OLEIVERB_INPLACEACTIVATE);
        Logger.info("oleControlSite.doVerb(OLE.OLEIVERB_INPLACEACTIVATE) = {}", result);
    }

    public void dispose() {
        oleControl.dispose();
        oleSetting.dispose();
        olePlayer.dispose();
        oleControlSite.dispose();
        oleFrame.dispose();
    }

    public boolean autoStart() {
        int ids[] = oleSetting.getIDsOfNames(new String[] { "autoStart" });
        if (ids != null) {
            Variant vv = oleSetting.getProperty(ids[0]);
            if (vv != null) {
                return vv.getBoolean();
            }
        }
        throw new UnsupportedOperationException("settings.autoStart property not supported");
    }

    public void autoStart(boolean autoStart) {
        int ids[] = oleSetting.getIDsOfNames(new String[] { "autoStart" });
        if (ids != null) {
            oleSetting.setProperty(ids[0], new Variant(autoStart));
        } else {
            throw new UnsupportedOperationException("settings.autoStart property not supported");
        }
    }

    public boolean mute() {
        int ids[] = oleSetting.getIDsOfNames(new String[] { "mute" });
        if (ids != null) {
            Variant vv = oleSetting.getProperty(ids[0]);
            if (vv != null) {
                return vv.getBoolean();
            }
        }
        throw new UnsupportedOperationException("settings.mute property not supported");
    }

    public void mute(boolean mute) {
        int ids[] = oleSetting.getIDsOfNames(new String[] { "mute" });
        if (ids != null) {
            oleSetting.setProperty(ids[0], new Variant(mute));
        } else {
            throw new UnsupportedOperationException("settings.mute property not supported");
        }
    }

    public int volume() {
        int ids[] = oleSetting.getIDsOfNames(new String[] { "volume" });
        if (ids != null) {
            Variant vv = oleSetting.getProperty(ids[0]);
            if (vv != null) {
                return vv.getInt();
            }
        }
        throw new UnsupportedOperationException("settings.volume property not supported");
    }

    /**
     * Set volume
     * 
     * @param volume
     *            from 0 to 100
     */
    public void volume(int volume) {
        int ids[] = oleSetting.getIDsOfNames(new String[] { "volume" });
        if (ids != null) {
            oleSetting.setProperty(ids[0], new Variant(volume));
        } else {
            throw new UnsupportedOperationException("settings.volume property not supported");
        }
    }

    /**
     * autoRewind Mode ———indicating whether the tracks are rewound to the
     * beginning after playing to the end. Default state is true.
     *
     * loop Mode ——– indicating whether the sequence of tracks repeats itself.
     * Default state is false.
     *
     * showFrame Mode ——— indicating whether the nearest video key frame is
     * displayed at the current position when not playing. Default state is
     * false. Has no effect on audio tracks.
     *
     * shuffle Mode ———- indicating whether the tracks are played in random
     * order. Default state is false.
     *
     */
    public void setMode(String mode, boolean flag) {
        int[] ids = oleSetting.getIDsOfNames(new String[] { "setMode" });
        if (ids != null) {
            oleSetting.invoke(ids[0], new Variant[] { new Variant(mode), new Variant(flag) });
        } else {
            throw new UnsupportedOperationException("settings.setMode() function not supported");
        }
    }

    public boolean getMode(String mode) {
        int[] ids = oleSetting.getIDsOfNames(new String[] { "getMode" });
        if (ids != null) {
            Variant vv = oleSetting.invoke(ids[0], new Variant[] { new Variant(mode) });
            if (vv != null) {
                return vv.getBoolean();
            }
        }
        throw new UnsupportedOperationException("settings.getMode() function not supported");
    }

    public void play() {
        int ids[] = oleControl.getIDsOfNames(new String[] { "play" });
        if (ids != null) {
            oleControl.invoke(ids[0]);
        } else {
            throw new UnsupportedOperationException("controls.play() function not supported");
        }
    }

    public void stop() {
        int ids[] = oleControl.getIDsOfNames(new String[] { "stop" });
        if (ids != null) {
            oleControl.invoke(ids[0]);
        } else {
            throw new UnsupportedOperationException("controls.stop() function not supported");
        }
    }

    public void pause() {
        int ids[] = oleControl.getIDsOfNames(new String[] { "pause" });
        if (ids != null) {
            oleControl.invoke(ids[0]);
        } else {
            throw new UnsupportedOperationException("controls.pause() function not supported");
        }
    }

    public String versionInfo() {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "versionInfo" });
        if (ids != null) {
            Variant vv = olePlayer.getProperty(ids[0]);
            if (vv != null) {
                return vv.getString();
            }
        }
        throw new UnsupportedOperationException("player.versionInfo property not supported");
    }

    public String URL() {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "URL" });
        if (ids != null) {
            Variant vv = olePlayer.getProperty(ids[0]);
            if (vv != null) {
                return vv.getString();
            }
        }
        throw new UnsupportedOperationException("player.URL property not supported");
    }

    public void URL(String url) {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "URL" });
        if (ids != null) {
            olePlayer.setProperty(ids[0], new Variant(url));
        } else {
            throw new UnsupportedOperationException("player.URL property not supported");
        }
    }

    public boolean enabled() {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "enabled" });
        if (ids != null) {
            Variant vv = olePlayer.getProperty(ids[0]);
            if (vv != null) {
                return vv.getBoolean();
            }
        }
        throw new UnsupportedOperationException("player.enabled property not supported");
    }

    public void enabled(boolean enabled) {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "enabled" });
        if (ids != null) {
            olePlayer.setProperty(ids[0], new Variant(enabled));
        } else {
            throw new UnsupportedOperationException("player.enabled property not supported");
        }
    }

    public boolean fullScreen() {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "fullScreen" });
        if (ids != null) {
            Variant vv = olePlayer.getProperty(ids[0]);
            if (vv != null) {
                return vv.getBoolean();
            }
        }
        throw new UnsupportedOperationException("player.fullScreen property not supported");
    }

    public void fullScreen(boolean fullScreen) {
        int ids[] = olePlayer.getIDsOfNames(new String[] { "fullScreen" });
        if (ids != null) {
            olePlayer.setProperty(ids[0], new Variant(fullScreen));
        } else {
            throw new UnsupportedOperationException("player.fullScreen property not supported");
        }
    }

    public boolean enableContextMenu() {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "enableContextMenu" });
        if (ids != null) {
            Variant vv = olePlayer.getProperty(ids[0]);
            if (vv != null) {
                return vv.getBoolean();
            }
        }
        throw new UnsupportedOperationException("player.enableContextMenu property not supported");
    }

    public void enableContextMenu(boolean enableContextMenu) {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "enableContextMenu" });
        if (ids != null) {
            olePlayer.setProperty(ids[0], new Variant(enableContextMenu));
        } else {
            throw new UnsupportedOperationException("player.enableContextMenu property not supported");
        }
    }

    public String uiMode() {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "uiMode" });
        if (ids != null) {
            Variant vv = olePlayer.getProperty(ids[0]);
            if (vv != null) {
                return vv.getString();
            }
        }
        throw new UnsupportedOperationException("player.uiMode property not supported");
    }

    /**
     * 
     * @param mode
     *            The possible values are none, invisible, mini, full
     */
    public void uiMode(String mode) {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "uiMode" });
        if (ids != null) {
            olePlayer.setProperty(ids[0], new Variant(mode));
        } else {
            throw new UnsupportedOperationException("player.uiMode property not supported");
        }
    }

    public boolean stretchToFit() {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "stretchToFit" });
        if (ids != null) {
            Variant vv = olePlayer.getProperty(ids[0]);
            if (vv != null) {
                return vv.getBoolean();
            }
        }
        throw new UnsupportedOperationException("player.stretchToFit property not supported");
    }

    public void stretchToFit(boolean stretchToFit) {
        int[] ids = olePlayer.getIDsOfNames(new String[] { "stretchToFit" });
        if (ids != null) {
            olePlayer.setProperty(ids[0], new Variant(stretchToFit));
        } else {
            throw new UnsupportedOperationException("player.stretchToFit property not supported");
        }
    }
}