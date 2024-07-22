package org.codejive.jvm;

/** The class implementing all the jvm command actions. */
public class Jvm {

    private Jvm() {}

    /**
     * Create a new {@link Builder} instance for the {@link Jvm} class.
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder class for the {@link Jvm} class. */
    public static class Builder {

        private Builder() {}

        /**
         * Builds the {@link Jvm} instance.
         *
         * @return A {@link Jvm} instance.
         */
        public Jvm build() {
            return new Jvm();
        }
    }
}
