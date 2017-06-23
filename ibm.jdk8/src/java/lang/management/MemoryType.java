/*
 * Licensed Materials - Property of IBM,
 *     Copyright IBM Corp. 2005, 2016  All Rights Reserved.
 */

package java.lang.management;

/**
 * @since 1.5
 */
public enum MemoryType {

    /**
     * Memory on the heap. The heap is the runtime area in the virtual machine,
     * created upon the start-up of the virtual machine, from which memory for
     * instances of types and arrays is allocated. The heap is shared among all
     * threads in the virtual machine.
     */
    HEAP,
    /**
     * Memory that is not on the heap. This encompasses all other storage used
     * by the virtual machine at runtime.
     */
    NON_HEAP;

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String result = null;
        switch (this) {
        case HEAP:
            result = "Heap memory"; //$NON-NLS-1$
            break;
        case NON_HEAP:
            result = "Non-heap memory"; //$NON-NLS-1$
            break;
        }
        return result;
    }
}
