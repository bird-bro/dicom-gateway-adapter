package org.bird.adapter.cstore;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomOutputStream;

import java.io.*;

/**
 * @author bird
 * @date 2021-7-2 16:42
 **/
public class DicomStreamUtils {

    /**
     * Adds the DICOM meta header to input stream.
     * @params
     * @return InputStream
     * @since 2021-3-2 11:26
     */
    public static InputStream dicomStreamWithFileMetaHeader(String sopInstanceUID, String sopClassUID, String transferSyntax,
                                                            InputStream inDicomStream) throws IOException {

        // File meta header (group 0002 tags), always in Explicit VR Little Endian.
        // http://dicom.nema.org/dicom/2013/output/chtml/part10/chapter_7.html
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        DicomOutputStream fmiStream = new DicomOutputStream(outBuffer, UID.ExplicitVRLittleEndian);
        Attributes fmi = Attributes.createFileMetaInformation(sopInstanceUID, sopClassUID, transferSyntax);

        fmiStream.writeFileMetaInformation(fmi);

        // Add the file meta header + DICOM dataset (other groups) as a sequence of input streams.
        return new SequenceInputStream(new ByteArrayInputStream(outBuffer.toByteArray()), inDicomStream);
    }

    private DicomStreamUtils() {}

}
