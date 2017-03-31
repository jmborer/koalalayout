package net.java.dev.koalalayout;

import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 *  This class extends GridbagConstraints but sets its default fill constraints to BOTH instead of NONE
 */
public class KoalaConstraints extends GridBagConstraints {

    public KoalaConstraints() {
        super();
        fill = GridBagConstraints.BOTH;
        anchor = GridBagConstraints.CENTER;
    }

    public KoalaConstraints(int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor, int fill, Insets insets, int ipadx, int ipady) {
        super(gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, ipadx, ipady);
    }
}
