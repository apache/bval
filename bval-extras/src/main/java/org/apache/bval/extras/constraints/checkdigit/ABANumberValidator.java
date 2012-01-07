package org.apache.bval.extras.constraints.checkdigit;

public final class ABANumberValidator
    extends ModulusValidator<ABANumber> {

    /** weighting given to digits depending on their right position */
    private static final int[] POSITION_WEIGHT = new int[] {3, 1, 7};

    public ABANumberValidator() {
        super(10);
    }

    /**
     * Calculates the <i>weighted</i> value of a character in the
     * code at a specified position.
     * <p>
     * ABA Routing numbers are weighted in the following manner:
     * <pre><code>
     *     left position: 1  2  3  4  5  6  7  8  9
     *            weight: 3  7  1  3  7  1  3  7  1
     * </code></pre>
     *
     * {@inheritDoc}
     */
    @Override
    protected int weightedValue( int charValue, int leftPos, int rightPos )
            throws Exception {
        int weight = POSITION_WEIGHT[rightPos % 3];
        return (charValue * weight);
    }

}
