package com.gman.evaluator.engine.services;

import com.gman.evaluator.engine.Evaluation;
import com.gman.evaluator.engine.Item;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author gman
 * @since 03.03.13 15:32
 */
public class EvaluatingCarService extends AbstractService<Double> {

    private Item item;
    private final AtomicReference<Evaluation> evaluationHolder;

    public EvaluatingCarService(AtomicReference<Evaluation> evaluationHolder) {
        super("Evaluate custom car");
        this.evaluationHolder = evaluationHolder;
    }

    @Override
    public Double call() throws Exception {
        if (evaluationHolder.get() != null && item != null) {
            return evaluationHolder.get().countPriceFor(item);
        } else {
            throw new IllegalStateException("No data for evaluation!");
        }
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}