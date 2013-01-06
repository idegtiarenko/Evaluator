package com.gman.evaluator.engine.services;

import com.gman.evaluator.engine.Evaluation;
import com.gman.evaluator.engine.Item;
import com.gman.evaluator.engine.Items;
import com.gman.evaluator.engine.Matrix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author gman
 * @since 11/30/12 9:40 AM
 */
public class EvaluatingService extends AbstractService<Evaluation> {

    private Items items;
    private List<String> extractedProperties;

    public EvaluatingService() {
        super("Evaluating service");
    }

    public void setItems(Items items) {
        this.items = items;
    }

    @Override
    public Evaluation call() throws Exception {
        callback.processed("Creating matrix x", 0);
        final Matrix x = createMatrixX();
        callback.processed("Creating matrix y", 20);
        final Matrix y = createMatrixY();
        callback.processed("Processing matrix", 40);
        final Matrix a = countRegressionCoefficients(x, y);
        callback.processed("Done", 100);
        return extractEvaluation(a);
    }

    private Matrix createMatrixX() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Nothing to evaluate!");
        }
        final Set<String> combinedProperties = new HashSet<String>();
        for (Item item : items) {
            combinedProperties.addAll(item.getDeclaredProperties());
        }
        extractedProperties = new ArrayList<String>(combinedProperties);
        final int propertiesNum = extractedProperties.size();
        final Matrix x = new Matrix(items.size(), 1 + propertiesNum);
        int i = 0;
        for (Item item : items) {
            x.setElement(i, 0, 1.0);
            for (int p = 0; p < propertiesNum; p++) {
                x.setElement(i, p + 1, item.getProperty(extractedProperties.get(p)));
            }
            i++;
        }
        return x;
    }

    private Matrix createMatrixY() {
        final Matrix y = new Matrix(items.size(), 1);
        int i = 0;
        for (Item item : items) {
            y.setElement(i, 0, item.getPrice());
            i++;
        }
        return y;
    }

    private Matrix countRegressionCoefficients(Matrix x, Matrix y) {
        final Matrix xt = x.transpose();
        final Matrix xtx = xt.mult(x);
        final Matrix xty = xt.mult(y);
        return xtx.reverse().mult(xty);
    }

    private Evaluation extractEvaluation(Matrix a) {
        final List<String> properties = new ArrayList<String>();
        properties.add(Evaluation.BASE);
        properties.addAll(extractedProperties);
        final Evaluation evaluation = new Evaluation();
        for (int i = 0; i < properties.size(); i++) {
            evaluation.addPrice(properties.get(i), a.getElement(i, 0));
        }
        extractedProperties = null;
        return evaluation;
    }
}