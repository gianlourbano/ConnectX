package schillaci;

public interface TT {
    abstract int searchHashNode(long hashKey, int depth, int alpha, int beta);

    abstract void storeHashNode(long hashKey, int value, int depth, int type);
}
