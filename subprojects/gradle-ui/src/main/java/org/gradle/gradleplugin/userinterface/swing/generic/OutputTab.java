/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.gradleplugin.userinterface.swing.generic;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.io.IOException;

/**
 * This just wraps up an OutputPanel so it has a tab header that an be dynamic. The current (rather awkward) JTabbedPane
 * implemenation is to separate the tab contents from its component. This only works with java 1.6 or later.
 *
 * @author mhunsicker
 */
public class OutputTab extends OutputPanel {

    private static final Logger LOGGER = Logging.getLogger(OutputTab.class);

    private JPanel mainPanel;
    private JLabel mainTextLabel;
    private JLabel pinnedLabel;
    private JLabel closeLabel;

   private static ImageIcon closeIcon;
   private static ImageIcon closeHighlightIcon;

   public OutputTab(OutputPanelParent parent, String header) {
        super( parent );
        mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

        mainTextLabel = new JLabel(header);
        pinnedLabel = new JLabel("(Pinned) ");
        pinnedLabel.setVisible(isPinned());

        setupCloseLabel();

        mainPanel.add(mainTextLabel);
        mainPanel.add(Box.createHorizontalStrut(5));
        mainPanel.add(pinnedLabel);
        mainPanel.add(closeLabel);
    }

   private void setupCloseLabel()
   {
      if( closeIcon == null )
      {
          BufferedImage closeImage = getImageResource( "close.png" );
          BufferedImage closeHighlightImage = getImageResource( "close-highlight.png" );

          if( closeImage != null ) {
             closeIcon = new ImageIcon( closeImage );
          }

         if( closeHighlightImage != null ) {
            closeHighlightIcon = new ImageIcon( closeHighlightImage );
         }
      }

      closeLabel = new JLabel( closeIcon );
      closeLabel.addMouseListener( new MouseAdapter() {
         @Override
         public void mouseEntered( MouseEvent e ) {
            closeLabel.setIcon( closeHighlightIcon );
         }

         @Override
         public void mouseExited( MouseEvent e ) {
            closeLabel.setIcon( closeIcon );
         }

         public void mouseClicked(MouseEvent e) {
             close();
         }
      } );
   }

   private BufferedImage getImageResource( String imageResourceName )
    {
       InputStream inputStream = getClass().getResourceAsStream(imageResourceName);
       if (inputStream != null) {
          try {
              BufferedImage image = ImageIO.read(inputStream);
             return image;
          }
          catch ( IOException e) {
              LOGGER.error("Reading image " + imageResourceName, e);
          }
       }

       return null;
    }

    /**
     * Call this if you're going to reuse this. it resets its output.
     *
     * @author mhunsicker
     */
    @Override
    public void reset() {
        super.reset();
        closeLabel.setEnabled(true);
    }

    public Component getTabHeader() {
        return mainPanel;
    }

    public void setTabHeaderText(String newText) {
        mainTextLabel.setText(newText);
    }

    public boolean close() {
        closeLabel.setEnabled(false); // provide feedback to the user that we received their click

        boolean result = super.close();
        if( result ) {
           closeLabel.setEnabled(true);
        }

       return result;
    }

    /**
     * Overridden so we can indicate the pinned state.
     *
     * @param pinned whether or not we're pinned
     * @author mhunsicker
    */
    @Override
    public void setPinned(boolean pinned) {
        pinnedLabel.setVisible(pinned);

        super.setPinned(pinned);
    }
}
