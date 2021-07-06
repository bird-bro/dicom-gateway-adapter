package org.bird.adapter.cstore.destination;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CountingInputStream;
import org.bird.adapter.AetDictionary.Aet;
import org.bird.gateway.IGatewayClient;

import java.io.InputStream;

/**
 * @author bird
 * @date 2021-7-5 10:09
 **/
public class DestinationHolder {

    private IGatewayClient singleDestination;
    private ImmutableList<IGatewayClient> healthcareDestinations;
    private ImmutableList<Aet> dicomDestinations;
    private CountingInputStream countingInputStream;


    public DestinationHolder(InputStream destinationInputStream, IGatewayClient defaultDestination) {
        this.countingInputStream = new CountingInputStream(destinationInputStream);
        //default values
        this.singleDestination = defaultDestination;
        this.healthcareDestinations = ImmutableList.of(defaultDestination);
        this.dicomDestinations = ImmutableList.of();
    }


    public CountingInputStream getCountingInputStream() {
        return countingInputStream;
    }


    public void setSingleDestination(IGatewayClient dicomWebClient) {
        this.singleDestination = dicomWebClient;
    }


    public IGatewayClient getSingleDestination() {
        return singleDestination;
    }


    public void setHealthcareDestinations(ImmutableList<IGatewayClient> healthcareDestinations) {
        this.healthcareDestinations = healthcareDestinations;
    }

    public void setDicomDestinations(ImmutableList<Aet> dicomDestinations) {
        this.dicomDestinations = dicomDestinations;
    }

    public ImmutableList<IGatewayClient> getHealthcareDestinations() {
        return healthcareDestinations;
    }

    public ImmutableList<Aet> getDicomDestinations() {
        return dicomDestinations;
    }


}
