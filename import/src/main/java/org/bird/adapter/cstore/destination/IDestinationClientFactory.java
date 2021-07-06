package org.bird.adapter.cstore.destination;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author bird
 * @date 2021-7-5 10:09
 **/
public interface IDestinationClientFactory {

    DestinationHolder create(String callingAet, InputStream inPdvStream) throws IOException;

}
