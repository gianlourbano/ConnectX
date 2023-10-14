package connectx.schillaci.TableTWOBIG1;

import connectx.CXBoard;

public class TTEntry {
    public TTEntry(TTResult r) {
        first = r;
    }

    public TTResult pickResult(CXBoard B, int depth) {
        boolean isFirstLegal = !B.fullColumn(first.bestMove),
                isSecondLegal = second == null ? false : !B.fullColumn(second.bestMove);

        if (!isFirstLegal)
			return isSecondLegal ? second : null;
		if (!isSecondLegal)
			return isFirstLegal ? first : null;
		if (depth <= first.depth && first.flag == TTResult.type.EXACT)
			return first;
		if (depth <= second.depth && second.flag == TTResult.type.EXACT)
			return second;
		if (depth <= first.depth)
			return first;
		if (depth <= second.depth)
			return second;
		return first;
    }

    public void add(TTResult r) {
        if(r.compare(first) >= 0) {
            second = first;
            first = r;
        } else {
            second = r;
        }
    }

    public TTResult first;
    public TTResult second = null;
}
