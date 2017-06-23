package sandbox.java.swt.wmplayer;

public class WMPlayerConstant {
    /**
     * Sent when the control changes OpenState void OpenStateChange([in] long
     * NewState);
     */
    final static int WMP_EVENT_OPENSTATECHANGE = 0x00001389;

    /**
     * Sent when the control changes PlayState void PlayStateChange([in] long
     * NewState);
     */
    final static int WMP_EVENT_PLAYSTATECHANGE = 0x000013ed;

    /**
     * Sent when the current audio language has changed void
     * AudioLanguageChange([in] long LangID);
     */
    final static int WMP_EVENT_AUDIOLANGUAGECHANGE = 0x000013ee;

    /**
     * Sent when the status string changes void StatusChange();
     */
    final static int WMP_EVENT_STATUSCHANGE = 0x0000138a;

    /**
     * Sent when a synchronized command or URL is received void
     * ScriptCommand([in] BSTR scType, [in] BSTR Param);
     */
    final static int WMP_EVENT_SCRIPTCOMMAND = 0x000014b5;

    /**
     * Sent when a new stream is started in a channel void NewStream();
     */
    final static int WMP_EVENT_NEWSTREAM = 0x0000151b;

    /**
     * Sent when the control is disconnected from the server void
     * Disconnect([in] long Result);
     */
    final static int WMP_EVENT_DISCONNECT = 0x00001519;

    /**
     * Sent when the control begins or ends buffering void Buffering([in]
     * VARIANT_BOOL Start);
     */
    final static int WMP_EVENT_BUFFERING = 0x0000151a;

    /**
     * Sent when the control has an error condition void Error();
     */
    final static int WMP_EVENT_ERROR = 0x0000157d;

    /**
     * Sent when the control encounters a problem void Warning([in] long
     * WarningType, [in] long Param, [in] BSTR Description);
     */
    final static int WMP_EVENT_WARNING = 0x000015e1;

    /**
     * Sent when the end of file is reached void EndOfStream([in] long Result);
     */
    final static int WMP_EVENT_ENDOFSTREAM = 0x00001451;

    /**
     * void PositionChange([in] double oldPosition, [in] double newPosition);
     */
    final static int WMP_EVENT_POSITIONCHANGED = 0x00001452;

    final static int WMP_EVENT_MARKERHIT = 0x00001453;

    final static int WMP_EVENT_DURATIONUNITCHANGE = 0x00001454;

    final static int WMP_EVENT_CDROMMEDIACHANGE = 0x00001645;

    /**
     * Sent when a playlist changes void PlaylistChange([in] IDispatch*
     * Playlist, [in] WMPPlaylistChangeEventType change);
     */
    final static int WMP_EVENT_PLAYLISTCHANGE = 0x000016a9;

    final static int WMP_EVENT_CURRENTPLAYLISTCHANGE = 0x000016ac;

    final static int WMP_EVENT_CURRENTPLAYLISTITEMAVAILABLE = 0x000016ad;

    final static int WMP_EVENT_MEDIACHANGE = 0x000016aa;

    final static int WMP_EVENT_CURRENTMEDIAITEMAVAILABLE = 0x000016ab;

    final static int WMP_EVENT_CURRENTITEMCHANGE = 0x000016ae;

    final static int WMP_EVENT_MEDIACOLLECTIONCHANGE = 0x000016af;

    /**
     * Occurs when a user clicks the mouse void Click([in] short nButton, [in]
     * short nShiftState, [in] long fX, [in] long fY);
     */
    final static int WMP_EVENT_CLICK = 0x00001969;

    /**
     * Occurs when a user double-clicks the mouse void DoubleClick([in] short
     * nButton, [in] short nShiftState, [in] long fX, [in] long fY);
     */
    final static int WMP_EVENT_DOUBLECLICK = 0x0000196a;

    /**
     * Occurs when a key is pressed void KeyDown([in] short nKeyCode, [in] short
     * nShiftState);
     */
    final static int WMP_EVENT_KEYDOWN = 0x0000196b;

    /**
     * Occurs when a key is pressed and released void KeyPress([in] short
     * nKeyAscii);
     */
    final static int WMP_EVENT_KEYPRESS = 0x0000196c;

    /**
     * Occurs when a key is released void KeyUp([in] short nKeyCode, [in] short
     * nShiftState);
     */
    final static int WMP_EVENT_KEYUP = 0x000016af;

    /**
     * Undefined Windows Media Player is in an undefined state.
     */
    final static int WMP_PLAYSTATE_UNDEFINED = 0;

    /**
     * Stopped Playback of the current media item is stopped.
     */
    final static int WMP_PLAYSTATE_STOPPED = 1;

    /**
     * Paused Playback of the current media item is paused. When a media item is
     * paused, resuming playback begins from the same location.
     */
    final static int WMP_PLAYSTATE_PAUSED = 2;

    /**
     * Playing The current media item is playing.
     */
    final static int WMP_PLAYSTATE_PLAYING = 3;

    /**
     * ScanForward The current media item is fast forwarding.
     */
    final static int WMP_PLAYSTATE_SCANFWD = 4;

    /**
     * ScanReverse The current media item is fast rewinding.
     */
    final static int WMP_PLAYSTATE_SCANREV = 5;

    /**
     * Buffering The current media item is getting additional data from the
     * server.
     */
    final static int WMP_PLAYSTATE_BUFFERING = 6;

    /**
     * Waiting Connection is established, but the server is not sending data.
     * Waiting for session to begin.
     */
    final static int WMP_PLAYSTATE_WAITING = 7;

    /**
     * MediaEnded Media item has completed playback.
     */
    final static int WMP_PLAYSTATE_MEDIA_END = 8;

    /**
     * Transitioning Preparing new media item.
     */
    final static int WMP_PLAYSTATE_TRANSITIONING = 9;

    /**
     * Ready Ready to begin playing.
     */
    final static int WMP_PLAYSTATE_READY = 10;

    /**
     * Reconnecting Reconnecting to stream.
     */
    final static int WMP_PLAYSTATE_RECONNECTING = 11;
}
