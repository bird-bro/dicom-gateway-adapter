package org.bird.adapter.cstore.destination;

import com.google.common.collect.ImmutableList;
import org.bird.adapter.AetDictionary.Aet;
import org.bird.adapter.DestinationFilter;
import org.bird.adapter.ImportAdapter.Pair;
import org.bird.gateway.IGatewayClient;
import org.dcm4che3.data.Attributes;

/**
 * @author bird
 * @date 2021-7-5 10:13
 **/
public class MultipleDestinationClientFactory extends DestinationClientFactory {

    private ImmutableList<Pair<DestinationFilter, Aet>> dicomDestinations;

    public MultipleDestinationClientFactory(ImmutableList<Pair<DestinationFilter, IGatewayClient>> healthcareDestinations,
                                            ImmutableList<Pair<DestinationFilter, Aet>> dicomDestinations,
                                            IGatewayClient defaultDicomWebClient) {
        super(healthcareDestinations, defaultDicomWebClient, dicomDestinations != null && !dicomDestinations.isEmpty());
        this.dicomDestinations = dicomDestinations;
    }


    @Override
    protected void selectAndPutDestinationClients(DestinationHolder destinationHolder, String callingAet, Attributes attrs) {
        ImmutableList.Builder<IGatewayClient> filteredHealthcareWebClientsBuilder = ImmutableList.builder();
        if (healthcareDestinations != null) {
            for (Pair<DestinationFilter, IGatewayClient> filterToDestination : healthcareDestinations) {
                if (filterToDestination.getLeft().matches(callingAet, attrs)) {
                    filteredHealthcareWebClientsBuilder.add(filterToDestination.getRight());
                }
            }
            destinationHolder.setHealthcareDestinations(filteredHealthcareWebClientsBuilder.build());
        }

        if (dicomDestinations != null) {
            ImmutableList.Builder<Aet> filteredDicomDestinationsBuilder = ImmutableList.builder();
            for (Pair<DestinationFilter, Aet> filterToDestination : dicomDestinations) {
                if (filterToDestination.getLeft().matches(callingAet, attrs)) {
                    filteredDicomDestinationsBuilder.add(filterToDestination.getRight());
                }
            }
            destinationHolder.setDicomDestinations(filteredDicomDestinationsBuilder.build());
        }
    }


}
