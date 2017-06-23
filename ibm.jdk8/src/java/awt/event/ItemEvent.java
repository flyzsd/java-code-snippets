/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1996, 2013. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.awt.event;

import java.awt.AWTEvent;
import java.awt.ItemSelectable;

/**
 * A semantic event which indicates that an item was selected or deselected.
 * This high-level event is generated by an ItemSelectable object (such as a
 * List) when an item is selected or deselected by the user.
 * The event is passed to every <code>ItemListener</code> object which
 * registered to receive such events using the component's
 * <code>addItemListener</code> method.
 * <P>
 * The object that implements the <code>ItemListener</code> interface gets
 * this <code>ItemEvent</code> when the event occurs. The listener is
 * spared the details of processing individual mouse movements and mouse
 * clicks, and can instead process a "meaningful" (semantic) event like
 * "item selected" or "item deselected".
 * <p>
 * An unspecified behavior will be caused if the {@code id} parameter
 * of any particular {@code ItemEvent} instance is not
 * in the range from {@code ITEM_FIRST} to {@code ITEM_LAST}.
 * <p>
 * The {@code stateChange} of any {@code ItemEvent} instance takes one of the following
 * values:
 *                     <ul>
 *                     <li> {@code ItemEvent.SELECTED}
 *                     <li> {@code ItemEvent.DESELECTED}
 *                     </ul>
 * Assigning the value different from listed above will cause an unspecified behavior.
 *
 * @author Carl Quinn
 *
 * @see java.awt.ItemSelectable
 * @see ItemListener
 * @see <a href="https://docs.oracle.com/javase/tutorial/uiswing/events/itemlistener.html">Tutorial: Writing an Item Listener</a>
 *
 * @since 1.1
 */
public class ItemEvent extends AWTEvent {

    /**
     * The first number in the range of ids used for item events.
     */
    public static final int ITEM_FIRST          = 701;

    /**
     * The last number in the range of ids used for item events.
     */
    public static final int ITEM_LAST           = 701;

    /**
     * This event id indicates that an item's state changed.
     */
    public static final int ITEM_STATE_CHANGED  = ITEM_FIRST; //Event.LIST_SELECT

    /**
     * This state-change value indicates that an item was selected.
     */
    public static final int SELECTED = 1;

    /**
     * This state-change-value indicates that a selected item was deselected.
     */
    public static final int DESELECTED  = 2;

    /**
     * The item whose selection state has changed.
     *
     * @serial
     * @see #getItem()
     */
    Object item;

    /**
     * <code>stateChange</code> indicates whether the <code>item</code>
     * was selected or deselected.
     *
     * @serial
     * @see #getStateChange()
     */
    int stateChange;

    /*
     * JDK 1.1 serialVersionUID
     */
    private static final long serialVersionUID = -608708132447206933L;

    /**
     * Constructs an <code>ItemEvent</code> object.
     * <p> This method throws an
     * <code>IllegalArgumentException</code> if <code>source</code>
     * is <code>null</code>.
     *
     * @param source The <code>ItemSelectable</code> object
     *               that originated the event
     * @param id           The integer that identifies the event type.
     *                     For information on allowable values, see
     *                     the class description for {@link ItemEvent}
     * @param item   An object -- the item affected by the event
     * @param stateChange  An integer that indicates whether the item was
     *               selected or deselected.
     *                     For information on allowable values, see
     *                     the class description for {@link ItemEvent}
     * @throws IllegalArgumentException if <code>source</code> is null
     * @see #getItemSelectable()
     * @see #getID()
     * @see #getStateChange()
     */
    public ItemEvent(ItemSelectable source, int id, Object item, int stateChange) {
        super(source, id);
        this.item = item;
        this.stateChange = stateChange;
    }

    /**
     * Returns the originator of the event.
     *
     * @return the ItemSelectable object that originated the event.
     */
    public ItemSelectable getItemSelectable() {
        return (ItemSelectable)source;
    }

   /**
    * Returns the item affected by the event.
    *
    * @return the item (object) that was affected by the event
    */
    public Object getItem() {
        return item;
    }

   /**
    * Returns the type of state change (selected or deselected).
    *
    * @return an integer that indicates whether the item was selected
    *         or deselected
    *
    * @see #SELECTED
    * @see #DESELECTED
    */
    public int getStateChange() {
        return stateChange;
    }

    /**
     * Returns a parameter string identifying this item event.
     * This method is useful for event-logging and for debugging.
     *
     * @return a string identifying the event and its attributes
     */
    public String paramString() {
        String typeStr;
        switch(id) {
          case ITEM_STATE_CHANGED:
              typeStr = "ITEM_STATE_CHANGED";
              break;
          default:
              typeStr = "unknown type";
        }

        String stateStr;
        switch(stateChange) {
          case SELECTED:
              stateStr = "SELECTED";
              break;
          case DESELECTED:
              stateStr = "DESELECTED";
              break;
          default:
              stateStr = "unknown type";
        }
        return typeStr + ",item="+item + ",stateChange="+stateStr;
    }

}
