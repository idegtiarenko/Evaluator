package com.gman.evaluator.gui;

import com.gman.evaluator.engine.Currency;
import com.gman.evaluator.engine.Evaluation;
import com.gman.evaluator.engine.Items;
import com.gman.evaluator.engine.Parameter;
import com.gman.evaluator.engine.Parser;
import com.gman.evaluator.engine.ParserFactory;
import com.gman.evaluator.engine.Rule;
import com.gman.evaluator.engine.UrlGenerator;
import com.gman.evaluator.engine.parameters.Counter;
import com.gman.evaluator.engine.services.DataExtractingService;
import com.gman.evaluator.engine.services.EvaluatingService;
import com.gman.evaluator.engine.services.OfferingService;
import com.gman.evaluator.gui.components.ParameterCreator;
import com.gman.evaluator.gui.components.ParserCreator;
import com.gman.evaluator.gui.components.RuleCreator;
import com.gman.evaluator.gui.components.UrlCreator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * @author gman
 * @since 26.11.12 20:02
 */
public class MainForm extends JFrame {

    //model
    private final JPickListModel<Parser> parsers = new JPickListModel<Parser>();
    private final JPickListModel<String> sources = new JPickListModel<String>();
    private final JPickListModel<Parameter<?>> parameters = new JPickListModel<Parameter<?>>();
    private final ItemTableModel allItemTableModel = new ItemTableModel();
    private final EvaluationsTableModel evaluationsTableModel = new EvaluationsTableModel();
    private final JPickListModel<Rule> rules = new JPickListModel<Rule>();
    private final ItemTableModel offersTableModel = new ItemTableModel();

    //services
    private final DataExtractingService dataExtractingService = new DataExtractingService();
    private final EvaluatingService evaluatingService = new EvaluatingService();
    private final OfferingService offeringService = new OfferingService();

    //actions
    private final LoadFromDiscAction loadFromDiscAction = new LoadFromDiscAction();
    private final LoadFromSourcesAction loadFromSourcesAction = new LoadFromSourcesAction();
    private final EvaluateAction evaluateAction = new EvaluateAction();
    private final OfferAction offerAction = new OfferAction();
    private final AboutAction aboutAction = new AboutAction();

    {
        parsers.addItem(ParserFactory.crete(getClass().getClassLoader().getResourceAsStream("auto_ria_ua.properties")));
        parsers.addItem(ParserFactory.crete(getClass().getClassLoader().getResourceAsStream("m_rst_ua.properties")));

        parameters.addItem(new Counter("page", 1, 100));
    }

    public MainForm() throws HeadlessException {
        super();
        init();
    }

    private void init() {
        setTitle("Evaluator");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());


        final JMenuBar menu = new JMenuBar();
        menu.add(ComponentUtils.menu("Actions",
                ComponentUtils.activeElement(new JMenuItem("Load from disc"), loadFromDiscAction),
                ComponentUtils.activeElement(new JMenuItem("Load from sources"), loadFromSourcesAction),
                ComponentUtils.activeElement(new JMenuItem("Evaluate"), evaluateAction),
                ComponentUtils.activeElement(new JMenuItem("Offer"), offerAction)
        ));
        menu.add(ComponentUtils.menu("Exchange rate",
                ComponentUtils.activeElement(new JMenuItem("for USD"), new ExchangeRateAction(Currency.USD)),
                ComponentUtils.activeElement(new JMenuItem("for EUR"), new ExchangeRateAction(Currency.EUR)),
                ComponentUtils.activeElement(new JMenuItem("for UAH"), new ExchangeRateAction(Currency.UAH))
        ));
        menu.add(ComponentUtils.menu("About",
                ComponentUtils.activeElement(new JMenuItem("About"), aboutAction)
        ));
        setJMenuBar(menu);

        final JTabbedPane pane = new JTabbedPane();
        pane.add("Parsers", new JPickList<Parser>(parsers, new ParserCreator(), new ParserOperation()));
        pane.add("Sources", new JPickList<String>(sources, new UrlCreator(this)));
        pane.add("Params", new JPickList<Parameter<?>>(parameters, new ParameterCreator(this)));
        pane.add("Items", new JItemsViewer(allItemTableModel));
        pane.add("Evaluations", ComponentUtils.table(evaluationsTableModel));
        pane.add("Rules", new JPickList<Rule>(rules, new RuleCreator(this)));
        pane.add("Offers", new JItemsViewer(offersTableModel));
        getContentPane().add(pane, BorderLayout.CENTER);

