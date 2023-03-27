package com.ericsson.mts.nas.informationelement.field.translator;

import com.ericsson.mts.nas.BitInputStream;
import com.ericsson.mts.nas.exceptions.DecodingException;
import com.ericsson.mts.nas.exceptions.DictionaryException;
import com.ericsson.mts.nas.exceptions.NotHandledException;
import com.ericsson.mts.nas.informationelement.field.AbstractField;
import com.ericsson.mts.nas.informationelement.field.AbstractTranslatorField;
import com.ericsson.mts.nas.reader.Reader;
import com.ericsson.mts.nas.reader.XMLFormatReader;
import com.ericsson.mts.nas.registry.Registry;
import com.ericsson.mts.nas.writer.FormatWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

import static com.ericsson.mts.nas.reader.Reader.encodeFields;
import static com.ericsson.mts.nas.reader.XMLFormatReader.binaryToHex;

public class BinaryLengthField extends AbstractTranslatorField {

    @Override
    public int decode(Registry mainRegistry, BitInputStream bitInputStream, FormatWriter formatWriter) throws IOException, DecodingException, DictionaryException, NotHandledException {

        formatWriter.enterObject(name);
        int result = bitInputStream.readBits(((DigitsField)pdu.get(0)).length);
        formatWriter.intValue("Length", BigInteger.valueOf(result));

        logger.trace("Enter field {} -- {}", name, result);
        if (result > 0) {
            BitInputStream buffer = new BitInputStream(new ByteArrayInputStream(Reader.readByte(result*8,nBitLength,bitInputStream,logger, formatWriter)));
    
            for (AbstractField abstractField : pdu.subList( 1, pdu.size())) {
                abstractField.decode(mainRegistry, buffer, formatWriter);
            }
        }
        formatWriter.leaveObject(name);
        return 0;
    }

    @Override
    public String encode(Registry mainRegistry, XMLFormatReader r, StringBuilder binaryString) throws DecodingException {

        StringBuilder hexaString = new StringBuilder();

        r.enterObject(name);
        if(r.exist("Length") != null){
            Integer len = Integer.valueOf(r.stringValue("Length"));
            Integer nBit = ((DigitsField)pdu.get(0)).length;
            logger.trace("encode field {} {}", name, len);
            if (len > 0) {
                encodeFields(pdu,mainRegistry,r,binaryString,hexaString);
            } else {
                binaryToHex(binaryString.append(String.format("%"+nBit+"s", Integer.toBinaryString(len.byteValue() & 0xFF)).replace(' ', '0')),hexaString,nBit);
            }

        }
        r.leaveObject(name);
        return hexaString.toString();
    }
}
