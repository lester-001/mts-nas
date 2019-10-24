package com.ericsson.mts.nas.informationelement.field;

import java.util.List;
import java.util.Map;

public abstract class AbstractTranslatorField extends AbstractField {
    public Integer length;
    public Map<Integer, String> namedValue;
    public Integer nBitLength = 8;
    public List<AbstractField> pdu;
}