        final JPanel controls = new JPanel(new GridLayout(1, 4));
        controls.add(ComponentUtils.activeElement(new JButton("Load from disc"), loadFromDiscAction));
        controls.add(ComponentUtils.activeElement(new JButton("Load from sources"), loadFromSourcesAction));
        controls.add(ComponentUtils.activeElement(new JButton("Evaluate"), evaluateAction));
        controls.add(ComponentUtils.activeElement(new JButton("Offer"), offerAction));
        getContentPane().add(controls, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(900, 600));
        pack();
    }

    public JPickListModel<Parser> getParsers() {
        return parsers;
    }

    public ItemTableModel getAllItemTableModel() {
        return allItemTableModel;
    }

    private class LoadFromDiscAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ComponentUtils.openFileOperation(new ComponentUtils.OpenFileOperation() {
                @Override
                public void perform(InputStream is) throws IOException {
                    try {
                        allItemTableModel.setItems((Items) new ObjectInputStream(is).readObject());
                    } catch (ClassNotFoundException e1) {
                        throw new IOException(e1);
                    }
                }
            });
        }
    }

    private class LoadFromSourcesAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            final java.util.List<UrlGenerator> generators = new ArrayList<UrlGenerator>();
            for (String source : sources.getData()) {
                generators.add(new UrlGenerator(source, parameters.getData()));
            }
            dataExtractingService.setParsers(parsers.getData());
            dataExtractingService.setUrlGenerators(generators);
            ComponentUtils.executeWithProgressMonitor(MainForm.this,
                    new ComponentUtils.BackgroundProcessable<Items>(dataExtractingService) {
                        @Override
                        public void setResult() {
                            try {
                                allItemTableModel.setItems(getResult());
                            } catch (Exception ex) {
                                ComponentUtils.showErrorDialog(ex);
                            }
                        }
                    });
        }
    }

    private class EvaluateAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            evaluatingService.setItems(allItemTableModel.getItems());
            ComponentUtils.executeWithProgressMonitor(MainForm.this,
                    new ComponentUtils.BackgroundProcessable<Evaluation>(evaluatingService) {
                        @Override
                        public void setResult() {
                            try {
                                evaluationsTableModel.setEvaluation(getResult());
                            } catch (Exception ex) {
                                ComponentUtils.showErrorDialog(ex);
                            }
                        }
                    });
        }
    }

    private class OfferAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            offeringService.setItems(allItemTableModel.getItems());
            offeringService.setEvaluation(evaluationsTableModel.getEvaluation());
            offeringService.setRules(rules.getData());
            ComponentUtils.executeWithProgressMonitor(MainForm.this,
                    new ComponentUtils.BackgroundProcessable<Items>(offeringService) {
                        @Override
                        public void setResult() {
                            try {
                                offersTableModel.setItems(getResult());
                            } catch (Exception ex) {
                                ComponentUtils.showErrorDialog(ex);
                            }
                        }
                    });
        }
    }

    private class AboutAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ComponentUtils.showMessage("Created by gman. \n mailto : gmandnepr@gmail.com");
        }
    }

    private class ParserOperation implements JPickListItemOperation<Parser> {
        @Override
        public void performed(Parser item) {
            Browser.openURL(item.getProperties().getSearch());
        }
    }

    private class ExchangeRateAction implements ActionListener {

        private final Currency currency;

        private ExchangeRateAction(Currency currency) {
            this.currency = currency;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final String toParse = JOptionPane.showInputDialog(null,
                    "Current rate is " + currency.getExchangeRate(),
                    "Set exchange rate for " + currency.toString(),
                    JOptionPane.QUESTION_MESSAGE);
            if (toParse != null) {
                try {
                    currency.setExchangeRate(Double.parseDouble(toParse));
                } catch (NumberFormatException ex) {
                    ComponentUtils.showMessage("Failed to parse number");
                }
            }
        }
    }
}