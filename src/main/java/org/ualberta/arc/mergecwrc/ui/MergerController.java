package org.ualberta.arc.mergecwrc.ui;

/**
 * A basic controller interface that mergers can use to display and correct results.
 * 
 * @author mmckella
 */
public interface MergerController {
    void addMerge(MultipleMatchModel match);
    void setTotalEntities(int total);
    void incrementCurrentEntities();
}
