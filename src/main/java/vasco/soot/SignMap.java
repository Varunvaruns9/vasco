package vasco.soot;

import java.util.HashMap;

import soot.Local;
import vasco.soot.examples.SignAnalysis.Sign;

// Extends HashMap for adding a new method for equals.
// Made only for Sign Analysis.
public class SignMap extends HashMap<Local, Sign> {
    public SignMap() {
        super();
    }

    public SignMap(SignMap mp) {
        super(mp);
    }

    public boolean equals(SignMap mp) {
        if (this.size() != mp.size()) {
            return false;
        }
        for (Local val: this.keySet()) {
            if (!mp.containsKey(val) || this.get(val) != mp.get(val)) {
                return false;
            }
        }
        return true;
    }
}
