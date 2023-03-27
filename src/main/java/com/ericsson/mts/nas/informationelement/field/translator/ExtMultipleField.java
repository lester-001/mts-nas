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
import java.util.List;

import static com.ericsson.mts.nas.reader.Reader.encodeFields;
import static com.ericsson.mts.nas.reader.XMLFormatReader.binaryToHex;

public class ExtMultipleField extends AbstractTranslatorField {

    public String contentLength;
    public Integer nBit = 8;
    public List<AbstractField> pdu;
    public Integer Extension = 0;
    public Integer ConfigurationProtocol = 0;

    @Override
    public int decode(Registry mainRegistry, BitInputStream bitInputStream, FormatWriter formatWriter) throws IOException, DecodingException, DictionaryException, NotHandledException {
        if (contentLength.toLowerCase().contains("number")) {
            int result = bitInputStream.readBits(((DigitsField)pdu.get(0)).length);
            formatWriter.intValue(pdu.get(0).getName(), BigInteger.valueOf(result));
            logger.trace("Number element " + result);

            for (int i = 0; i < result; i++) {
                logger.trace("Enter in field " + name + (i+1));
                formatWriter.enterObject(name + (i+1));
                for (AbstractField abstractField : pdu.subList(1, pdu.size())) {
                    abstractField.decode(mainRegistry, bitInputStream, formatWriter);
                }
                formatWriter.leaveObject(name + i+1);
            }
        }

        if(contentLength.toLowerCase().contains("length")){
            int i = 0;
            int result = bitInputStream.bigReadBits(nBit).intValueExact() - 1;
            formatWriter.intValue("Length", BigInteger.valueOf(result));

            int rExt = bitInputStream.bigReadBits(Extension).intValueExact();
            formatWriter.intValue("Extension", BigInteger.valueOf(rExt));

            int rPPP = bitInputStream.bigReadBits(ConfigurationProtocol).intValueExact();
            formatWriter.intValue("ConfigurationProtocol", BigInteger.valueOf(rPPP));

            BitInputStream buffer = new BitInputStream(new ByteArrayInputStream(Reader.readByte(result*8,nBit,bitInputStream,logger, formatWriter)));

            while(buffer.available() > 0){
                logger.trace("\n\nEnter in field " + name + (i+1));
                formatWriter.enterObject(name + (i+1));
                for (AbstractField abstractField : pdu) {
                    abstractField.decode(mainRegistry, buffer, formatWriter);
                }
                formatWriter.leaveObject(name + i+1);
                i++;
            }
        }
        return 0;
    }

    @Override
    public String encode(Registry mainRegistry, XMLFormatReader r, StringBuilder binaryString) throws DecodingException {

        StringBuilder hexaString = new StringBuilder();
        int i = 1;

        logger.trace("Enter field {}", name);

        if(r.exist("Length") != null){
            Integer len = Integer.valueOf(r.stringValue("Length")) + 1;
            logger.trace("Enter field {} {}", name, len);
            binaryToHex(binaryString.append(String.format("%"+nBit+"s", Integer.toBinaryString(len.byteValue() & 0xFF)).replace(' ', '0')),hexaString,nBit);
        } else{
            binaryToHex(binaryString.append(String.format("%"+((AbstractTranslatorField)pdu.get(0)).length+"s", Integer.toBinaryString(Integer.valueOf(r.stringValue(pdu.get(0).getName())).byteValue() & 0xFF)).replace(' ', '0')),hexaString, ((AbstractTranslatorField)pdu.get(0)).length);
            pdu =  pdu.subList(1, pdu.size());
        }

        if(r.exist("Extension") != null){
            binaryToHex(binaryString.append(String.format("%"+Extension+"s", Integer.toBinaryString(Integer.valueOf(r.stringValue("Extension")).byteValue() & 0xFF)).replace(' ', '0')),hexaString,Extension);
        }

        if(r.exist("ConfigurationProtocol") != null){
            binaryToHex(binaryString.append(String.format("%"+ConfigurationProtocol+"s", Integer.toBinaryString(Integer.valueOf(r.stringValue("ConfigurationProtocol")).byteValue() & 0xFF)).replace(' ', '0')),hexaString,ConfigurationProtocol);
        }

        while(r.isElementExist()){
            logger.trace("Enter field {}", name+i);
            logger.trace("Enter field {} -- {} -- {}", name, hexaString.toString(), binaryString.toString());
            r.enterObject(name+i);
            if(r.isElementExist()) {
                encodeFields(pdu, mainRegistry, r, binaryString, hexaString);
                r.leaveObject(name + i);
                i++;
            }
        }

        return hexaString.toString();
    }
}
