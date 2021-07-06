package org.bird.adapter.cstore.destination;

import com.google.common.collect.ImmutableList;
import org.bird.adapter.DestinationFilter;
import org.bird.adapter.ImportAdapter.Pair;
import org.bird.gateway.IGatewayClient;
import org.dcm4che3.data.Attributes;

/**
 * @author bird
 * @date 2021-7-5 10:17
 **/
public class SingleDestinationClientFactory extends DestinationClientFactory {

    public SingleDestinationClientFactory(ImmutableList<Pair<DestinationFilter, IGatewayClient>> healthDestinationPairList, IGatewayClient defaultDicomWebClient) {
        super(healthDestinationPairList, defaultDicomWebClient);
    }

    @Override
    protected void selectAndPutDestinationClients(DestinationHolder destinationHolder, String callingAet, Attributes attrs) {
        for (Pair<DestinationFilter, IGatewayClient> filterToDestination: healthcareDestinations) {
            if (filterToDestination.getLeft().matches(callingAet, attrs)) {
                destinationHolder.setSingleDestination(filterToDestination.getRight());
                return;
            }
        }
    }

}
