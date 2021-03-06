/*******************************************************************************
 * Copyhacked (H) 2012-2014.
 * This program and the accompanying materials
 * are made available under no term at all, use it like
 * you want, but share and discuss about it
 * every time possible with every body.
 * 
 * Contributors:
 *      ron190 at ymail dot com - initial implementation
 ******************************************************************************/
package com.jsql.view.swing.manager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.jsql.i18n.I18n;
import com.jsql.model.injection.MediatorModel;
import com.jsql.view.swing.HelperGUI;
import com.jsql.view.swing.MediatorGUI;
import com.jsql.view.swing.list.DnDList;
import com.jsql.view.swing.scrollpane.LightScrollPane;

/**
 * Manager to display webpages frequently used as backoffice administration.
 */
@SuppressWarnings("serial")
public class ManagerAdminPage extends ManagerAbstractList {
    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = Logger.getLogger(ManagerAdminPage.class);

    /**
     * Create admin page finder.
     */
    public ManagerAdminPage() {
        this.setLayout(new BorderLayout());
        this.setDefaultText(I18n.ADMIN_PAGE_RUN_BUTTON);

        List<String> pathList = new ArrayList<String>();
        try {
            InputStream in = ManagerAdminPage.class.getResourceAsStream("/com/jsql/list/admin-page.txt");
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                pathList.add(line);
            }
            reader.close();
        } catch (IOException e) {
            LOGGER.error(e, e);
        }

        final DnDList listFile = new DnDList(pathList);

        this.add(new LightScrollPane(1, 1, 0, 0, listFile), BorderLayout.CENTER);

        JPanel lastLine = new JPanel();
        lastLine.setOpaque(false);
        lastLine.setLayout(new BoxLayout(lastLine, BoxLayout.X_AXIS));

        lastLine.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, HelperGUI.COMPONENT_BORDER), 
                BorderFactory.createEmptyBorder(1, 0, 1, 1)));
        
        run = new JButton(
            defaultText, 
            new ImageIcon(ManagerAdminPage.class.getResource("/com/jsql/view/swing/images/adminSearch.png"))
        );

        run.setToolTipText(I18n.ADMIN_PAGE_RUN_BUTTON_TOOLTIP);
        run.setBorder(HelperGUI.BLU_ROUND_BORDER);

        run.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (listFile.getSelectedValuesList().isEmpty()) {
                    LOGGER.warn("Select at least one admin page.");
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (run.getText().equals(defaultText)) {
                            if ("".equals(MediatorGUI.top().addressBar.getText())) {
                                LOGGER.warn("Please define the site URL first.");
                            } else {
                                run.setText("Stop");
                                loader.setVisible(true);
                                MediatorModel.model().ressourceAccessObject.getAdminPage(
                                    MediatorGUI.top().addressBar.getText(), 
                                    listFile.getSelectedValuesList()
                                );
                            }
                        } else {
                            MediatorModel.model().ressourceAccessObject.endAdminSearch = true;
                            run.setEnabled(false);
                        }
                    }
                }, "getAdminPage").start();
            }
        });

        loader.setVisible(false);

        lastLine.add(Box.createHorizontalGlue());
        lastLine.add(loader);
        lastLine.add(Box.createRigidArea(new Dimension(5, 0)));
        lastLine.add(run);
        this.add(lastLine, BorderLayout.SOUTH);
    }
}
