package net.carbonmc.graphene.util;


public interface FastMapDuck {
    StateIndexer<?> fastmap$getIndexer();
    int           fastmap$getIndex();
    void          fastmap$setIndexer(StateIndexer<?> idx);
    void          fastmap$setIndex(int ix);
}